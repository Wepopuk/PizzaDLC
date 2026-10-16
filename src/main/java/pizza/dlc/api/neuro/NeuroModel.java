package pizza.dlc.api.neuro;

import com.google.gson.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Immutable PyTorch-compatible causal Conv1d snapshot. No native inference dependency. */
public final class NeuroModel {
   public final long steps,samples; public final double savedAt;public final String name,trainingDevice,trainingBackend;
   private final JsonObject metrics;
   public JsonObject metrics(){return metrics.deepCopy();}
   private final float[][] weights=new float[3][],biases=new float[3][];
   private final float[] head,bias;
   public final NeuroMovementModel movement;
   public NeuroModel(String name,JsonObject json) {
      this.trainingDevice=json.has("training_device")?json.get("training_device").getAsString():"unknown";this.trainingBackend=json.has("training_backend")?json.get("training_backend").getAsString():"unknown";
      this.name=name;this.samples=json.has("samples")?json.get("samples").getAsLong():0;this.savedAt=json.has("saved_at")?json.get("saved_at").getAsDouble():0;this.metrics=json.has("metrics")?json.getAsJsonObject("metrics").deepCopy():new JsonObject();
      if(!Double.isFinite(savedAt)||samples<0)throw new IllegalArgumentException("Invalid model metadata");
      if(json.has("train_loss"))metrics.add("train_loss",json.get("train_loss"));
      if(json.get("format").getAsInt()!=1 || !json.get("architecture").getAsString().equals("causal-tcn-v2") || json.get("sequence").getAsInt()!=16 || json.get("channels").getAsInt()!=24)throw new IllegalArgumentException("Incompatible architecture");
      checkArray(json.getAsJsonArray("features"),NeuroSchema.NAMES);checkNumbers(json.getAsJsonArray("scales"),NeuroSchema.SCALES);
      checkNumbers(json.getAsJsonArray("dilations"),new float[]{1,2,4});checkNumbers(json.getAsJsonArray("output_scale"),new float[]{30,30});
      steps=json.get("steps").getAsLong();if(steps<0)throw new IllegalArgumentException("Invalid training steps");
      JsonObject state=json.getAsJsonObject("weights");
      for(int i=0;i<3;i++){weights[i]=array(state,"layers."+i+".weight",24*(i==0?NeuroSchema.NAMES.length:24)*3);biases[i]=array(state,"layers."+i+".bias",24);}
      head=array(state,"head.weight",48);bias=array(state,"head.bias",2);
      movement=json.has("movement")?new NeuroMovementModel(json.getAsJsonObject("movement")):null;
   }
   private static void checkArray(JsonArray values,String[] wanted){if(values.size()!=wanted.length)throw new IllegalArgumentException("Feature count");for(int i=0;i<wanted.length;i++)if(!values.get(i).getAsString().equals(wanted[i]))throw new IllegalArgumentException("Feature order");}
   private static void checkNumbers(JsonArray values,float[] wanted){if(values.size()!=wanted.length)throw new IllegalArgumentException("Normalization shape");for(int i=0;i<wanted.length;i++)if(values.get(i).getAsFloat()!=wanted[i])throw new IllegalArgumentException("Normalization mismatch");}
   private static float[] array(JsonObject data,String key,int size){var a=data.getAsJsonArray(key);if(a==null||a.size()!=size)throw new IllegalArgumentException("Invalid weights: "+key);float[] out=new float[size];for(int i=0;i<size;i++){out[i]=a.get(i).getAsFloat();if(!Float.isFinite(out[i]))throw new IllegalArgumentException("Non-finite weights");}return out;}
   public float[] predict(float[][] sequence) {
      if(sequence.length!=16)throw new IllegalArgumentException("Expected 16 frames");
      float[][] input=new float[16][];for(int t=0;t<16;t++){if(sequence[t].length!=NeuroSchema.NAMES.length)throw new IllegalArgumentException("Input size");input[t]=sequence[t];for(float v:input[t])if(!Float.isFinite(v))throw new IllegalArgumentException("Non-finite input");}
      for(int layer=0;layer<3;layer++){
         int channels=input[0].length,dilation=1<<layer;float[][] output=new float[16][24];
         for(int t=0;t<16;t++)for(int o=0;o<24;o++){
            float sum=biases[layer][o];for(int c=0;c<channels;c++)for(int k=0;k<3;k++){int at=t-(2-k)*dilation;if(at>=0)sum+=input[at][c]*weights[layer][(o*channels+c)*3+k];}output[t][o]=Math.max(0,sum);
         }input=output;
      }
      float[] result=new float[2];for(int o=0;o<2;o++){float sum=bias[o];for(int c=0;c<24;c++)sum+=input[15][c]*head[o*24+c];result[o]=sum*30;if(!Float.isFinite(result[o]))throw new IllegalStateException("Non-finite prediction");}return result;
   }
   public static boolean validName(String name){return name!=null&&name.matches("[A-Za-z0-9_-]{1,32}");}
   public static NeuroModel load(Path root,String name) throws Exception {
      if(!validName(name))throw new IllegalArgumentException("Use 1–32 Latin letters, digits, _ or -");
      Path folder=root.resolve("models").resolve(name).toAbsolutePath().normalize();var failures=new ArrayList<String>();
      for(String pointer:List.of("current.json","previous.json"))try{
         if(Files.size(folder.resolve(pointer))>8192)throw new IllegalArgumentException("Oversized checkpoint pointer");
         var data=JsonParser.parseString(Files.readString(folder.resolve(pointer))).getAsJsonObject();
         if(data.get("format").getAsInt()!=1)throw new IllegalArgumentException("Checkpoint format");
         Path inference=child(folder,data.get("inference").getAsString()),checkpoint=child(folder,data.get("checkpoint").getAsString());
         if(Files.size(inference)>2*1024*1024||Files.size(checkpoint)>128*1024*1024)throw new IllegalArgumentException("Oversized checkpoint");
         if(!hash(inference).equals(data.get("inference_sha256").getAsString())||!hash(checkpoint).equals(data.get("checkpoint_sha256").getAsString()))throw new IllegalArgumentException("Checkpoint checksum mismatch");
         return new NeuroModel(name,JsonParser.parseString(Files.readString(inference)).getAsJsonObject());
      }catch(Exception e){failures.add(pointer+": "+e.getMessage());}
      throw new IllegalArgumentException("No valid checkpoint for "+name+": "+String.join("; ",failures));
   }
   private static Path child(Path folder,String name){Path file=folder.resolve(name).normalize();if(!file.getParent().equals(folder)||!name.startsWith("g-"))throw new IllegalArgumentException("Invalid checkpoint path");return file;}
   private static String hash(Path file)throws Exception{var digest=MessageDigest.getInstance("SHA-256");try(var in=Files.newInputStream(file)){byte[] chunk=new byte[65536];int count;while((count=in.read(chunk))!=-1)digest.update(chunk,0,count);}return HexFormat.of().formatHex(digest.digest());}
}
