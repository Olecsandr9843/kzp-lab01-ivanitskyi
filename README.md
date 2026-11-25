# Аптека — консольний звіт

Лабораторні роботи № 1–4 з курсу «Кросплатформні засоби програмування», варіант 11.
Поточна версія — **1.3.0** (ЛР4). Звіти: [REPORT.md](REPORT.md) (ЛР4), [docs/REPORT-lab03.md](docs/REPORT-lab03.md) (ЛР3), [docs/REPORT-lab02.md](docs/REPORT-lab02.md) (ЛР2), [docs/REPORT-lab01.md](docs/REPORT-lab01.md) (ЛР1).
Автор: Іваницький.

## Призначення

Програма читає перелік лікарських препаратів із CSV-файла, перевіряє кожен запис,
відкидає хибні рядки з поясненням (номер рядка + причина) і обчислює показники:

1. кількість коректних записів;
2. середню ціну препарату;
3. найменший термін до закінчення придатності;
4. кількість рецептурних препаратів.

Звіт виводиться в консоль і записується у файл.

## Формат входу

Файл у кодуванні UTF-8, один запис у рядку, поля розділені крапкою з комою, без заголовка:

```text
name;form;price;daysToExpire;prescription
```

| Поле           | Тип     | Правило                                  |
|----------------|---------|------------------------------------------|
| `name`         | String  | непорожня назва препарату                |
| `form`         | String  | непорожня форма випуску                  |
| `price`        | double  | число з крапкою, ≥ 0 (наприклад `45.50`) |
| `daysToExpire` | int     | ціле число, ≥ 0                          |
| `prescription` | boolean | `true` або `false` (регістр не важливий) |

Приклад:

```text
Парацетамол;таблетки;45.50;365;false
Амоксицилін;капсули;128.90;180;true
```

Рядок пропускається, якщо він порожній, має не 5 полів, містить порожнє поле, число в
хибному форматі (`12,50`, `хх.50`, `NaN`), від'ємне значення або логічне поле, відмінне від
`true`/`false`. Решта файла при цьому обробляється далі.

## Збірка та запуск

Потрібен JDK 21. Maven окремо встановлювати не треба: Maven Wrapper сам завантажить
потрібну версію під час першого запуску.

Windows (cmd або PowerShell):

```text
mvnw.cmd verify
java -jar target\lab01-1.3.0.jar --version
java -jar target\lab01-1.3.0.jar
java -jar target\lab01-1.3.0.jar --input data\input.csv --output out\report.txt
```

macOS / Ubuntu:

```text
./mvnw verify
java -jar target/lab01-1.3.0.jar --version
java -jar target/lab01-1.3.0.jar
```

`verify` компілює код, запускає тести JUnit 5, збирає виконуваний JAR і перевіряє байт-код
аналізатором SpotBugs. Окремо доступні `test` (лише тести) і `package` (тести + JAR).

### Параметри командного рядка

| Параметр          | Значення                                            |
|-------------------|-----------------------------------------------------|
| `--help`, `-h`    | довідка                                             |
| `--version`       | версія та номер збірки (`1.3.0+local` або `1.3.0+42`) |
| `--input <файл>`  | вхідний CSV, типово `data/input.csv`                |
| `--output <файл>` | файл звіту, типово `out/report.txt`                 |

Коди завершення: `0` — успіх, `1` — помилка читання/запису файла, `2` — хибні аргументи.

### Українські літери в консолі Windows

Файл звіту завжди записується в UTF-8. Щоб і консоль показувала кирилицю правильно,
перед запуском перемкніть кодову сторінку:

```text
chcp 65001
java -jar target\lab01-1.3.0.jar
```

## Приклад

Для `data/input.csv` із репозиторію (4 коректні та 6 хибних рядків):

```text
Звіт аптеки (варіант 11)
Вхідний файл: data/input.csv
Коректних записів: 4
Середня ціна: 86.00 грн
Найменший термін придатності: 45 дн.
Рецептурних препаратів: 2
Пропущено рядків: 6
  Рядок 5: порожнє поле «ціна»
  Рядок 6: від'ємна ціна -310.00
  Рядок 7: очікується 5 полів, отримано 4
  Рядок 8: ціна має бути числом з крапкою, отримано "хх.50"
  Рядок 9: порожній рядок
  Рядок 10: ознака рецептурності має бути true або false, отримано "ні"
Звіт збережено у файл: out/report.txt
```

## Модель даних

```text
                 «sealed abstract» Medicine
        name, form, price, daysToExpire, kind: MedicineKind
        інваріанти · fromCsv · of · equals/hashCode · toString
                abstract int availabilityScore()
                   ┌──────────┴──────────┐
     PrescriptionMedicine           FreeMedicine
     max(10, 60 − штраф)       100 − штраф (− 20, якщо < 30 дн.)
```

