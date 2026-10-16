package pizza.dlc.api.filemanager.impl;

import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import pizza.dlc.api.account.ServerLoginStore;
import pizza.dlc.api.filemanager.api.ManagerFileAbstract;

public class AltAccountManager extends ManagerFileAbstract<AltAccountManager.OfflineAlt> {
   public AltAccountManager() {
      super("alts.json", "", (new TypeToken<List<OfflineAlt>>() {}).getType(), ArrayList::new);
   }

   public void addAlt(String name) {
      if (name == null || name.isBlank()) return;

      boolean exists = this.getItems().stream().anyMatch(a -> a.name().equalsIgnoreCase(name));
      if (!exists) {
         long now = System.currentTimeMillis();
         this.add(new OfflineAlt(name, now, now, false, false, false));
         this.save();
      }
   }

   public void addPremium(String name) {
      if (name == null || name.isBlank()) return;

      boolean exists = this.getItems().stream().anyMatch(a -> a.name().equalsIgnoreCase(name));
      if (exists) {
         this.rebuild(name, a -> new OfflineAlt(name, a.createdMs(), a.lastUsedMs(), a.selected(), a.favorite(), true, a.note()));
      } else {
         long now = System.currentTimeMillis();
         this.add(new OfflineAlt(name, now, now, false, false, true));
         this.save();
      }
   }

   public void rename(String oldName, String newName) {
      if (oldName.equals(newName)) return;

      this.getItems().removeIf(a -> a.name().equalsIgnoreCase(newName) && !a.name().equalsIgnoreCase(oldName));
      this.rebuild(oldName, a -> new OfflineAlt(newName, a.createdMs(), a.lastUsedMs(), a.selected(), a.favorite(), a.premium(), a.note()));
      ServerLoginStore.get().rename(oldName, newName);
   }

   private void rebuild(String name, java.util.function.UnaryOperator<OfflineAlt> change) {
      List<OfflineAlt> rebuilt = this.getItems().stream().map(a -> a.name().equalsIgnoreCase(name) ? change.apply(a) : a).toList();
      this.getItems().clear();
      this.getItems().addAll(rebuilt);
      this.save();
   }

   public void removeAlt(String name) {
      this.getItems().removeIf(a -> a.name().equalsIgnoreCase(name));
      ServerLoginStore.get().removeAccount(name);
      this.save();
   }

   public void selectAlt(String name) {
      long now = System.currentTimeMillis();
      List<OfflineAlt> rebuilt = this.getItems().stream().map(a -> {
         boolean sel = a.name().equalsIgnoreCase(name);
         return new OfflineAlt(a.name(), a.createdMs(), sel ? now : a.lastUsedMs(), sel, a.favorite(), a.premium(), a.note());
      }).toList();
      this.getItems().clear();
      this.getItems().addAll(rebuilt);
      this.save();
   }

   public void toggleFavorite(String name) {
      List<OfflineAlt> rebuilt = this.getItems()
         .stream()
         .map(
            a -> a.name().equalsIgnoreCase(name)
               ? new OfflineAlt(a.name(), a.createdMs(), a.lastUsedMs(), a.selected(), !a.favorite(), a.premium(), a.note())
               : a
         )
         .toList();
      this.getItems().clear();
      this.getItems().addAll(rebuilt);
      this.save();
   }

   public OfflineAlt getSelected() {
      return this.getItems().stream().filter(OfflineAlt::selected).findFirst().orElse(null);
   }

   public void setNote(String name, String note) {
      this.rebuild(name, a -> new OfflineAlt(a.name(), a.createdMs(), a.lastUsedMs(), a.selected(), a.favorite(), a.premium(), note));
   }

   public record OfflineAlt(String name, long createdMs, long lastUsedMs, boolean selected, boolean favorite, boolean premium, String note) {
      public OfflineAlt {
         note = note == null ? "" : note;
      }

      public OfflineAlt(String name, long createdMs, long lastUsedMs, boolean selected, boolean favorite, boolean premium) {
         this(name, createdMs, lastUsedMs, selected, favorite, premium, "");
      }
   }
}
