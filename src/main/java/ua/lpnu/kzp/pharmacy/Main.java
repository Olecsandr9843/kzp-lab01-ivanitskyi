package ua.lpnu.kzp.pharmacy;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Консольна програма «Аптека» (варіант 11): читає CSV із препаратами, перевіряє записи,
 * обчислює показники та виводить звіт у консоль і файл.
 */
public final class Main {
    /** Вхідний файл за замовчуванням (відносно поточного каталогу). */
    static final Path DEFAULT_INPUT = Path.of("data", "input.csv");
    /** Файл звіту за замовчуванням (відносно поточного каталогу). */
    static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    /** Код завершення: успіх. */
    static final int EXIT_OK = 0;
    /** Код завершення: помилка читання або запису файла. */
    static final int EXIT_IO_ERROR = 1;
    /** Код завершення: хибні аргументи командного рядка. */
    static final int EXIT_USAGE = 2;

    private static final String USAGE = """
            Використання: java -jar lab01-1.0.0.jar [параметри]

            Параметри:
              --help              показати цю довідку
              --version           показати версію програми та номер збірки
              --input <файл>      вхідний CSV (типово data/input.csv)
              --output <файл>     файл звіту (типово out/report.txt)

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
     *             {@code --input <файл>}, {@code --output <файл>}
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
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> {
                        out.print(USAGE.replace("\n", System.lineSeparator()));
                        return EXIT_OK;
                    }
                    case "--input" -> input = Path.of(requireValue(args, ++i, "--input"));
                    case "--output" -> output = Path.of(requireValue(args, ++i, "--output"));
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
            PharmacyStatistics statistics = new PharmacyStatistics(result.validRecords());
            String report = ReportFormatter.format(input.toString(), statistics, result.errors());

            out.print(report);
            FileReport.writeReport(output, report);
            out.printf("Звіт збережено у файл: %s%n", output);
            return EXIT_OK;
        } catch (IOException exception) {
            // MalformedInputException (підклас IOException) означає, що файл не в UTF-8.
            err.printf("Помилка вводу-виводу: %s (%s)%n", exception.getMessage(),
                    exception.getClass().getSimpleName());
            return EXIT_IO_ERROR;
        }
    }

    /* Повертає значення параметра або повідомляє, що воно відсутнє. */
    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].isBlank()) {
            throw new IllegalArgumentException("після %s потрібно вказати шлях до файла".formatted(option));
        }
        return args[index];
    }
}
