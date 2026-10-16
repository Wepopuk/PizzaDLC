package pizza.dlc.client.modules.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
   String name();

   String description() default "У этого модуля нет описания";

   ModuleCategory category();

   int key() default -1;

   boolean hidden() default false;

   boolean permanent() default false;
}
