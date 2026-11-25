package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Перевіряє параметри {@code --export} і {@code --date}. */
class ExportCliTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);

    private int run(String... extra) {
        String[] base = {"--input", Path.of("data", "input.csv").toString(),
            "--output", tempDir.resolve("report.txt").toString()};
        String[] args = new String[base.length + extra.length];
        System.arraycopy(base, 0, args, 0, base.length);
        System.arraycopy(extra, 0, args, base.length, extra.length);
        return Main.run(args, out, err);
    }

    @Test
    void exportsCsvAndConfirmsRoundTrip() throws IOException {
        Path csv = tempDir.resolve("export").resolve("medicines.csv");

        int code = run("--export", csv.toString(), "--date", "2026-11-25");

        assertEquals(Main.EXIT_OK, code, errBytes.toString(StandardCharsets.UTF_8));
        assertEquals("придатність,форма,назва,рецепт,ціна" + NL
                + "2027-11-25,таблетки,Парацетамол,FREE,45.5" + NL
                + "2027-05-24,капсули,Амоксицилін,PRESCRIPTION,128.9" + NL
                + "2027-02-23,суспензія,Ібупрофен,FREE,96.0" + NL
                + "2027-01-09,таблетки,Еналаприл,PRESCRIPTION,73.6" + NL,
                Files.readString(csv, StandardCharsets.UTF_8));
        assertTrue(outBytes.toString(StandardCharsets.UTF_8).contains("round-trip: відновлено 4, усі рівні початковим"));
    }

    @Test
    void exportsJsonByExtension() throws IOException {
        Path json = tempDir.resolve("medicines.json");

        assertEquals(Main.EXIT_OK, run("--export", json.toString(), "--date", "2026-11-25"));

        String text = Files.readString(json, StandardCharsets.UTF_8);
        assertTrue(text.startsWith("[" + NL + "  {\"придатність\": \"2027-11-25\""), text);
        assertTrue(text.contains("\"ціна\": 73.6}"), text);
    }

    /* Звіт від --export не змінюється. */
    @Test
    void reportIsUnchangedByExport() throws IOException {
        run("--export", tempDir.resolve("m.csv").toString());

        String report = Files.readString(tempDir.resolve("report.txt"), StandardCharsets.UTF_8);
        assertTrue(report.contains("Коректних записів: 4" + NL), report);
        assertTrue(!report.contains("Експортовано"), report);
    }

    @Test
    void invalidDateIsUsageError() {
        assertEquals(Main.EXIT_USAGE, run("--date", "2026-13-01"));
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).contains("хибна дата «2026-13-01»"));
    }

    /* Файлова помилка: шлях експорту — існуючий каталог. Повідомлення містить і причину. */
    @Test
    void fileErrorIsReportedWithCause() {
        int code = run("--export", tempDir.toString());

        String errors = errBytes.toString(StandardCharsets.UTF_8);
        assertEquals(Main.EXIT_IO_ERROR, code);
        assertTrue(errors.contains("Помилка збереження даних: не вдалося записати файл"), errors);
        assertTrue(errors.contains("Причина:"), errors);
    }
}
