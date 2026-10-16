package pizza.dlc.api.macros;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import pizza.dlc.api.macros.constructor.MacroBlock;
import pizza.dlc.api.macros.constructor.impl.JumpBlock;
import pizza.dlc.api.macros.constructor.impl.SendMessageBlock;
import pizza.dlc.api.macros.constructor.impl.SwingBlock;

public class MacroSerializer implements JsonSerializer<MacroBlock>, JsonDeserializer<MacroBlock> {
   public JsonElement serialize(MacroBlock src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject result = new JsonObject();
      result.addProperty("type", src.getClass().getSimpleName());
      result.add("properties", context.serialize(src));
      return result;
   }

   public MacroBlock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject object = json.getAsJsonObject();
      String type = object.get("type").getAsString();
      JsonElement element = object.get("properties");

      return switch (type) {
         case "SendMessageBlock" -> (MacroBlock)context.deserialize(element, SendMessageBlock.class);
         case "JumpBlock" -> (MacroBlock)context.deserialize(element, JumpBlock.class);
         case "SwingBlock" -> (MacroBlock)context.deserialize(element, SwingBlock.class);
         default -> throw new JsonParseException("Unknown block: " + type);
      };
   }
}
