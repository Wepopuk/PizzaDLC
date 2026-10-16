package pizza.dlc.api.emote;

import java.util.List;

public final class EmoteRegistry {
   public static final List<EmoteDefinition> ALL = List.of(
      new EmoteDefinition("six_seven", "67"),
      new EmoteDefinition("rampage", "Рампейдж"),
      new EmoteDefinition("rampage_2", "Рампейдж 2"),
      new EmoteDefinition("rampage_3", "Рампейдж 3"),
      new EmoteDefinition("rampage_4", "Рампейдж 4"),
      new EmoteDefinition("sturdy_drill", "Sturdy (drill)"),
      new EmoteDefinition("aus_shuffle", "AUS shuffle"),
      new EmoteDefinition("dnb_step", "DnB step"),
      new EmoteDefinition("jumpstyle", "Jumpstyle"),
      new EmoteDefinition("hopak", "Гопак"),
      new EmoteDefinition("slip_back", "Slip Back"),
      new EmoteDefinition("waving", "Привет"),
      new EmoteDefinition("clap", "Аплодисменты"),
      new EmoteDefinition("point", "Указать"),
      new EmoteDefinition("here", "Иди сюда"),
      new EmoteDefinition("palm", "Фейспалм"),
      new EmoteDefinition("crying", "Плач"),
      new EmoteDefinition("backflip", "Сальто"),
      new EmoteDefinition("twerk", "Тверк"),
      new EmoteDefinition("kazotsky_kick", "Казачок"),
      new EmoteDefinition("club_penguin_dance", "Танец пингвина"),
      new EmoteDefinition("roblox_potion_dance", "Танец с зельем"),
      new EmoteDefinition("ziga", "Зига", "textures/emotes/skins/ziga.png"),
      new EmoteDefinition("larp333", "LARP333"),
      new EmoteDefinition("larp333_2", "LARP333 2")
   );

   private EmoteRegistry() {
   }

   public static EmoteDefinition byId(String id) {
      for (EmoteDefinition definition : ALL) {
         if (definition.id().equals(id)) {
            return definition;
         }
      }

      return null;
   }
}
