package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Перевіряє кожен підтип окремо: категорію, інваріанти базового типу та оцінку доступності. */
class SubtypesTest {

    // ---------- PrescriptionMedicine ----------

    @Test
    void prescriptionMedicineHasPrescriptionKind() {
        PrescriptionMedicine medicine = new PrescriptionMedicine("Амоксицилін", "капсули", 128.90, 180);

        assertEquals(MedicineKind.PRESCRIPTION, medicine.getKind());
        assertTrue(medicine.isPrescription());
    }

    /* 60 − ⌊128.90 / 10⌋ = 60 − 12 = 48. */
    @Test
    void prescriptionScoreStartsAtSixtyMinusPricePenalty() {
        assertEquals(48, new PrescriptionMedicine("Амоксицилін", "капсули", 128.90, 180).availabilityScore());
        assertEquals(60, new PrescriptionMedicine("Дешевий", "таблетки", 9.99, 10).availabilityScore());
    }

    /* Дорогий рецептурний препарат не опускається нижче 10, поки придатний. */
    @Test
    void prescriptionScoreHasLowerBound() {
        assertEquals(PrescriptionMedicine.MIN_SCORE,
                new PrescriptionMedicine("Дорогий", "ампули", 5000.0, 100).availabilityScore());
    }

    @Test
    void prescriptionScoreIsZeroWhenExpiresToday() {
        assertEquals(0, new PrescriptionMedicine("Еналаприл", "таблетки", 73.60, 0).availabilityScore());
    }

    /* Спільні інваріанти перевіряє базовий тип — підтип їх не дублює, але й не обходить. */
    @Test
    void prescriptionMedicineInheritsBaseInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new PrescriptionMedicine(" ", "капсули", 1.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new PrescriptionMedicine("А", "капсули", -1.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new PrescriptionMedicine("А", "капсули", 1.0, -1));
        assertThrows(NullPointerException.class, () -> new PrescriptionMedicine("А", null, 1.0, 1));
    }

    // ---------- FreeMedicine ----------

    @Test
    void freeMedicineHasFreeKind() {
        FreeMedicine medicine = new FreeMedicine("Парацетамол", "таблетки", 45.50, 365);

        assertEquals(MedicineKind.FREE, medicine.getKind());
        assertFalse(medicine.isPrescription());
    }

    /* 100 − ⌊45.50 / 10⌋ = 100 − 4 = 96. */
    @Test
    void freeScoreStartsAtHundredMinusPricePenalty() {
        assertEquals(96, new FreeMedicine("Парацетамол", "таблетки", 45.50, 365).availabilityScore());
        assertEquals(100, new FreeMedicine("Безкоштовний", "таблетки", 0.0, 365).availabilityScore());
    }

    /* Межа 30 днів: 29 — короткий термін (−20), 30 — уже ні. */
    @Test
    void freeScoreDropsForShortExpiry() {
        assertEquals(76, new FreeMedicine("А", "сироп", 45.50, 29).availabilityScore());
        assertEquals(96, new FreeMedicine("А", "сироп", 45.50, 30).availabilityScore());
    }

    /* Штраф за ціну обмежений 50 балами. */
    @Test
    void freeScorePricePenaltyIsCapped() {
        assertEquals(50, new FreeMedicine("Дорогий", "мазь", 10_000.0, 365).availabilityScore());
    }

    @Test
    void freeScoreIsZeroWhenExpiresToday() {
        assertEquals(0, new FreeMedicine("Назол", "спрей", 60.0, 0).availabilityScore());
    }

    @Test
    void freeMedicineInheritsBaseInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new FreeMedicine("А", "", 1.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new FreeMedicine("А", "сироп", Double.NaN, 1));
    }

    // ---------- Порівняння підтипів ----------

    /* Однакові дані — різна поведінка: саме це доводить, що реалізації справді відрізняються. */
    @Test
    void subtypesScoreSameDataDifferently() {
        Medicine prescription = new PrescriptionMedicine("Ліки", "таблетки", 100.0, 200);
        Medicine free = new FreeMedicine("Ліки", "таблетки", 100.0, 200);

        assertEquals(50, prescription.availabilityScore());
        assertEquals(90, free.availabilityScore());
        assertNotEquals(prescription.availabilityScore(), free.availabilityScore());
    }

    @Test
    void toStringUsesKindLabel() {
        assertEquals("Ліки (таблетки): 100.00 грн, 200 дн., за рецептом",
                new PrescriptionMedicine("Ліки", "таблетки", 100.0, 200).toString());
        assertEquals("Ліки (таблетки): 100.00 грн, 200 дн., без рецепта",
                new FreeMedicine("Ліки", "таблетки", 100.0, 200).toString());
    }
}
