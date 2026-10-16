"""Add native combat module labels to PizzaDLC's five existing language catalogs."""
from pathlib import Path
import json, re

ROOT=Path(__file__).resolve().parents[1]
ROWS='''Дистанция атаки|Attack range|Дистанція атаки|Zasięg ataku|Angriffsreichweite
Дистанция ротации|Rotation range|Дистанція ротації|Zasięg obrotu|Rotationsreichweite
Дистанция обнаружения|Detection range|Дистанція виявлення|Zasięg wykrywania|Erkennungsreichweite
Дистанция отлета|Retreat distance|Дистанція відльоту|Odległość odwrotu|Rückzugsdistanz
Тип ротации|Rotation mode|Тип ротації|Tryb obrotu|Rotationsmodus
Вид ротации|Rotation style|Вид ротації|Styl obrotu|Rotationsstil
Фактор высоты|Height factor|Коефіцієнт висоти|Współczynnik wysokości|Höhenfaktor
Скорость Генерации|Generation speed|Швидкість генерації|Szybkość generowania|Generierungsgeschwindigkeit
Предсказание|Prediction|Передбачення|Przewidywanie|Vorhersage
Скорость аима|Aim speed|Швидкість наведення|Szybkość celowania|Zielgeschwindigkeit
FOV аима|Aim field of view|Поле зору наведення|Pole widzenia celowania|Zielsichtfeld
Множитель скорости|Speed multiplier|Множник швидкості|Mnożnik prędkości|Geschwindigkeitsmultiplikator
Доля вертикали|Vertical share|Вертикальна частка|Udział pionowy|Vertikaler Anteil
Резкость доводки|Aim sharpness|Різкість наведення|Ostrość celowania|Zielschärfe
Дрожание гориз.|Horizontal jitter|Горизонтальне тремтіння|Drgania poziome|Horizontales Zittern
Дрожание верт.|Vertical jitter|Вертикальне тремтіння|Drgania pionowe|Vertikales Zittern
Точка прицела|Aim point|Точка наведення|Punkt celowania|Zielpunkt
Тип сортировки|Target sorting|Сортування цілей|Sortowanie celów|Zielsortierung
Умная|Smart|Розумне|Inteligentne|Intelligent
Ускорение|Acceleration|Прискорення|Przyspieszenie|Beschleunigung
Тики|Ticks|Тіки|Ticki|Ticks
Таргеты|Targets|Цілі|Cele|Ziele
Включить ClientLook|Enable ClientLook|Увімкнути ClientLook|Włącz ClientLook|ClientLook aktivieren
Отображение цели|Target effect|Ефект цілі|Efekt celu|Zieleffekt
Слои желе|Jelly layers|Шари желе|Warstwy galaretki|Geleeschichten
Размер частиц желе|Jelly particle size|Розмір частинок желе|Rozmiar cząstek galaretki|Größe der Geleepartikel
Желе|Jelly|Желе|Galaretka|Gelee
Тип кольца|Ring type|Тип кільця|Typ pierścienia|Ringtyp
Тип квадрата|Square type|Тип квадрата|Typ kwadratu|Quadrattyp
Тип призраков|Ghost type|Тип привидів|Typ duchów|Geistertyp
Тип кристалликов|Crystal type|Тип кристаликів|Typ kryształów|Kristalltyp
Тип молний|Lightning type|Тип блискавок|Typ błyskawic|Blitztyp
Просвечивать кристаллы|Crystals through walls|Кристали крізь стіни|Kryształy przez ściany|Kristalle durch Wände
Просвечивать|Through walls|Крізь стіни|Przez ściany|Durch Wände
Освещение|Surface lighting|Освітлення поверхні|Oświetlenie powierzchni|Oberflächenbeleuchtung
Скорость кольца|Ring speed|Швидкість кільця|Prędkość pierścienia|Ringgeschwindigkeit
Кол-во душ|Soul count|Кількість душ|Liczba dusz|Seelenanzahl
Красный при ударе|Red on hit|Червоний при ударі|Czerwony przy trafieniu|Rot bei Treffer
Авто цвет под тему|Use theme colors|Кольори теми|Użyj kolorów motywu|Designfarben verwenden
Цвет отображения|Effect color|Колір ефекту|Kolor efektu|Effektfarbe
Цвет градиента кристаллов|Crystal gradient color|Колір градієнта кристалів|Kolor gradientu kryształów|Kristallverlaufsfarbe
Искажение|Distortion|Спотворення|Zniekształcenie|Verzerrung
Сила искажения|Distortion strength|Сила спотворення|Siła zniekształcenia|Verzerrungsstärke
Элитра лаг|Elytra lag|Лаг елітр|Opóźnienie elytry|Elytra-Verzögerung
Элитра перегон|Elytra overtake|Обгін на елітрах|Wyprzedzanie na elytrze|Elytra-Überholen
Режим предикта|Prediction mode|Режим передбачення|Tryb przewidywania|Vorhersagemodus
Сила предикта|Prediction strength|Сила передбачення|Siła przewidywania|Vorhersagestärke
Бить когда|Attack when|Атакувати коли|Atakuj gdy|Angreifen wenn
Предиктить от бпс|Prediction BPS threshold|Поріг BPS передбачення|Próg BPS przewidywania|BPS-Schwelle der Vorhersage
Автоматически использовать фейверк|Use fireworks automatically|Автоматично використовувати феєрверки|Automatycznie używaj fajerwerków|Feuerwerk automatisch verwenden
Моментальный отклик|Instant response|Миттєва реакція|Natychmiastowa reakcja|Sofortige Reaktion
Рендерить предикт|Show prediction|Показувати передбачення|Pokaż przewidywanie|Vorhersage anzeigen
Мод|Profile|Профіль|Profil|Profil
Рисовать текущий Yaw или Pitch|Show yaw and pitch|Показувати yaw і pitch|Pokaż yaw i pitch|Yaw und Pitch anzeigen
Бустер по X|Yaw boost|Буст за yaw|Przyspieszenie według yaw|Yaw-Verstärkung
Бустер по Y|Pitch boost|Буст за pitch|Przyspieszenie według pitch|Pitch-Verstärkung
Возвращать скорость|Restore speed|Відновлювати швидкість|Przywróć prędkość|Geschwindigkeit wiederherstellen
Останавливает полёт рядом с целью|Stops flight near the target|Зупиняє політ поруч із ціллю|Zatrzymuje lot w pobliżu celu|Stoppt den Flug nahe dem Ziel
Преследует игрока на элитрах|Follows a player while gliding|Переслідує гравця на елітрах|Śledzi gracza podczas lotu|Verfolgt einen Spieler im Gleitflug
Усиливает ускорение от фейерверков при полёте на элитрах|Boosts firework acceleration while gliding|Посилює прискорення від феєрверків під час польоту|Zwiększa przyspieszenie fajerwerków podczas lotu|Verstärkt die Feuerwerksbeschleunigung im Gleitflug
Автоматически наводится и бьет таргетов|Automatically aims and attacks targets|Автоматично наводиться й атакує цілі|Automatycznie celuje i atakuje cele|Zielt und greift automatisch an
Ближайшая точка|Closest point|Найближча точка|Najbliższy punkt|Nächster Punkt
Голова|Head|Голова|Głowa|Kopf
Центр тела|Body center|Центр тіла|Środek ciała|Körpermitte
Проекция|Projection|Проєкція|Projekcja|Projektion
Дефенсив|Defensive|Захисна|Defensywny|Defensiv
Джиттер|Jitter|Тремтіння|Drgania|Zittern
Обычная|Normal|Звичайна|Normalny|Normal
Незаметная|Silent|Непомітна|Dyskretna|Unauffällig
Сфокусированная|Focused|Сфокусована|Skupiona|Fokussiert
Полноценная|Full|Повноцінна|Pełna|Vollständig
От ротации|By rotation|За ротацією|Według obrotu|Nach Rotation
От скорости|By velocity|За швидкістю|Według prędkości|Nach Geschwindigkeit
Перегнал|After overtaking|Після обгону|Po wyprzedzeniu|Nach dem Überholen
Квадрат|Square|Квадрат|Kwadrat|Quadrat
Кольцо|Ring|Кільце|Pierścień|Ring
Кристалики|Crystals|Кристалики|Kryształy|Kristalle
Куб|Cube|Куб|Sześcian|Würfel
Молнии|Lightning|Блискавки|Błyskawice|Blitze
Призраки|Ghosts|Привиди|Duchy|Geister
Пентограмма|Pentagram|Пентаграма|Pentagram|Pentagramm
Ромбики|Diamonds|Ромбики|Romby|Rauten
Обруч|Hoop|Обруч|Obręcz|Reifen
Треугольник|Triangle|Трикутник|Trójkąt|Dreieck
Проверять кулдаун|Check cooldown|Перевіряти перезарядку|Sprawdzaj czas odnowienia|Abklingzeit prüfen
Проверять наводку|Check aim|Перевіряти наведення|Sprawdzaj celowanie|Zielausrichtung prüfen
Только в прыжке|Only while jumping|Лише в стрибку|Tylko podczas skoku|Nur beim Springen
Не бить когда ешь|Do not attack while eating|Не атакувати під час їжі|Nie atakuj podczas jedzenia|Beim Essen nicht angreifen
Обход через стены|Wall bypass|Обхід крізь стіни|Omijanie ścian|Wände umgehen
Тпс синх|TPS synchronization|Синхронізація TPS|Synchronizacja TPS|TPS-Synchronisierung'''
ROWS += "\n" + "Проверки|Checks|Перевірки|Sprawdzenia|Prüfungen\nЗолотые сердца|Absorption hearts|Золоті серця|Złote serca|Absorptionsherzen\nКристаллы|Crystals|Кристали|Kryształy|Kristalle\nДинамит|TNT|Динаміт|TNT|TNT\nЗдоровье обратного свапа|Health margin for swapping back|Запас здоров'я для повернення предмета|Zapas zdrowia do przywrócenia przedmiotu|Gesundheitspuffer zum Zurückwechseln\nЗдоровье на элитре|Health while gliding|Здоров'я на елітрі|Zdrowie podczas lotu elytrą|Gesundheit beim Elytra-Flug\nДистанция до кристалла|Crystal distance|Відстань до кристала|Odległość od kryształu|Kristallabstand\nДистанция до якоря|Anchor distance|Відстань до якоря|Odległość od kotwicy|Ankerabstand\nДистанция падения|Fall distance|Відстань падіння|Odległość upadku|Fallstrecke\nДистанция до динамита|TNT distance|Відстань до динаміту|Odległość od TNT|TNT-Abstand\nТики задержки свапа|Swap delay in ticks|Затримка заміни в тіках|Opóźnienie zamiany w tickach|Wechselverzögerung in Ticks\nСохранять зачарованный тотем|Preserve enchanted totems|Зберігати зачаровані тотеми|Oszczędzaj zaklęte totemy|Verzauberte Totems schonen\nНе свапать драконитки|Preserve Dragonite totems|Зберігати драконітки|Oszczędzaj totemy Dragonite|Dragonite-Totems schonen\nНе свапать шар|Keep the sphere|Зберігати кулю|Zachowaj kulę|Sphäre behalten\nКастомное здоровье с шаром|Custom health with sphere|Власне здоров'я з кулею|Własny próg zdrowia z kulą|Eigene Gesundheit mit Sphäre\nЗдоровье с шаром|Health with sphere|Здоров'я з кулею|Zdrowie z kulą|Gesundheit mit Sphäre\nЗадержка свапа на кристал|Crystal threat hold in ticks|Затримка після кристала в тіках|Czas ochrony po krysztale w tickach|Kristallschutzdauer in Ticks\nЗадержка свапа на якорь|Anchor threat hold in ticks|Затримка після якоря в тіках|Czas ochrony po kotwicy w tickach|Ankerschutzdauer in Ticks\nРежим свапов|Swap mode|Режим заміни|Tryb zamiany|Wechselmodus\nРейджовый|Aggressive|Агресивний|Agresywny|Aggressiv\nЛегитный|Legit|Звичайний|Zwykły|Normal\nТики остановки|Movement pause in ticks|Тіки зупинки|Ticki zatrzymania|Bewegungspause in Ticks\nАвтоматически берет тотем при различных факторах|Automatically equips a totem when danger is detected|Автоматично бере тотем за різних небезпек|Automatycznie zakłada totem po wykryciu zagrożenia|Rüstet bei erkannter Gefahr automatisch ein Totem aus\nдраконид|драконид|драконид|драконид|драконид\nдраконит|драконит|драконит|драконит|драконит"

