package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Перевіряє чотири показники варіанта на відомих наборах даних. */
class PharmacyStatisticsTest {
    private static final double DELTA = 1e-9;

    /* Набір підібрано так, щоб середнє не було цілим: це ловить помилку цілочисельного ділення. */
    @Test
    void computesAllFourIndicators() {
        List<String[]> records = List.of(
                MedicineLineParser.parse("Парацетамол;таблетки;45.50;365;false"),
                MedicineLineParser.parse("Амоксицилін;капсули;128.90;180;true"),
                MedicineLineParser.parse("Ібупрофен;суспензія;96.00;90;false"),
                MedicineLineParser.parse("Еналаприл;таблетки;73.60;45;true"));

        PharmacyStatistics statistics = new PharmacyStatistics(records);

        assertEquals(4, statistics.validCount());
        assertEquals(86.0, statistics.averagePrice(), DELTA);
        assertEquals(45, statistics.minDaysToExpire());
        assertEquals(2, statistics.prescriptionCount());
        assertFalse(statistics.isEmpty());
    }

    @Test
    void averageUsesFractionalDivision() {
        List<String[]> records = List.of(
                MedicineLineParser.parse("А;таблетки;1;10;false"),
                MedicineLineParser.parse("Б;таблетки;2;20;false"));

        assertEquals(1.5, new PharmacyStatistics(records).averagePrice(), DELTA);
    }

    @Test
    void singleRecordIsBothMinimumAndAverage() {
        PharmacyStatistics statistics = new PharmacyStatistics(
                List.<String[]>of(MedicineLineParser.parse("Зразок;мазь;19.99;7;true")));

        assertEquals(19.99, statistics.averagePrice(), DELTA);
        assertEquals(7, statistics.minDaysToExpire());
        assertEquals(1, statistics.prescriptionCount());
    }

    /* Порожній набір не дає NaN і не кидає винятків. */
    @Test
    void emptyInputGivesZeroes() {
        PharmacyStatistics statistics = new PharmacyStatistics(List.of());

        assertTrue(statistics.isEmpty());
        assertEquals(0, statistics.validCount());
        assertEquals(0.0, statistics.averagePrice(), DELTA);
        assertEquals(0, statistics.prescriptionCount());
    }
}
