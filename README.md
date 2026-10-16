# PizzaDLC

Клиент Minecraft 26.2 на Fabric. Основной Java-пакет: `pizza.dlc`; ID мода и ресурсов: `pizzadlc`.

## Сборка

Нужен JDK 25. Gradle Wrapper включён в репозиторий.

```powershell
.\gradlew.bat jar sourcesJar
```

Готовый клиент: `build/libs/PizzaDLC-1.0-SNAPSHOT.jar`.

```powershell
.\gradlew.bat runClient
.\gradlew.bat verifyNeuro verifyClientLanguages verifyClientDataMigration
```

## Локальные данные

Аккаунты, пароли, настройки, игровые миры, демонстрации Neuro и обученные персональные модели в репозиторий не включаются. Клиент хранит свои данные в папке `pizzadlc` рабочего каталога. При первом запуске после переименования предусмотрена миграция предыдущей папки и совместимость зашифрованных аккаунтов.

Документация Neuro: [tools/NEURO.md](tools/NEURO.md). Python-тренер работает локально; отдельное Python-окружение и PyTorch устанавливаются на машине пользователя. Проверки Python описаны в документации Neuro.

В `libs` находится LuaJ, необходимый текущей сборке. Сведения о лицензиях включённых компонентов находятся в `src/main/resources/licenses` и в самих библиотеках.
