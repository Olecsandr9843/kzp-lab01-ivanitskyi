package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Наскрізні тести: аргументи командного рядка, звіт у консоль і файл, коди завершення. */
class MainTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    /* Повний прогін: звіт у файлі збігається з тим, що виведено в консоль. */
    @Test
    void writesSameReportToConsoleAndFile() throws IOException {
        Path input = tempDir.resolve("input.csv");
        Path output = tempDir.resolve("nested").resolve("report.txt");
        Files.write(input, List.of(
                "Парацетамол;таблетки;45.50;365;false",
                "Амоксицилін;капсули;128.90;180;true",
                "Ібупрофен;суспензія;96.00;90;false",
                "Еналаприл;таблетки;73.60;45;true",
                "Цефтріаксон;порошок;-310.00;400;true",
                "Вітамін D3;краплі;210.00;730"), StandardCharsets.UTF_8);

        int code = Main.run(new String[] {"--input", input.toString(), "--output", output.toString()}, out, err);

        assertEquals(Main.EXIT_OK, code, err());
        String fileReport = Files.readString(output, StandardCharsets.UTF_8);
        String expected = "Звіт аптеки (варіант 11)" + NL
                + "Вхідний файл: " + input + NL
                + "Коректних записів: 4" + NL
                + "Середня ціна: 86.00 грн" + NL
                + "Найменший термін придатності: 45 дн." + NL
                + "Рецептурних препаратів: 2" + NL
                + "Пропущено рядків: 2" + NL
                + "  Рядок 5: від'ємна ціна -310.00" + NL
                + "  Рядок 6: очікується 5 полів, отримано 4" + NL;
        assertEquals(expected, fileReport);
        assertTrue(out().startsWith(expected), out());
    }

    @Test
    void reportsEmptyInputWithoutFailure() throws IOException {
        Path input = tempDir.resolve("empty.csv");
        Files.writeString(input, "", StandardCharsets.UTF_8);
        Path output = tempDir.resolve("report.txt");

        int code = Main.run(new String[] {"--input", input.toString(), "--output", output.toString()}, out, err);

        assertEquals(Main.EXIT_OK, code);
        assertTrue(out().contains("Коректних записів: 0"), out());
        assertTrue(out().contains("Середня ціна: 0.00 грн"), out());
        assertTrue(out().contains("Найменший термін придатності: немає даних"), out());
    }

    @Test
    void helpPrintsUsage() {
        int code = Main.run(new String[] {"--help"}, out, err);

        assertEquals(Main.EXIT_OK, code);
        assertTrue(out().contains("--input <файл>"), out());
    }

    @Test
    void versionPrintsProjectVersionAndBuild() {
        int code = Main.run(new String[] {"--version"}, out, err);

        assertEquals(Main.EXIT_OK, code);
        assertTrue(out().strip().matches("\\d+\\.\\d+\\.\\d+\\+\\w+"), out());
    }

    @Test
    void missingInputFileIsReported() {
        Path missing = tempDir.resolve("missing.csv");

        int code = Main.run(new String[] {"--input", missing.toString()}, out, err);

        assertEquals(Main.EXIT_IO_ERROR, code);
        assertTrue(err().contains("не знайдено"), err());
    }

    @Test
    void unknownOptionAndMissingValueAreUsageErrors() {
        assertEquals(Main.EXIT_USAGE, Main.run(new String[] {"--colour"}, out, err));
        assertEquals(Main.EXIT_USAGE, Main.run(new String[] {"--input"}, out, err));
        assertTrue(err().contains("--input"), err());
    }
}
