package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Перевіряє, що хибні рядки пропускаються з номером і причиною, а коректні стають об'єктами. */
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
        assertEquals("Еналаприл", result.validRecords().get(1).getName());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Рядок 2: "), result.errors().get(0));
        assertEquals("Рядок 3: порожній рядок", result.errors().get(1));
    }

    /* Хибний запис не потрапляє до списку, тому не впливає на статистику. */
    @Test
    void invalidRecordDoesNotChangeStatistics() {
        List<String> valid = List.of("А;таблетки;10.00;30;true", "Б;сироп;20.00;12;false");
        List<String> withInvalid = List.of("А;таблетки;10.00;30;true", "В;мазь;-5.00;1;true",
                "Б;сироп;20.00;12;false");

        PharmacyStatistics expected = new PharmacyStatistics(RecordProcessor.process(valid).validRecords());
        PharmacyStatistics actual = new PharmacyStatistics(RecordProcessor.process(withInvalid).validRecords());

        assertEquals(expected.validCount(), actual.validCount());
        assertEquals(expected.averagePrice(), actual.averagePrice(), 1e-9);
        assertEquals(expected.expiryPrescription(), actual.expiryPrescription());
    }

    /* Файл, збережений Блокнотом Windows, може починатися з BOM — його не можна вважати частиною назви. */
    @Test
    void stripsByteOrderMarkFromFirstLine() {
        ProcessingResult result = RecordProcessor.process(List.of("﻿Парацетамол;таблетки;45.50;365;false"));

        assertEquals("Парацетамол", result.validRecords().get(0).getName());
    }

    /* Список у результаті незмінний: його не можна доповнити ззовні. */
    @Test
    void resultIsProtectedFromExternalChanges() {
        ProcessingResult result = RecordProcessor.process(List.of("Парацетамол;таблетки;45.50;365;false"));

        assertThrows(UnsupportedOperationException.class,
                () -> result.validRecords().add(Medicine.of("Х", "Х", 1, 1, false)));
    }
}
