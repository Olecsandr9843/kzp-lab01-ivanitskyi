package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Перевіряє розбір і валідацію одного рядка CSV. */
class MedicineLineParserTest {

    /* Коректний рядок з українськими символами повертає п'ять полів без змін. */
    @Test
    void parsesValidLineWithUkrainianText() {
        String[] fields = MedicineLineParser.parse("Ібупрофен «Форте»;суспензія;96.00;90;false");

        assertArrayEquals(new String[] {"Ібупрофен «Форте»", "суспензія", "96.00", "90", "false"}, fields);
    }

    /* Пробіли навколо полів не заважають розбору, а TRUE нормалізується до true. */
    @Test
    void stripsSpacesAndNormalizesBoolean() {
        String[] fields = MedicineLineParser.parse("  Амоксицилін ; капсули ; 128.90 ; 180 ; TRUE ");

        assertEquals("Амоксицилін", fields[MedicineLineParser.NAME]);
        assertEquals("128.90", fields[MedicineLineParser.PRICE]);
        assertEquals("true", fields[MedicineLineParser.PRESCRIPTION]);
    }

    /* Нульові значення ціни й терміну допустимі (граничний випадок). */
    @Test
    void acceptsZeroPriceAndZeroDays() {
        String[] fields = MedicineLineParser.parse("Зразок;таблетки;0;0;false");

        assertEquals("0", fields[MedicineLineParser.PRICE]);
        assertEquals("0", fields[MedicineLineParser.DAYS_TO_EXPIRE]);
    }

    @Test
    void rejectsBlankLine() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MedicineLineParser.parse("   "));
        assertEquals("порожній рядок", error.getMessage());
    }

    @Test
    void rejectsWrongFieldCount() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MedicineLineParser.parse("Вітамін D3;краплі;210.00;730"));
        assertTrue(error.getMessage().contains("очікується 5 полів, отримано 4"), error.getMessage());
    }

    /* Порожнє останнє поле зберігається завдяки split(";", -1), тому помилка — саме порожнє поле. */
    @Test
    void reportsEmptyLastFieldInsteadOfWrongCount() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MedicineLineParser.parse("Назол;спрей;60.00;30;"));
        assertTrue(error.getMessage().contains("ознака рецептурності"), error.getMessage());
    }

    @Test
    void rejectsEmptyName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MedicineLineParser.parse(" ;таблетки;10.00;30;false"));
        assertTrue(error.getMessage().contains("назва препарату"), error.getMessage());
    }

    @Test
    void rejectsMalformedPrice() {
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parse("Назол;спрей;хх.50;60;false"));
        // Кома як десятковий роздільник і службові значення double не приймаються.
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parsePrice("12,50"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parsePrice("NaN"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parsePrice("Infinity"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parsePrice("5d"));
    }

    @Test
    void rejectsNegativePrice() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> MedicineLineParser.parse("Цефтріаксон;порошок;-310.00;400;true"));
        assertTrue(error.getMessage().contains("від'ємна ціна"), error.getMessage());
    }

    @Test
    void rejectsNegativeAndMalformedDays() {
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parseDaysToExpire("-1"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parseDaysToExpire("3.5"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parseDaysToExpire("99999999999"));
    }

    @Test
    void acceptsOnlyTrueOrFalseForPrescription() {
        assertTrue(MedicineLineParser.parsePrescription("true"));
        assertFalse(MedicineLineParser.parsePrescription("False"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parsePrescription("ні"));
        assertThrows(IllegalArgumentException.class, () -> MedicineLineParser.parsePrescription("1"));
    }
}
