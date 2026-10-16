package pizza.dlc.client.modules.impl.misc;
import pizza.dlc.api.settings.impl.*;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.*;
@ModuleInfo(name="NeuroTrainer",description="Ручные демонстрации, обучение модели и тренировочные боты",category=ModuleCategory.OTHER,permanent=true)
public class NeuroRecorder extends Module {
 public final SliderSetting botSpeed=new SliderSetting("Скорость ботов (м/с)",this,2.8F,.5F,6,.1F);
 public final SliderSetting distance=new SliderSetting("Дистанция ботов",this,2.8F,2,8,.1F);
 public final SliderSetting strafe=new SliderSetting("Интенсивность стрейфов",this,.8F,0,1,.05F);
 public final SliderSetting range=new SliderSetting("Радиус демонстраций",this,12,4,32,1);
 public final SliderSetting learningRate=new SliderSetting("Learning rate",this,.001F,.0001F,.01F,.0001F);
 public final SliderSetting batch=new SliderSetting("Batch size",this,64,16,128,16);
 public final SliderSetting capacity=new SliderSetting("Буфер обучения",this,4096,1024,12000,256);
 public final SliderSetting minimum=new SliderSetting("Минимум примеров",this,256,64,2048,64);
 public final SliderSetting updates=new SliderSetting("Обновлений за пакет",this,2,1,8,1);
 public final SliderSetting checkpoint=new SliderSetting("Checkpoint (сек)",this,30,5,120,5);
 public final SliderSetting mousePause=new SliderSetting("Пауза после мыши (сек)",this,.8F,.2F,3,.1F);
 public final SliderSetting maxYaw=new SliderSetting("Предел yaw за тик",this,120,15,180,5);
 public final SliderSetting maxPitch=new SliderSetting("Предел pitch за тик",this,75,10,90,5);
 public final SliderSetting yawGain=new SliderSetting("Множитель yaw нейро",this,1,.25F,3,.05F);
 public final SliderSetting pitchGain=new SliderSetting("Множитель pitch нейро",this,1,.25F,3,.05F);
 public final BooleanSetting autoWalk=new BooleanSetting("Автоходьба нейро",this);
 public final BooleanSetting autoJump=new BooleanSetting("Автопрыжок нейро",this);
 public final ModeSetting device=new ModeSetting("Устройство обучения",this,"Auto","GPU","CPU");
}
