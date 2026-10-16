# ZakoOpt — изменения при переносе в PizzaDLC

Оригинал: ZakoOpt 1.0.0, автор Zako (https://t.me/StarikZako), LGPL-3.0-only (см. NOTICE, COPYING, COPYING.LESSER).
Исходный код перенесённой части лежит в `src/main/java/zako/opt`, конфиг миксинов — `zakoopt.mixins.json`.

Что изменено:

- Перенесено с Minecraft 1.21.11 (intermediary) на 26.2 (имена Mojang).
- Настройки берутся из модуля PizzaDLC «ZakoOpt», а не из `config/zakoopt.json` и system properties; убраны SodiumOptions и зависимость от Iris API.
- Переписаны под 26.2:
  - LOD предметов: `SubmitNodeCollection.submitItem` вместо `ItemRenderer.renderItem`.
  - Без сортировки скинов: `RenderTypeFeatureRenderer$Group.getOrAddDraw` вместо `BufferSource`.
  - Анимация GUI: `GuiItemAtlas.getOrUpdate` вместо лямбды `GuiRenderer.prepareItemElements`.
  - Холст без отвязки: новая сигнатура `clearColorAndDepthTextures(..., Vector4fc, ...)`.
  - Кэш света частиц: `Particle.getLightCoords`.
  - Кэш света блок-сущностей: `LightCoordsUtil.getLightCoords`.
  - Кэш биома поршня: `ClientLevel.getBiome`.
  - Типы рендера: `RenderTypes.entityCutout` вместо `entityCutoutNoCull`.
- Убрано, потому что в 26.2 это уже делает ванилла: один fence на кадр (submit-fence в `GlCommandEncoder`), общие FBO (`FrameBufferCache`), пропуск обводки без светящихся сущностей.
- Убрано, потому что API в 26.2 удалён или переписан: кольцевой буфер вершин и «вершины сразу в кольцо» (нет `MultiBufferSource`), кэш HUD, атлас скинов, кэш моделей табличек, многопоточная сборка вершин частиц, кэш `SubmitNodeStorage.order`.
- Убрано, потому что требует Sodium/FRAPI или ImmediatelyFast: кэш и плоский свет движущихся блоков, кэш буфера Sodium, стек памяти Sodium, пулы буферов ImmediatelyFast.
