package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Перевіряє чотири показники варіанта на відомих наборах даних.
 *
 * <p>Тести перенесено з ЛР1: ті самі рядки та очікувані значення; замість {@code List<String[]>}
 * тепер {@code List<Medicine>}, створений через {@link Medicine#fromCsv(String)}.
 */
class PharmacyStatisticsTest {
    private static final double DELTA = 1e-9;

    @Test
    void computesAllFourIndicators() {
        List<Medicine> medicines = List.of(
                Medicine.fromCsv("Парацетамол;таблетки;45.50;365;false"),
                Medicine.fromCsv("Амоксицилін;капсули;128.90;180;true"),
                Medicine.fromCsv("Ібупрофен;суспензія;96.00;90;false"),
                Medicine.fromCsv("Еналаприл;таблетки;73.60;45;true"));

        PharmacyStatistics statistics = new PharmacyStatistics(medicines);

        assertEquals(4, statistics.validCount());
        assertEquals(86.0, statistics.averagePrice(), DELTA);
        assertEquals(45, statistics.minDaysToExpire());
        assertEquals(2, statistics.prescriptionCount());
        assertFalse(statistics.isEmpty());
    }

    /* Набір із дробовим середнім ловить помилку цілочисельного ділення. */
    @Test
    void averageUsesFractionalDivision() {
        List<Medicine> medicines = List.of(
                Medicine.of("А", "таблетки", 1, 10, false),
                Medicine.of("Б", "таблетки", 2, 20, false));

        assertEquals(1.5, new PharmacyStatistics(medicines).averagePrice(), DELTA);
    }

    @Test
    void singleRecordIsBothMinimumAndAverage() {
        PharmacyStatistics statistics = new PharmacyStatistics(
                List.of(Medicine.fromCsv("Зразок;мазь;19.99;7;true")));

        assertEquals(19.99, statistics.averagePrice(), DELTA);
        assertEquals(7, statistics.minDaysToExpire());
        assertEquals(1, statistics.prescriptionCount());
    }

    @Test
    void emptyInputGivesZeroes() {
        PharmacyStatistics statistics = new PharmacyStatistics(List.of());

        assertTrue(statistics.isEmpty());
        assertEquals(0, statistics.validCount());
        assertEquals(0.0, statistics.averagePrice(), DELTA);
        assertEquals(0, statistics.prescriptionCount());
    }

    /* Показники 3 і 4 доступні разом як record. */
    @Test
    void exposesExpiryPrescriptionRecord() {
        PharmacyStatistics statistics = new PharmacyStatistics(List.of(
                Medicine.of("А", "таблетки", 10, 30, true),
                Medicine.of("Б", "сироп", 20, 12, false)));

        assertEquals(new ExpiryPrescription(12, 1), statistics.expiryPrescription());
    }
}
