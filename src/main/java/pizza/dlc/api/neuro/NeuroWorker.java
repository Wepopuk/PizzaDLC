package pizza.dlc.api.neuro;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/** Bounded pipe to an owned local subprocess. All IO/JSON/checkpoint reads run off the game thread. */
public final class NeuroWorker {
   private final Path root;
   private static final Gson CODEC=new Gson();
   private final ArrayBlockingQueue<Object> queue=new ArrayBlockingQueue<>(512);
   private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"neuro-trainer-io");t.setDaemon(true);return t;});
   private final ExecutorService files=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),r->{var t=new Thread(r,"neuro-file-io");t.setDaemon(true);return t;});
   private final ScheduledExecutorService monitor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"neuro-process-watchdog");t.setDaemon(true);return t;});
   private volatile long generation;
   private volatile Process process;private volatile boolean stopping,starting,active,ready,cancelled;
   private volatile String executable="";
   public volatile JsonObject status=new JsonObject();public volatile String error="";public volatile NeuroModel model;public volatile boolean loading;
   public long dropped;
   public NeuroWorker(Path root){this.root=root.toAbsolutePath().normalize();}
   public boolean running(){return active;}
   public boolean ready(){return ready&&active&&!stopping;}
   public boolean stopping(){return stopping;}
   public int queued(){return queue.size();}
   public boolean submit(JsonObject message){boolean accepted=queue.offer(message);if(!accepted)dropped++;return accepted;}
   public record Sample(long segment,long sequence,long time,int target,float[] x,float[] y,float[] controls){public Sample{x=x.clone();y=y.clone();controls=controls==null?null:controls.clone();}public Sample(long segment,long sequence,long time,int target,float[] x,float[] y){this(segment,sequence,time,target,x,y,null);}}
   public boolean submitSample(Sample sample){boolean accepted=queue.offer(sample);if(!accepted)dropped++;return accepted;}
   private static JsonObject encode(Sample s){var row=new JsonObject();row.addProperty("cmd","sample");row.addProperty("segment",s.segment);row.addProperty("seq",s.sequence);row.addProperty("time_ns",s.time);row.addProperty("target",s.target);row.add("x",CODEC.toJsonTree(s.x));row.add("y",CODEC.toJsonTree(s.y));if(s.controls!=null)row.add("controls",CODEC.toJsonTree(s.controls));return row;}
   public void start(String name,String device,JsonObject config){
      if(running()||loading)throw new IllegalStateException("Тренер уже работает или загружает модель");
      long job=++generation;active=true;starting=true;ready=false;cancelled=false;stopping=false;error="";status=new JsonObject();queue.clear();dropped=0;
      executor.submit(()->{
         try{
            Files.createDirectories(root);Path script=root.resolve("trainer.py");
            try(var in=NeuroWorker.class.getResourceAsStream("/neuro/trainer.py")){if(in==null)throw new IOException("Missing trainer.py");Files.copy(in,script,StandardCopyOption.REPLACE_EXISTING);}
            try(var in=NeuroWorker.class.getResourceAsStream("/neuro/movement.py")){if(in==null)throw new IOException("Missing movement.py");Files.copy(in,root.resolve("movement.py"),StandardCopyOption.REPLACE_EXISTING);}
            Path configFile=root.resolve("training-config.json");Files.writeString(configFile,config.toString(),StandardCharsets.UTF_8);
            if(cancelled)return;
            executable=python();var builder=new ProcessBuilder(executable,"-u",script.toString(),"--root",root.toString(),"--name",name,"--device",device,"--config-file",configFile.toString());
            builder.redirectError(root.resolve("trainer-stderr.log").toFile());builder.environment().put("PYTHONIOENCODING","utf-8");
            builder.environment().put("OMP_NUM_THREADS","1");builder.environment().put("MKL_NUM_THREADS","1");
            process=builder.start();
            Process owned=process;
            monitor.schedule(()->{if(active&&generation==job&&!ready&&!cancelled){if(error.isEmpty())error="Запуск тренера превысил 180 секунд: "+stage()+"; см. trainer-stderr.log";terminate(owned);}},180,TimeUnit.SECONDS);
            var reader=new Thread(()->read(owned,job),"neuro-trainer-status");reader.setDaemon(true);reader.start();
            try(var writer=new BufferedWriter(new OutputStreamWriter(owned.getOutputStream(),StandardCharsets.UTF_8))){
               boolean drainSent=false;
               while(owned.isAlive()){
                  if(cancelled){terminate(owned);break;}
                  // Do not fill a Windows pipe while PyTorch/CUDA is still importing.
                  if(!ready){Thread.sleep(50);continue;}
                  if(stopping&&!drainSent){writer.write("{\"cmd\":\"drain\"}\n");writer.flush();drainSent=true;}
                  Object queued=queue.poll(100,TimeUnit.MILLISECONDS);JsonObject message=queued instanceof Sample sample?encode(sample):(JsonObject)queued;
                  if(message!=null){try{writer.write(message.toString());writer.newLine();writer.flush();}catch(IOException broken){if(owned.waitFor(500,TimeUnit.MILLISECONDS))reader.join(1000);if(!error.isEmpty())throw new IOException(error,broken);throw broken;}}
                  if(stopping&&queue.isEmpty()){writer.write("{\"cmd\":\"stop\"}\n");writer.flush();break;}
               }
            }
            if(!owned.waitFor(120,TimeUnit.SECONDS)){terminate(owned);throw new IOException("Тренер не завершил сохранение за 120 секунд; см. trainer-stderr.log (последний checkpoint сохранён ранее)");}
            reader.join(3000);
            if(!cancelled&&owned.exitValue()!=0&&error.isEmpty())throw new IOException("Процесс обучения завершился с кодом "+owned.exitValue()+"; этап "+stage()+"; см. trainer-stderr.log");
            if(!cancelled&&error.isEmpty())model=NeuroModel.load(root,name);
         }catch(Exception e){if(!cancelled&&error.isEmpty())error=e.getMessage()==null?e.toString():e.getMessage();if(process!=null&&process.isAlive())terminate(process);}
         finally{if(cancelled){var s=new JsonObject();s.addProperty("type","cancelled");s.addProperty("stage","Запуск отменён до готовности: новые веса не сохранялись");status=s;}starting=false;ready=false;stopping=false;process=null;queue.clear();active=false;}
      });
   }
   private static void terminate(Process owned){
      // Windows venv python.exe is a launcher; killing only it leaves the real interpreter alive.
      try{for(var child:owned.descendants().toList().reversed())child.destroyForcibly();}
      finally{owned.destroyForcibly();}
   }
   private String stage(){var s=status;return s.has("stage")?s.get("stage").getAsString():"ожидание Python ("+executable+")";}
   private void read(Process owned,long job){try(var reader=new BufferedReader(new InputStreamReader(owned.getInputStream(),StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null){if(generation!=job)return;if(line.length()>65536)throw new IOException("Oversized trainer response");var update=JsonParser.parseString(line).getAsJsonObject();String type=update.get("type").getAsString();if("error".equals(type))error=update.get("message").getAsString();else{update.addProperty("python",executable);status=update;if("ready".equals(type)){ready=true;starting=false;}}}}catch(Exception e){if(!cancelled&&generation==job){if(error.isEmpty())error="Протокол тренера: "+e.getMessage();terminate(owned);}}}
   private String python(){
      String override=System.getProperty("pizzadlc.neuro.python");if(override!=null&&!override.isBlank())return override;
      // The client commonly runs from run/. Search the project and its parent, then the system CPU runtime.
      Path cwd=Path.of("").toAbsolutePath();for(Path base:new Path[]{cwd,cwd.getParent(),cwd.getParent()==null?cwd:cwd.getParent().getParent()}){
         if(base==null)continue;Path candidate=base.resolve(".neuro-runtime/Scripts/python.exe");if(Files.isRegularFile(candidate))return candidate.toString();
      }return "python";
   }
   public void stop(){if(running()&&!stopping){stopping=true;long job=generation;if(!ready){cancelled=true;monitor.execute(()->{var owned=process;if(generation==job&&owned!=null)terminate(owned);});return;}monitor.schedule(()->{if(active&&stopping&&generation==job){if(error.isEmpty())error="Тренер не завершил остановку за 180 секунд; см. trainer-stderr.log (сохранение не подтверждено)";var owned=process;if(owned!=null)terminate(owned);}},180,TimeUnit.SECONDS);}}
   public void load(String name){if(running()||loading)throw new IllegalStateException("Сначала останови обучение");loading=true;error="";executor.submit(()->{try{var loaded=NeuroModel.load(root,name);model=loaded;status=snapshotStatus(loaded);}catch(Exception e){error=e.getMessage();}finally{loading=false;}});}
   public void activateModelStatus(){var ready=model;if(ready!=null&&(!status.has("name")||!status.get("name").getAsString().equals(ready.name)))status=snapshotStatus(ready);}
   private static JsonObject snapshotStatus(NeuroModel ready){var s=ready.metrics();s.addProperty("name",ready.name);s.addProperty("samples",ready.samples);s.addProperty("steps",ready.steps);s.addProperty("saved_at",ready.savedAt);s.addProperty("device",ready.trainingDevice);s.addProperty("device_name",ready.trainingDevice);s.addProperty("backend",ready.trainingBackend);s.addProperty("movement_samples",ready.movement==null?0:ready.movement.samples);s.addProperty("movement_steps",ready.movement==null?0:ready.movement.steps);s.addProperty("movement_jump_samples",ready.movement==null?0:ready.movement.jumpSamples);if(ready.movement!=null)for(var metric:ready.movement.metrics().entrySet())s.add(metric.getKey(),metric.getValue());return s;}
   public void shutdown(){if(monitor.isShutdown())return;stop();files.shutdown();executor.shutdown();try{if(!executor.awaitTermination(120,TimeUnit.SECONDS)&&process!=null)terminate(process);}catch(InterruptedException e){Thread.currentThread().interrupt();if(process!=null)terminate(process);}finally{monitor.shutdownNow();}}
   public boolean task(Runnable task){try{files.submit(task);return true;}catch(RejectedExecutionException e){return false;}}
}
