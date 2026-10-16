package pizza.dlc.api.draggable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import pizza.dlc.PizzaDLC;

public class DraggableManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
   private final HashMap<String, Draggable> draggables = new HashMap<>();
   private final HashMap<String, float[]> pendingPositions = new HashMap<>();

   public void addDraggable(Draggable draggable) {
      this.draggables.put(draggable.getName(), draggable);
      float[] pos = this.pendingPositions.remove(draggable.getName());
      if (pos != null) {
         draggable.setX(pos[0]);
         draggable.setY(pos[1]);
         draggable.syncRenderPosition();
         draggable.setPositioned(true);
      }
   }

   public Draggable getDraggable(String name) {
      return this.draggables.get(name);
   }

   public JsonObject save() {
      try {
         JsonArray arr = GSON.toJsonTree(this.draggables.values()).getAsJsonArray();
         JsonObject obj = new JsonObject();
         obj.add("draggables", arr);
         return obj;
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to save draggables", ex);
         return null;
      }
   }

   public void load(JsonObject object) {
      if (object == null || !object.has("draggables")) return;

      try {
         Draggable[] saved = (Draggable[])GSON.fromJson(object.getAsJsonArray("draggables"), Draggable[].class);

         for (Draggable s : saved) {
            if (s == null) continue;

            Draggable cur = this.draggables.get(s.getName());
            if (cur == null) {
               this.pendingPositions.put(s.getName(), new float[]{s.getX(), s.getY()});
            } else {
               cur.setX(s.getX());
               cur.setY(s.getY());
               cur.syncRenderPosition();
               cur.setPositioned(true);
            }
         }
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to load draggables", ex);
      }
   }

   public void resetAllPositions() {
      for (Draggable d : this.draggables.values()) {
         d.resetPosition();
      }
   }

   public HashMap<String, Draggable> getDraggables() {
      return this.draggables;
   }

   public HashMap<String, float[]> getPendingPositions() {
      return this.pendingPositions;
   }
}
