package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Перевіряє enum {@link MedicineKind}: склад, підписи, поле рецептурності та перетворення з CSV. */
class MedicineKindTest {

    @Test
    void containsExactlyTwoKinds() {
        assertArrayEquals(new MedicineKind[] {MedicineKind.PRESCRIPTION, MedicineKind.FREE}, MedicineKind.values());
    }

    @Test
    void labelsMatchReportWording() {
        assertEquals("за рецептом", MedicineKind.PRESCRIPTION.label());
        assertEquals("без рецепта", MedicineKind.FREE.label());
    }

    @Test
    void requiresPrescriptionField() {
        assertTrue(MedicineKind.PRESCRIPTION.requiresPrescription());
        assertFalse(MedicineKind.FREE.requiresPrescription());
    }

    /* Константи enum — єдині екземпляри, тому їх можна порівнювати через ==. */
    @Test
    void fromPrescriptionFlagReturnsSingletonConstant() {
        assertSame(MedicineKind.PRESCRIPTION, MedicineKind.fromPrescriptionFlag(true));
        assertSame(MedicineKind.FREE, MedicineKind.fromPrescriptionFlag(false));
        assertSame(MedicineKind.FREE, MedicineKind.valueOf("FREE"));
    }

    @Test
    void valueOfRejectsUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> MedicineKind.valueOf("OTC"));
    }
}