- **`Medicine`** — абстрактний `sealed` базовий тип. Тут усі спільні поля та інваріанти:
  непорожні назва й форма, скінченна невід'ємна ціна, невід'ємний термін, категорія не `null`.
  Підтипи їх не дублюють.
- **`PrescriptionMedicine`** і **`FreeMedicine`** відрізняються поведінкою — оцінкою
  доступності `availabilityScore()` від 0 до 100. Штраф за ціну — 1 бал за кожні повні 10 грн,
  не більше 50. Препарат, термін якого спливає сьогодні, отримує 0.
- **`enum MedicineKind`** — `PRESCRIPTION("за рецептом")`, `FREE("без рецепта")`. Дає підпис
  для звіту й ознаку рецептурності.
- **Рівність**: два препарати рівні, якщо мають однаковий підтип, назву та форму випуску.
  Ціна й термін — властивості партії. `--details` рахує унікальні препарати через `HashSet`.
- `Medicine.fromCsv` / `Medicine.of` — єдине місце, де вибирається підтип (за полем
  `prescription`). Уся решта коду працює з `List<Medicine>`.
- Показники 3 і 4 повертаються разом як record **`ExpiryPrescription`**.

Приклад `--details` для `data/input.csv`:

```text
Асортимент:
  Парацетамол (таблетки): 45.50 грн, 365 дн., без рецепта — доступність 96/100
  Амоксицилін (капсули): 128.90 грн, 180 дн., за рецептом — доступність 48/100
  Ібупрофен (суспензія): 96.00 грн, 90 дн., без рецепта — доступність 91/100
  Еналаприл (таблетки): 73.60 грн, 45 дн., за рецептом — доступність 53/100
Унікальних препаратів: 4
Середня оцінка доступності: 72.00
```

## Запити Stream API (`--queries`, `--find`)

Клас `MedicineQueries` містить п'ять запитів і пошук. Кожен з них — окремий конвеєр без
побічних ефектів.

| Запит | Конвеєр | Результат |
|-------|---------|-----------|
| `expiringSoon` | `filter(days ≤ 30)` → `toList` | `List<Medicine>` |
| `names` | `map(getName)` → `distinct` → `toList` | `List<String>` |
| `countByForm` | `groupingBy(getForm, TreeMap::new, counting())` | `Map<String, Long>` |
| `countByKindAndForm` | `groupingBy(getKind, groupingBy(getForm, counting()))` | `Map<MedicineKind, Map<String, Long>>` |
| `priceStatistics` | `summarizingDouble(getPrice)` | `DoubleSummaryStatistics` |
| `topNearestExpiry(n)` | `sorted(термін ↑, назва ↑)` → `limit(n)` | `List<Medicine>` |
| `topByAvailability(n)` | `sorted(оцінка ↓ reversed, назва ↑)` → `limit(n)` | `List<Medicine>` |
| `findByName` | `filter(назва без регістру)` → `findFirst` | `Optional<Medicine>` |

Контрольний набір для запитів — `data/pharmacy-full.csv` (13 препаратів). Файл `data/input.csv`
не змінюється, бо від нього залежить порівняння з контрольним звітом ЛР1.

```text
java -jar target/lab01-1.3.0.jar --input data/pharmacy-full.csv --queries --find Назол
```

## Структура

```text
src/main/java/ua/lpnu/kzp/pharmacy/
  Main.java                — аргументи командного рядка, запуск конвеєра
  FileReport.java          — читання/запис файлів через Path і UTF-8
  RecordProcessor.java     — обхід рядків, номер рядка в повідомленні про помилку
  Medicine.java            — абстрактний sealed базовий тип + fromCsv (ЛР2–ЛР3)
  PrescriptionMedicine.java, FreeMedicine.java — підтипи (ЛР3)
  MedicineKind.java        — enum категорії відпуску (ЛР3)
  ExpiryPrescription.java  — record підсумку (ЛР2)
  ProcessingResult.java    — коректні препарати + помилки
  PharmacyStatistics.java  — чотири показники варіанта
  MedicineQueries.java     — запити Stream API та пошук Optional (ЛР4)
  ReportFormatter.java     — текст звіту, розділи --details/--queries/--find (Locale.ROOT, %n)
  BuildInfo.java           — версія з version.properties
src/test/java/...          — тести JUnit 5
src/test/resources/lab01-baseline-report.txt — контрольний звіт ЛР1
data/input.csv             — приклад вхідних даних (контрольний для звіту)
data/pharmacy-full.csv     — контрольний набір для запитів ЛР4
docs/                      — звіти попередніх робіт
ai/                        — ролі ШІ-агентів
.github/                   — CI, шаблони Issue та PR
```
