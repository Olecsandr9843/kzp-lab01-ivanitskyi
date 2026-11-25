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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Перевіряє розширений розділ звіту {@code --details}, побудований на поліморфізмі. */
class DetailsReportTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    @Test
    void formatDetailsListsScoresUniqueCountAndAverage() {
        List<Medicine> medicines = List.of(
                Medicine.fromCsv("Парацетамол;таблетки;45.50;365;false"),
                Medicine.fromCsv("Амоксицилін;капсули;128.90;180;true"),
                Medicine.fromCsv("Парацетамол;таблетки;39.90;120;false"));

        String details = ReportFormatter.formatDetails(medicines);

        String expected = "Асортимент:" + NL
                + "  Парацетамол (таблетки): 45.50 грн, 365 дн., без рецепта — доступність 96/100" + NL
                + "  Амоксицилін (капсули): 128.90 грн, 180 дн., за рецептом — доступність 48/100" + NL
                + "  Парацетамол (таблетки): 39.90 грн, 120 дн., без рецепта — доступність 97/100" + NL
                + "Унікальних препаратів: 2" + NL
                + "Середня оцінка доступності: 80.33" + NL;
        assertEquals(expected, details);
    }

    @Test
    void formatDetailsHandlesEmptyList() {
        String details = ReportFormatter.formatDetails(List.of());

        assertTrue(details.contains("Унікальних препаратів: 0"), details);
        assertTrue(details.contains("Середня оцінка доступності: 0.00"), details);
    }

    /* Без --details звіт такий самий, як у попередніх роботах; з ним — доповнений у кінці. */
    @Test
    void detailsOptionAppendsSectionOnlyWhenRequested() throws IOException {
        Path input = tempDir.resolve("input.csv");
        Files.write(input, List.of("Парацетамол;таблетки;45.50;365;false"), StandardCharsets.UTF_8);
        Path plain = tempDir.resolve("plain.txt");
        Path detailed = tempDir.resolve("detailed.txt");
        PrintStream sink = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        Main.run(new String[] {"--input", input.toString(), "--output", plain.toString()}, sink, sink);
        Main.run(new String[] {"--input", input.toString(), "--output", detailed.toString(), "--details"}, sink, sink);

        String plainReport = Files.readString(plain, StandardCharsets.UTF_8);
        String detailedReport = Files.readString(detailed, StandardCharsets.UTF_8);
        assertFalse(plainReport.contains("Асортимент:"), plainReport);
        assertTrue(detailedReport.startsWith(plainReport), detailedReport);
        assertTrue(detailedReport.contains("доступність 96/100"), detailedReport);
    }
}
