# PizzaDLC

Клиент **Minecraft 26.2** на **Fabric**. Основной Java-пакет — `pizza.dlc`, ID мода и ресурсов — `pizzadlc`.

**Почти весь клиент сделан с помощью нейросети**: написание и адаптация кода, интерфейсы, перенос функций и исправление ошибок выполнялись при её участии.

## Как запустить из исходников

### Что нужно

- **JDK 25, 64-bit** — именно комплект разработчика Java, содержащий `java` и `javac`.
- **Git** для клонирования репозитория.
- Доступ к этому приватному репозиторию и интернет для первой загрузки Gradle и зависимостей.

Gradle отдельно устанавливать не нужно: Wrapper включён в исходники.

### Windows / PowerShell

```powershell
git clone https://github.com/Wepopuk/PizzaDLC.git
cd PizzaDLC
java -version
javac -version
.\gradlew.bat runClient
```

Обе проверки Java должны показать версию **25**. Если установлено несколько JDK, укажи нужный в текущем PowerShell (замени путь на свой):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat runClient
```

Первый запуск скачает необходимые зависимости и откроет Minecraft с PizzaDLC в среде разработки. Игровые файлы находятся в папке `run`.

### Linux / macOS

С установленным JDK 25:

```bash
git clone https://github.com/Wepopuk/PizzaDLC.git
cd PizzaDLC
chmod +x gradlew
./gradlew runClient
```

### IntelliJ IDEA

Открой папку проекта как Gradle-проект. Установи **Project SDK = JDK 25** и **Gradle JVM = JDK 25**, дождись синхронизации и запусти Gradle-задачу `runClient`.

## Как собрать JAR

Windows:

```powershell
.\gradlew.bat jar sourcesJar
```

Linux / macOS:

```bash
./gradlew jar sourcesJar
```

Результат:

- `build/libs/PizzaDLC-1.0-SNAPSHOT.jar` — клиент для установки в профиль Minecraft 26.2 с Fabric Loader и Fabric API соответствующей версии.
- `build/libs/PizzaDLC-1.0-SNAPSHOT-sources.jar` — архив исходников.

Перед пересборкой закрой запущенный из этого проекта Minecraft, если Windows сообщает, что JAR в кэше Loom занят другим процессом.

## Проверки

```powershell
.\gradlew.bat verifyNeuro verifyClientLanguages verifyClientDataMigration
```

Для полного набора проверок:

```powershell
.\gradlew.bat check
```

## Локальные данные и Neuro

Аккаунты, пароли, настройки, игровые миры, демонстрации Neuro и обученные персональные модели в репозиторий не включены. Клиент хранит данные в папке `pizzadlc` рабочего каталога; при запуске через `runClient` это обычно `run/pizzadlc`. После переименования клиента предусмотрена миграция предыдущей папки данных и совместимость зашифрованных аккаунтов.

Для обычного запуска клиента Python не требуется. Для **обучения Neuro** нужны отдельное локальное Python-окружение и PyTorch. Инструкции, команды тренировки и проверки находятся в [tools/NEURO.md](tools/NEURO.md). Тренер работает локально.

## Включённые компоненты

В `libs` находится LuaJ, необходимый текущей сборке. Сведения о лицензиях включённых компонентов находятся в `src/main/resources/licenses` и в самих библиотеках.
