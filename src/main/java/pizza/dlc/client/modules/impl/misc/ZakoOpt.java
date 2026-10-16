package pizza.dlc.client.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import zako.opt.ZakoOptConfig;

@ModuleInfo(
   name = "ZakoOpt",
   description = "Порт мода ZakoOpt (автор Zako, t.me/StarikZako, LGPL-3.0): кэши текста, LOD сущностей и предметов, частицы, спавнеры",
   category = ModuleCategory.OTHER
)
public class ZakoOpt extends Module {
   private static final String TEXT_LAYOUT = "Кэш раскладки текста";
   private static final String TEXT_PREPARED = "Кэш готового текста";
   private static final String ENTITY_LOD = "LOD сущностей";
   private static final String ITEM_LOD = "LOD предметов";
   private static final String NO_SORT = "Без сортировки скинов";
   private static final String PARTICLE_LIGHT = "Кэш света частиц";
   private static final String PARTICLE_PHYSICS = "Простая физика частиц";
   private static final String PARTICLE_LOD = "LOD частиц";
   private static final String PARTICLE_THREADS = "Многопоточные частицы";
   private static final String SPAWNER_CULL = "Скрыть мобов спавнеров";
   private static final String SPAWNER_CACHE = "Кэш ближних спавнеров";
   private static final String SPAWNER_REPLAY = "Спавнеры по тикам";
   private static final String BLOCK_ENTITY_CACHE = "Кэш блок-сущностей";
   private static final String LOOKUP_CACHE = "Кэш поисков";
   private static final String LAZY_CLEAR = "Холст без отвязки";
   private static final String MICRO = "Мелкие ускорения";
   private static final String GUI_ANIM = "Анимация GUI по монитору";
   private final MultiModeSetting text = new MultiModeSetting("Текст", this, TEXT_LAYOUT, TEXT_PREPARED);
   private final MultiModeSetting entities = new MultiModeSetting("Сущности", this, ENTITY_LOD, ITEM_LOD, NO_SORT);
   private final SliderSetting entityLodDistance = new SliderSetting("Дистанция LOD сущностей", this, 24.0F, 8.0F, 64.0F, 1.0F);
   private final SliderSetting itemLodDistance = new SliderSetting("Дистанция LOD предметов", this, 8.0F, 2.0F, 32.0F, 1.0F);
   private final MultiModeSetting particles = new MultiModeSetting("Частицы", this, PARTICLE_LIGHT, PARTICLE_PHYSICS, PARTICLE_LOD, PARTICLE_THREADS);
   private final MultiModeSetting blocks = new MultiModeSetting("Блоки", this, SPAWNER_CULL, SPAWNER_CACHE, SPAWNER_REPLAY, BLOCK_ENTITY_CACHE, LOOKUP_CACHE);
   private final SliderSetting spawnerDistance = new SliderSetting("Дистанция спавнеров", this, 16.0F, 4.0F, 64.0F, 1.0F);
   private final MultiModeSetting render = new MultiModeSetting("Рендер", this, LAZY_CLEAR, MICRO, GUI_ANIM);
   private final ZakoOptConfig.Values values = new ZakoOptConfig.Values();

   public ZakoOpt() {
      for (MultiModeSetting group : new MultiModeSetting[]{this.text, this.entities, this.particles, this.blocks, this.render}) {
         for (int i = 0; group.get(i) != null; i++) {
            group.get(i).setEnabled(true);
         }
      }

      this.entityLodDistance.setVisible(() -> this.on(this.entities, ENTITY_LOD));
      this.itemLodDistance.setVisible(() -> this.on(this.entities, ITEM_LOD));
      this.spawnerDistance.setVisible(() -> this.on(this.blocks, SPAWNER_CULL));
      this.setEnabled(true);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.push();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      ZakoOptConfig.apply(null);
   }

   @EventHandler
   public void onTick(EventTick e) {
      this.push();
   }

   private void push() {
      ZakoOptConfig.Values v = this.values;
      v.enabled = this.isEnabled();
      v.bidiCache = this.on(this.text, TEXT_LAYOUT);
      v.preparedTextCache = this.on(this.text, TEXT_PREPARED);
      v.entityLod = this.on(this.entities, ENTITY_LOD);
      v.itemLod = this.on(this.entities, ITEM_LOD);
      v.noEntitySort = this.on(this.entities, NO_SORT);
      v.entityLodDistance = Math.round(this.entityLodDistance.getValue());
      v.itemLodDistance = Math.round(this.itemLodDistance.getValue());
      v.particleLight = this.on(this.particles, PARTICLE_LIGHT);
      v.particlePhysics = this.on(this.particles, PARTICLE_PHYSICS);
      v.particleLod = this.on(this.particles, PARTICLE_LOD);
      v.parallelParticles = this.on(this.particles, PARTICLE_THREADS);
      v.spawnerCull = this.on(this.blocks, SPAWNER_CULL);
      v.spawnerTickCache = this.on(this.blocks, SPAWNER_CACHE);
      v.spawnerReplay = this.on(this.blocks, SPAWNER_REPLAY);
      v.blockEntityCache = this.on(this.blocks, BLOCK_ENTITY_CACHE);
      v.lookupCaches = this.on(this.blocks, LOOKUP_CACHE);
      v.spawnerDistance = Math.round(this.spawnerDistance.getValue());
      v.lazyClear = this.on(this.render, LAZY_CLEAR);
      v.microOpts = this.on(this.render, MICRO);
      v.guiAnimatedItems = this.on(this.render, GUI_ANIM);
      ZakoOptConfig.apply(v);
   }

   private boolean on(MultiModeSetting group, String name) {
      BooleanSetting s = group.get(name);
      return s != null && s.isEnabled();
   }
}
