package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Перевіряє розділи звіту {@code --queries} і {@code --find}. */
class QueriesReportTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    private String runWith(String... extra) throws IOException {
        Path output = tempDir.resolve("report-" + extra.length + ".txt");
        String[] base = {"--input", Path.of("data", "pharmacy-full.csv").toString(), "--output", output.toString()};
        String[] args = new String[base.length + extra.length];
        System.arraycopy(base, 0, args, 0, base.length);
        System.arraycopy(extra, 0, args, base.length, extra.length);
        PrintStream sink = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        assertEquals(Main.EXIT_OK, Main.run(args, sink, sink));
        return Files.readString(output, StandardCharsets.UTF_8);
    }

    @Test
    void queriesSectionContainsAllFiveResults() throws IOException {
        String report = runWith("--queries");

        assertTrue(report.contains("1. Термін придатності ≤ 30 дн.: 6" + NL), report);
        assertTrue(report.contains("2. Назви: Парацетамол, Амоксицилін,"), report);
        assertTrue(report.contains("3. Кількість за формою: капсули=1, краплі=1, мазь=1, розчин=2,"), report);
        assertTrue(report.contains("   за рецептом: капсули=1, розчин=2, таблетки=2" + NL), report);
        assertTrue(report.contains("4. Статистика цін: кількість 13, сума 1880.95, середня 144.69, мін 39.90, "
                + "макс 640.00 грн" + NL), report);
        assertTrue(report.contains("5. Топ-5 за найближчим терміном придатності:" + NL
                + "   1) Мелоксикам (розчин): 155.00 грн, 0 дн., за рецептом" + NL), report);
    }

    @Test
    void findReportsFoundAndMissing() throws IOException {
        assertTrue(runWith("--find", "назол").contains("Пошук «назол»: Назол (спрей): 60.00 грн, 14 дн., без рецепта"));
        assertTrue(runWith("--find", "Аспірин").contains("Пошук «Аспірин»: не знайдено"));
    }

    /* Без нових параметрів звіт не містить нових розділів. */
    @Test
    void plainReportHasNoQuerySections() throws IOException {
        String report = runWith();

        assertFalse(report.contains("Запити Stream API"), report);
        assertFalse(report.contains("Пошук"), report);
    }

    @Test
    void queriesSectionForEmptyInput() {
        String text = ReportFormatter.formatQueries(List.of(), 5);

        assertTrue(text.contains("1. Термін придатності ≤ 30 дн.: 0"), text);
        assertTrue(text.contains("3. Кількість за формою: немає даних"), text);
        assertTrue(text.contains("4. Статистика цін: немає даних"), text);
    }

    @Test
    void formatSearchUsesOptional() {
        assertEquals("Пошук «Х»: не знайдено" + NL, ReportFormatter.formatSearch(" Х ", Optional.empty()));
    }

    @Test
    void findWithoutValueIsUsageError() {
        PrintStream sink = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        assertEquals(Main.EXIT_USAGE, Main.run(new String[] {"--find"}, sink, sink));
    }
}
