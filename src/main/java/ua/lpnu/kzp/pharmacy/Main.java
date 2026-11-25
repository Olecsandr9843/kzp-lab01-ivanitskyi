package ua.lpnu.kzp.pharmacy;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import ua.lpnu.kzp.storage.DataStorageException;
import ua.lpnu.kzp.storage.Repository;

/**
 * Консольна програма «Аптека» (варіант 11): читає CSV із препаратами, перевіряє записи,
 * обчислює показники та виводить звіт у консоль і файл.
 */
public final class Main {
    /** Вхідний файл за замовчуванням (відносно поточного каталогу). */
    static final Path DEFAULT_INPUT = Path.of("data", "input.csv");
    /** Файл звіту за замовчуванням (відносно поточного каталогу). */
    static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /** Скільки елементів показувати в рейтингах розділу {@code --queries}. */
    static final int TOP_N = 5;

    /** Код завершення: успіх. */
    static final int EXIT_OK = 0;
    /** Код завершення: помилка читання або запису файла. */
    static final int EXIT_IO_ERROR = 1;
    /** Код завершення: хибні аргументи командного рядка. */
    static final int EXIT_USAGE = 2;

    private static final String USAGE = """
            Використання: java -jar lab01-1.4.0.jar [параметри]

            Параметри:
              --help              показати цю довідку
              --version           показати версію програми та номер збірки
              --input <файл>      вхідний CSV (типово data/input.csv)
              --output <файл>     файл звіту (типово out/report.txt)
              --details           додати перелік препаратів з оцінкою доступності
              --queries           додати результати запитів Stream API (фільтр, групування, топ-5…)
              --find <назва>      знайти препарат за назвою
              --export <файл>     зберегти асортимент у CSV (або JSON, якщо файл .json)
              --date <РРРР-ММ-ДД> опорна дата для --export (типово сьогодні)

            Формат рядка CSV (UTF-8, роздільник ;):
              name;form;price;daysToExpire;prescription
              Парацетамол;таблетки;45.50;365;false
            """;

    /* Забороняє створення екземплярів класу з точкою входу. */
    private Main() {
    }

    /**
     * Точка входу до програми.
     *
     * @param args аргументи командного рядка: {@code --help}, {@code --version},
     *             {@code --input <файл>}, {@code --output <файл>}, {@code --details},
     *             {@code --queries}, {@code --find <назва>}, {@code --export <файл>},
     *             {@code --date <РРРР-ММ-ДД>}
     */
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != EXIT_OK) {
            // Ненульовий код дає змогу скриптам і CI розпізнати помилку.
            System.exit(exitCode);
        }
    }

    /**
     * Виконує програму з указаними потоками виводу. Метод відокремлено від {@code main},
     * щоб тести могли перевірити поведінку без завершення JVM.
     *
     * @param args аргументи командного рядка
     * @param out  потік для звіту та довідки
     * @param err  потік для повідомлень про помилки
     * @return код завершення: {@value #EXIT_OK}, {@value #EXIT_IO_ERROR} або {@value #EXIT_USAGE}
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        // --version перевіряємо до іншої обробки аргументів.
        if (args.length == 1 && "--version".equals(args[0])) {
            out.printf("%s%n", BuildInfo.version());
            return EXIT_OK;
        }

        Path input = DEFAULT_INPUT;
        Path output = DEFAULT_OUTPUT;
        boolean details = false;
        boolean queries = false;
        String find = null;
        Path export = null;
        LocalDate today = LocalDate.now();
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> {
                        out.print(USAGE.replace("\n", System.lineSeparator()));
                        return EXIT_OK;
                    }
                    case "--input" -> input = Path.of(requireValue(args, ++i, "--input", "шлях до файла"));
                    case "--output" -> output = Path.of(requireValue(args, ++i, "--output", "шлях до файла"));
                    case "--details" -> details = true;
                    case "--queries" -> queries = true;
                    case "--find" -> find = requireValue(args, ++i, "--find", "назву препарату");
                    case "--export" -> export = Path.of(requireValue(args, ++i, "--export", "шлях до файла"));
                    case "--date" -> today = parseDate(requireValue(args, ++i, "--date", "дату РРРР-ММ-ДД"));
                    default -> throw new IllegalArgumentException("невідомий параметр " + args[i]);
                }
            }
        } catch (IllegalArgumentException exception) {
            // Сюди потрапляє й InvalidPathException (її підклас) для недопустимих символів у шляху.
            err.printf("Помилка аргументів: %s%n", exception.getMessage());
            err.printf("Запустіть із --help, щоб побачити довідку.%n");
            return EXIT_USAGE;
        }

        if (!Files.isRegularFile(input)) {
            err.printf("Помилка: вхідний файл не знайдено: %s%n", input);
            return EXIT_IO_ERROR;
        }

        try {
            List<String> lines = FileReport.readLines(input);
            ProcessingResult result = RecordProcessor.process(lines);
            // ЛР5: дані зберігаються в параметризованому сховищі, решта коду працює з його знімком.
            Repository<Medicine> repository = new Repository<>();
            repository.addAll(result.validRecords());
            List<Medicine> medicines = repository.all();
            PharmacyStatistics statistics = new PharmacyStatistics(medicines);
            String report = ReportFormatter.format(input.toString(), statistics, result.errors());
            if (details) {
                report += ReportFormatter.formatDetails(medicines);
            }
            if (queries) {
                report += ReportFormatter.formatQueries(medicines, TOP_N);
            }
            if (find != null) {
                report += ReportFormatter.formatSearch(find, MedicineQueries.findByName(medicines, find));
            }

            out.print(report);
            FileReport.writeReport(output, report);
            out.printf("Звіт збережено у файл: %s%n", output);
            if (export != null) {
                out.printf("%s%n", MedicineArchive.export(medicines, export, today));
            }
            return EXIT_OK;
        } catch (DataStorageException exception) {
            // Повідомлення власного винятку + первинна причина, щоб не втратити деталі.
            err.printf("Помилка збереження даних: %s%n", exception.getMessage());
            if (exception.getCause() != null) {
                err.printf("Причина: %s%n", exception.getCause());
            }
            return EXIT_IO_ERROR;
        } catch (IOException exception) {
            // MalformedInputException (підклас IOException) означає, що файл не в UTF-8.
            err.printf("Помилка вводу-виводу: %s (%s)%n", exception.getMessage(),
                    exception.getClass().getSimpleName());
            return EXIT_IO_ERROR;
        }
    }

    /* Розбирає дату ISO; хибну дату перетворює на зрозуміле повідомлення, зберігаючи причину. */
    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("хибна дата «%s», очікується РРРР-ММ-ДД".formatted(text), exception);
        }
    }

    /* Повертає значення параметра або повідомляє, що воно відсутнє. */
    private static String requireValue(String[] args, int index, String option, String what) {
        if (index >= args.length || args[index].isBlank()) {
            throw new IllegalArgumentException("після %s потрібно вказати %s".formatted(option, what));
        }
        return args[index];
    }
}
