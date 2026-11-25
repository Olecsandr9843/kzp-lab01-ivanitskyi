package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Перевіряє, що хибні рядки пропускаються з номером і причиною, а коректні зберігаються. */
class RecordProcessorTest {

    @Test
    void separatesValidRecordsFromErrors() {
        List<String> lines = List.of(
                "Парацетамол;таблетки;45.50;365;false",
                "Назол;спрей;хх.50;60;false",
                "",
                "Еналаприл;таблетки;73.60;45;true");

        ProcessingResult result = RecordProcessor.process(lines);

        assertEquals(2, result.validRecords().size());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Рядок 2: "), result.errors().get(0));
        assertEquals("Рядок 3: порожній рядок", result.errors().get(1));
    }

    /* Файл, збережений Блокнотом Windows, може починатися з BOM — його не можна вважати частиною назви. */
    @Test
    void stripsByteOrderMarkFromFirstLine() {
        ProcessingResult result = RecordProcessor.process(List.of("﻿Парацетамол;таблетки;45.50;365;false"));

        assertEquals("Парацетамол", result.validRecords().get(0)[MedicineLineParser.NAME]);
    }

    /* Зміна масиву, отриманого ззовні, не впливає на збережений результат. */
    @Test
    void resultIsProtectedFromExternalChanges() {
        ProcessingResult result = RecordProcessor.process(List.of("Парацетамол;таблетки;45.50;365;false"));

        result.validRecords().get(0)[MedicineLineParser.NAME] = "змінено";

        assertEquals("Парацетамол", result.validRecords().get(0)[MedicineLineParser.NAME]);
    }
}