ROWS += "\n" + "Общий|Global|Загальний|Ogólny|Global\nСмена на наземную цель|Switch to a ground target|Зміна на наземну ціль|Zmiana na cel na ziemi|Zu einem Bodenziel wechseln\nДистанция смены цели|Target switch distance|Дистанція зміни цілі|Odległość zmiany celu|Abstand für Zielwechsel\nМод запуска фейверка|Firework launch mode|Режим запуску феєрверка|Tryb odpalania fajerwerku|Feuerwerksstartmodus\nАвто взлет|Automatic takeoff|Автоматичний зліт|Automatyczny start|Automatischer Start\nСвап элитры, авто взлет и фейерверк по бинду|Elytra swap, automatic takeoff and bound firework launch|Зміна елітри, автоматичний зліт і феєрверк за клавішею|Zmiana elytry, automatyczny start i fajerwerk pod klawiszem|Elytra-Wechsel, automatischer Start und Feuerwerk per Taste\nНикакой|None|Жодний|Brak|Keiner\nПамять таргета|Target memory|Пам'ять цілі|Pamięć celu|Zielgedächtnis\nМинимальная дистанция|Minimum distance|Мінімальна відстань|Minimalna odległość|Mindestabstand\nКомпенсировать прыжком|Compensate with a jump|Компенсувати стрибком|Kompensuj skokiem|Durch Springen ausgleichen\nНе кидать в энтити|Avoid hitting entities|Не влучати в сутності|Unikaj trafiania istot|Treffer auf Entitäten vermeiden\nУчитывать воду|Account for water|Враховувати воду|Uwzględniaj wodę|Wasser berücksichtigen\nАвто ловушка|Automatic trap|Автоматична пастка|Automatyczna pułapka|Automatische Falle\nТип ловушки|Trap type|Тип пастки|Typ pułapki|Fallentyp\nЭндер Ловушка|Ender trap|Ендер-пастка|Pułapka Endera|Enderfalle\nЛовушка|Trap|Пастка|Pułapka|Falle\nЛюбая|Any|Будь-яка|Dowolna|Beliebig\nловушка|trap|пастка|pułapka|Falle\nэндер ловушка|ender trap|ендер-пастка|pułapka Endera|Enderfalle"

