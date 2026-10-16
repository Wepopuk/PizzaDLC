package pizza.dlc.api.neuro;

import com.google.gson.*;
import java.util.List;

/** Immutable optional keyboard TCN saved beside the backwards-compatible rotation weights. */
public final class NeuroMovementModel {
 public final long steps,samples,jumpSamples;
 private final JsonObject metrics;
 private final float[][] weights=new float[3][],biases=new float[3][];
 private final float[] head,bias;
 private final float[] skip,skipBias;
 public record Controls(int forward,int strafe,boolean jump,boolean sprint,float confidence) {}
 public NeuroMovementModel(JsonObject json){
  String architecture=json.get("architecture").getAsString();
  if(!List.of("causal-controls-v1","causal-controls-v2").contains(architecture)||json.get("sequence").getAsInt()!=16||json.get("features").getAsInt()!=37||json.get("channels").getAsInt()!=16)throw new IllegalArgumentException("Incompatible movement architecture");
  if(!json.get("dilations").toString().equals("[1,2,4]")||!json.get("outputs").toString().equals("[\"direction_9\",\"jump_logit\",\"sprint_logit\"]"))throw new IllegalArgumentException("Incompatible movement outputs");
  steps=json.get("steps").getAsLong();samples=json.get("samples").getAsLong();jumpSamples=json.get("jump_samples").getAsLong();
  metrics=json.has("metrics")?json.getAsJsonObject("metrics").deepCopy():new JsonObject();
  if(steps<0||samples<0||jumpSamples<0||jumpSamples>samples)throw new IllegalArgumentException("Invalid movement counters");
  var state=json.getAsJsonObject("weights");
  for(int i=0;i<3;i++){weights[i]=array(state,"layers."+i+".weight",16*(i==0?37:16)*3);biases[i]=array(state,"layers."+i+".bias",16);}
  head=array(state,"head.weight",11*16);bias=array(state,"head.bias",11);
  skip=state.has("skip.weight")?array(state,"skip.weight",11*37):new float[11*37];skipBias=state.has("skip.bias")?array(state,"skip.bias",11):new float[11];
 }
 private static float[] array(JsonObject state,String key,int size){var source=state.getAsJsonArray(key);if(source==null||source.size()!=size)throw new IllegalArgumentException("Invalid movement weights: "+key);float[] result=new float[size];for(int i=0;i<size;i++){result[i]=source.get(i).getAsFloat();if(!Float.isFinite(result[i]))throw new IllegalArgumentException("Non-finite movement weights");}return result;}
 public boolean ready(){return steps>0&&samples>=64;}
 public JsonObject metrics(){return metrics.deepCopy();}
 public float[] logits(float[][] sequence){
  if(sequence.length!=16)throw new IllegalArgumentException("Expected 16 movement frames");
  float[][] input=new float[16][];
  for(int t=0;t<16;t++){if(sequence[t].length!=37)throw new IllegalArgumentException("Movement feature count");input[t]=sequence[t];for(float v:input[t])if(!Float.isFinite(v))throw new IllegalArgumentException("Non-finite movement input");}
  for(int layer=0;layer<3;layer++){
   int channels=input[0].length,dilation=1<<layer;float[][] output=new float[16][16];
   for(int t=0;t<16;t++)for(int o=0;o<16;o++){float sum=biases[layer][o];for(int c=0;c<channels;c++)for(int k=0;k<3;k++){int at=t-(2-k)*dilation;if(at>=0)sum+=input[at][c]*weights[layer][(o*channels+c)*3+k];}output[t][o]=Math.max(0,sum);}input=output;
  }
  float[] result=new float[11];for(int o=0;o<11;o++){float sum=bias[o]+skipBias[o];for(int c=0;c<16;c++)sum+=input[15][c]*head[o*16+c];for(int c=0;c<37;c++)sum+=sequence[15][c]*skip[o*37+c];result[o]=sum;if(!Float.isFinite(sum))throw new IllegalStateException("Non-finite movement prediction");}return result;
 }
 public Controls predict(float[][] sequence){
  if(!ready())return null;float[] logits=logits(sequence);int direction=4;
  for(int i=0;i<9;i++)if(logits[i]>logits[direction])direction=i;
  double sum=0;for(int i=0;i<9;i++)sum+=Math.exp(logits[i]-logits[direction]);float confidence=(float)(1/sum);
  if(confidence<.3)direction=4;
  return new Controls(direction/3-1,direction%3-1,jumpSamples>=8&&sigmoid(logits[9])>=.55,sigmoid(logits[10])>=.55,confidence);
 }
 private static double sigmoid(float x){return 1/(1+Math.exp(-Math.clamp(x,-80,80)));}
}
