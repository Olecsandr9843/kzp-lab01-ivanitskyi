package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Доводить, що перебудова не змінила зовнішню поведінку: звіт для {@code data/input.csv}
 * збігається з контрольним виводом ЛР1 ({@code lab01-baseline-report.txt}), отриманим
 * програмою версії 1.0.0 до рефакторингу.
 */
class BaselineCompatibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void reportForSampleInputMatchesLab01Baseline() throws IOException {
        // Arrange: Maven запускає тести з кореня проєкту, тому відносний шлях працює на всіх ОС.
        Path input = Path.of("data", "input.csv");
        Path output = tempDir.resolve("report.txt");
        String expected;
        try (InputStream stream = getClass().getResourceAsStream("/lab01-baseline-report.txt")) {
            expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        PrintStream sink = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        // Act
        int code = Main.run(new String[] {"--input", input.toString(), "--output", output.toString()}, sink, sink);

        // Assert: вирівнюємо лише платформні відмінності — роздільник рядків і роздільник у шляху.
        assertEquals(Main.EXIT_OK, code);
        String actual = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals(normalize(expected), normalize(actual));
    }

    /* Приводить текст до вигляду Unix: \r\n → \n, data\input.csv → data/input.csv. */
    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replace('\\', '/');
    }
}