ROWS += "\n" + "Нагрудника|Chestplate|Нагрудника|Napierśnika|Brustpanzer\nОбход|Bypass|Обхід|Obejście|Umgehung"

translations={}
for row in ROWS.splitlines():
    source,en,uk,pl,de=row.split('|');translations[source]=dict(en=en,ru=source,uk=uk,pl=pl,de=de)
extra={
 'ElytraTarget':['Elytra Target','Элитра таргет','Ціль на елітрах','Cel elytry','Elytra-Ziel'],
 'ElytraMotion':['Elytra Motion','Элитра моушен','Рух на елітрах','Ruch elytry','Elytra-Bewegung'],
 'ElytraBooster':['Elytra Booster','Элитра бустер','Бустер елітр','Przyspieszenie elytry','Elytra-Verstärker'],
 'Custom':['Custom','Свой','Власний','Własny','Benutzerdefiniert'],
 'Model loaded successfully!':['Model loaded successfully!','Модель успешно загружена!','Модель успішно завантажена!','Model został załadowany!','Modell erfolgreich geladen!']}
for source,values in extra.items():translations[source]=dict(zip(('en','ru','uk','pl','de'),values))
sources=set()
for name in ('combat/AttackAura','combat/ElytraTarget','movement/ElytraMotion','movement/ElytraBooster','combat/AutoTotem','combat/TargetPearl','player/SwapSettings','player/ElytraHelper','movement/InventoryMove'):
    source=(ROOT/'src/main/java/pizza/dlc/client/modules/impl'/(name+'.java')).read_text(encoding='utf-8')
    sources.update(re.findall(r'"([^"\n]*)"',source))
for code in ('en','ru','uk','pl','de'):
    path=ROOT/'src/main/resources/assets/pizzadlc/lang/client'/(code+'.json')
    catalog=json.loads(path.read_text(encoding='utf-8'));labels=catalog['labels']
    for text in sources:
        if text in labels or not text.strip():continue
        if text in translations:value=translations[text][code]
        elif match:=re.fullmatch(r'(\d+-\d+) По ([XY])',text):value=match[1]+'° '+('Yaw' if match[2]=='X' else 'Pitch')
        elif match:=re.fullmatch(r'Тип (\d+)',text):value={'en':'Type ','ru':'Тип ','uk':'Тип ','pl':'Typ ','de':'Typ '}[code]+match[1]
        elif re.search('[А-Яа-яЁё]',text):raise ValueError('Missing translation: '+text)
        else:value=text
        labels[text]=value
    # Include descriptions from the explicit Module constructor as well.
    for text,values in translations.items():labels.setdefault(text,values[code])
    path.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Combat labels updated in all five languages')
