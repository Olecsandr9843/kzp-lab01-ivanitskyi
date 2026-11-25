package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Перевіряє, що Maven справді підставив значення у version.properties. */
class BuildInfoTest {

    /* Формат «версія+збірка», наприклад 1.0.0+local або 1.0.0+42. */
    @Test
    void versionContainsProjectVersionAndBuildNumber() {
        String version = BuildInfo.version();

        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+\\+\\w+"), "Неочікуваний формат версії: %s".formatted(version));
        assertFalse(version.contains("${"), "Maven не підставив значення: %s".formatted(version));
    }
}
