package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Перевіряє клас-сутність {@link Medicine}: конструктор (інваріанти) і фабрику {@code fromCsv}.
 *
 * <p>Випадки з {@code MedicineLineParserTest} ЛР1 перенесено сюди: ті самі рядки й ті самі
 * повідомлення, змінився лише спосіб доступу до результату (геттери замість індексів масиву).
 */
class MedicineTest {
    private static final double DELTA = 1e-9;

    // ---------- Конструктор (через Medicine.of: абстрактний клас не створюється напряму) ----------

    @Test
    void constructorCreatesValidMedicine() {
        // Arrange + Act
        Medicine medicine = Medicine.of("Парацетамол", "таблетки", 45.5, 365, false);

        // Assert
        assertEquals("Парацетамол", medicine.getName());
        assertEquals("таблетки", medicine.getForm());
        assertEquals(45.5, medicine.getPrice(), DELTA);
        assertEquals(365, medicine.getDaysToExpire());
        assertFalse(medicine.isPrescription());
    }

    /* Межові значення 0 для ціни й терміну допустимі. */
    @Test
    void constructorAcceptsZeroPriceAndZeroDays() {
        Medicine medicine = Medicine.of("Зразок", "таблетки", 0.0, 0, true);

        assertEquals(0.0, medicine.getPrice(), DELTA);
        assertEquals(0, medicine.getDaysToExpire());
        assertTrue(medicine.isPrescription());
    }

    @Test
    void constructorRejectsNullText() {
        assertThrows(NullPointerException.class, () -> Medicine.of(null, "таблетки", 1.0, 1, false));
        assertThrows(NullPointerException.class, () -> Medicine.of("Назва", null, 1.0, 1, false));
    }

    @Test
    void constructorRejectsBlankText() {
        IllegalArgumentException name = assertThrows(IllegalArgumentException.class,
                () -> Medicine.of("  ", "таблетки", 1.0, 1, false));
        IllegalArgumentException form = assertThrows(IllegalArgumentException.class,
                () -> Medicine.of("Назва", "", 1.0, 1, false));

        assertEquals("порожнє поле «назва препарату»", name.getMessage());
        assertEquals("порожнє поле «форма випуску»", form.getMessage());
    }

    @Test
    void constructorRejectsNegativeValues() {
        IllegalArgumentException price = assertThrows(IllegalArgumentException.class,
                () -> Medicine.of("Назва", "таблетки", -0.01, 1, false));
        IllegalArgumentException days = assertThrows(IllegalArgumentException.class,
                () -> Medicine.of("Назва", "таблетки", 1.0, -1, false));

        assertEquals("від'ємна ціна -0.01", price.getMessage());
        assertEquals("від'ємний термін придатності -1", days.getMessage());
    }

    /* NaN не можна відкинути перевіркою price < 0, бо будь-яке порівняння з NaN дає false. */
    @Test
    void constructorRejectsNonFinitePrice() {
        assertThrows(IllegalArgumentException.class, () -> Medicine.of("Назва", "таблетки", Double.NaN, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> Medicine.of("Назва", "таблетки", Double.POSITIVE_INFINITY, 1, false));
    }

    @Test
    void toStringUsesDotRegardlessOfLocale() {
        Medicine medicine = Medicine.of("Амоксицилін", "капсули", 128.9, 180, true);

        assertEquals("Амоксицилін (капсули): 128.90 грн, 180 дн., за рецептом", medicine.toString());
    }

    // ---------- fromCsv (перенесено з MedicineLineParserTest ЛР1) ----------

    @Test
    void parsesValidLineWithUkrainianText() {
        Medicine medicine = Medicine.fromCsv("Ібупрофен «Форте»;суспензія;96.00;90;false");

        assertEquals("Ібупрофен «Форте»", medicine.getName());
        assertEquals("суспензія", medicine.getForm());
        assertEquals(96.0, medicine.getPrice(), DELTA);
        assertEquals(90, medicine.getDaysToExpire());
        assertFalse(medicine.isPrescription());
    }

    @Test
    void stripsSpacesAndNormalizesBoolean() {
        Medicine medicine = Medicine.fromCsv("  Амоксицилін ; капсули ; 128.90 ; 180 ; TRUE ");

        assertEquals("Амоксицилін", medicine.getName());
        assertEquals(128.9, medicine.getPrice(), DELTA);
        assertTrue(medicine.isPrescription());
    }

    @Test
    void acceptsZeroPriceAndZeroDays() {
        Medicine medicine = Medicine.fromCsv("Зразок;таблетки;0;0;false");

        assertEquals(0.0, medicine.getPrice(), DELTA);
        assertEquals(0, medicine.getDaysToExpire());
    }

    @Test
    void rejectsNullAndBlankLine() {
        assertThrows(NullPointerException.class, () -> Medicine.fromCsv(null));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Medicine.fromCsv("   "));
        assertEquals("порожній рядок", error.getMessage());
    }

    @Test
    void rejectsWrongFieldCount() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Вітамін D3;краплі;210.00;730"));
        assertEquals("очікується 5 полів, отримано 4", error.getMessage());
    }

    /* Порожнє останнє поле зберігається завдяки split(";", -1), тому помилка — саме порожнє поле. */
    @Test
    void reportsEmptyLastFieldInsteadOfWrongCount() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Назол;спрей;60.00;30;"));
        assertTrue(error.getMessage().contains("ознака рецептурності"), error.getMessage());
    }

    /* Порушення інваріанта, виявлене конструктором, проходить крізь fromCsv без змін. */
    @Test
    void rejectsEmptyName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv(" ;таблетки;10.00;30;false"));
        assertEquals("порожнє поле «назва препарату»", error.getMessage());
    }

    @Test
    void rejectsMalformedPrice() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Назол;спрей;хх.50;60;false"));
        assertEquals("ціна має бути числом з крапкою, отримано \"хх.50\"", error.getMessage());
        // Кома як десятковий роздільник і службові значення double не приймаються.
        for (String price : new String[] {"12,50", "NaN", "Infinity", "5d", "1e3"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> Medicine.fromCsv("Назол;спрей;" + price + ";60;false"), price);
        }
    }

    @Test
    void rejectsEmptyPrice() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Сироп від кашлю;сироп;;120;false"));
        assertEquals("порожнє поле «ціна»", error.getMessage());
    }

    @Test
    void rejectsNegativePrice() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Цефтріаксон;порошок;-310.00;400;true"));
        assertEquals("від'ємна ціна -310.00", error.getMessage());
    }

    @Test
    void rejectsNegativeAndMalformedDays() {
        assertThrows(IllegalArgumentException.class, () -> Medicine.fromCsv("А;таблетки;1.00;-1;false"));
        assertThrows(IllegalArgumentException.class, () -> Medicine.fromCsv("А;таблетки;1.00;3.5;false"));
        assertThrows(IllegalArgumentException.class, () -> Medicine.fromCsv("А;таблетки;1.00;99999999999;false"));
    }

    @Test
    void acceptsOnlyTrueOrFalseForPrescription() {
        assertTrue(Medicine.fromCsv("А;таблетки;1.00;1;true").isPrescription());
        assertFalse(Medicine.fromCsv("А;таблетки;1.00;1;False").isPrescription());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("А;таблетки;1.00;1;ні"));
        assertEquals("ознака рецептурності має бути true або false, отримано \"ні\"", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Medicine.fromCsv("А;таблетки;1.00;1;1"));
    }
}
