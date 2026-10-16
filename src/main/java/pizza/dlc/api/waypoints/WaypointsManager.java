package pizza.dlc.api.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.PizzaDLC;

public class WaypointsManager {
   private static final long TEMP_LIFETIME_MS = 5000L;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File file = new File(PizzaDLC.DIRECTORY, "waypoints.json");
   private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();

   public void update(float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      long nowMs = System.currentTimeMillis();
      Iterator<Entry<String, Waypoint>> iterator = this.waypoints.entrySet().iterator();

      while (iterator.hasNext()) {
         Waypoint waypoint = iterator.next().getValue();
         if (waypoint.temp && nowMs - waypoint.creationTime > TEMP_LIFETIME_MS) {
            iterator.remove();
         } else if (waypoint.playerUUID != null && mc.level != null) {
            Player player = null;
            Iterator targetPos = mc.level.players().iterator();

            while (true) {
               if (targetPos.hasNext()) {
                  Player candidate = (Player)targetPos.next();
                  if (!candidate.getUUID().equals(waypoint.playerUUID)) {
                     continue;
                  }

                  player = candidate;
               }

               if (player == null) {
                  iterator.remove();
               } else {
                  Vec3 targetPosx = interpolatePos(player, tickDelta);
                  float alpha = 0.2F * tickDelta;
                  waypoint.pos = waypoint.pos.lerp(targetPosx, Mth.clamp(alpha, 0.0F, 1.0F));
               }
               break;
            }
         }
      }
   }

   public String uniqueName(String base) {
      String name = base;
      int counter = 1;

      while (this.waypoints.containsKey(name)) {
         name = base + " " + counter++;
      }

      return name;
   }

   public boolean add(String name, Vec3 pos) {
      if (name == null || name.isBlank() || pos == null) {
         return false;
      }

      if (this.waypoints.containsKey(name)) {
         return false;
      }

      this.waypoints.put(name, new Waypoint(name, pos, false, System.currentTimeMillis(), null));
      return true;
   }

   public boolean addTemp(String name, Vec3 pos, UUID playerUUID) {
      if (name == null || name.isBlank() || pos == null) {
         return false;
      }

      if (this.waypoints.containsKey(name)) {
         return false;
      }

      this.waypoints.put(name, new Waypoint(name, pos, true, System.currentTimeMillis(), playerUUID));
      return true;
   }

   public boolean del(String name) {
      return name != null && this.waypoints.remove(name) != null;
   }

   public void clear() {
      this.waypoints.clear();
   }

   public void clearPersistent() {
      this.waypoints.entrySet().removeIf(entry -> !entry.getValue().temp);
   }

   public void load() {
      if (!this.file.exists()) return;

      try {
         try (FileReader reader = new FileReader(this.file)) {
            if (JsonParser.parseReader(reader) instanceof JsonArray array) {
               for (JsonElement entryElement : array) {
                  if (!(entryElement instanceof JsonObject object)) continue;

                  String name = object.has("name") ? object.get("name").getAsString() : null;
                  if (name != null && !name.isBlank() && !this.waypoints.containsKey(name)) {
                     double x = object.has("x") ? object.get("x").getAsDouble() : 0.0;
                     double y = object.has("y") ? object.get("y").getAsDouble() : 0.0;
                     double z = object.has("z") ? object.get("z").getAsDouble() : 0.0;
                     this.waypoints.put(name, new Waypoint(name, new Vec3(x, y, z), false, System.currentTimeMillis(), null));
                  }
               }

               return;
            }
         }
      } catch (Exception ignored) {
      }
   }

   public void save() {
      try {
         if (!PizzaDLC.DIRECTORY.exists()) {
            PizzaDLC.DIRECTORY.mkdirs();
         }

         JsonArray array = new JsonArray();

         for (Waypoint waypoint : this.waypoints.values()) {
            if (waypoint == null || waypoint.temp || waypoint.playerUUID != null) continue;

            JsonObject object = new JsonObject();
            object.addProperty("name", waypoint.name);
            object.addProperty("x", waypoint.pos.x);
            object.addProperty("y", waypoint.pos.y);
            object.addProperty("z", waypoint.pos.z);
            array.add(object);
         }

         try (FileWriter writer = new FileWriter(this.file)) {
            writer.write(GSON.toJson(array));
         }
      } catch (Exception ignored) {
      }
   }

   private static Vec3 interpolatePos(Entity entity, float tickDelta) {
      return entity.getPosition(tickDelta);
   }

   public Map<String, Waypoint> getWaypoints() {
      return this.waypoints;
   }

   public static class Waypoint {
      public final String name;
      public Vec3 pos;
      public final boolean temp;
      public final long creationTime;
      public final UUID playerUUID;

      public Waypoint(String name, Vec3 pos, boolean temp, long creationTime, UUID playerUUID) {
         this.name = name;
         this.pos = pos;
         this.temp = temp;
         this.creationTime = creationTime;
         this.playerUUID = playerUUID;
      }
   }
}
