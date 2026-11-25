package ua.lpnu.kzp.pharmacy;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import ua.lpnu.kzp.storage.CsvColumn;

/**
 * Запис про препарат для збереження у файл (варіант 11, ЛР5).
 *
 * <p>На відміну від {@link Medicine}, де термін придатності — кількість днів відносно
 * поточного звіту, тут зберігається абсолютна дата {@code expiresOn}: файл, прочитаний
 * завтра, має означати те саме, що й сьогодні. Перетворення виконують {@link #from} і
 * {@link #toMedicine} відносно опорної дати.
 *
 * <p>Ключ запису (для демонстрації відновлення) — {@code name + form}, див. {@link #key()}.
 *
 * <p>Record сам дає {@code equals}/{@code hashCode} за всіма п'ятьма компонентами, тож
 * round-trip перевіряється звичайним {@code equals} списків.
 *
 * @param name         назва препарату; CSV-стовпець «назва»
 * @param form         форма випуску; «форма»
 * @param price        ціна в гривнях; «ціна»
 * @param expiresOn    дата закінчення терміну придатності; «придатність»
 * @param prescription категорія відпуску; «рецепт» (ім'я константи {@link MedicineKind})
 */
public record MedicineRecord(
        @CsvColumn(MedicineRecord.NAME) String name,
        @CsvColumn(MedicineRecord.FORM) String form,
        @CsvColumn(MedicineRecord.PRICE) double price,
        @CsvColumn(MedicineRecord.EXPIRES_ON) LocalDate expiresOn,
        @CsvColumn(MedicineRecord.PRESCRIPTION) MedicineKind prescription) {

    /** Назва CSV-стовпця для {@code name}. */
    public static final String NAME = "назва";
    /** Назва CSV-стовпця для {@code form}. */
    public static final String FORM = "форма";
    /** Назва CSV-стовпця для {@code price}. */
    public static final String PRICE = "ціна";
    /** Назва CSV-стовпця для {@code expiresOn}. */
    public static final String EXPIRES_ON = "придатність";
    /** Назва CSV-стовпця для {@code prescription}. */
    public static final String PRESCRIPTION = "рецепт";

    /**
     * Перевіряє інваріанти запису.
     *
     * @throws NullPointerException     якщо будь-який об'єктний компонент дорівнює {@code null}
     * @throws IllegalArgumentException якщо назва чи форма порожні або ціна не скінченна чи від'ємна
     */
    public MedicineRecord {
        Objects.requireNonNull(name, "назва не може бути null");
        Objects.requireNonNull(form, "форма не може бути null");
        Objects.requireNonNull(expiresOn, "дата придатності не може бути null");
        Objects.requireNonNull(prescription, "категорія не може бути null");
        if (name.isBlank() || form.isBlank()) {
            throw new IllegalArgumentException("назва й форма не можуть бути порожніми");
        }
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "ціна має бути скінченною й невід'ємною: %s",
                    price));
        }
    }

    /**
     * Створює запис з одного рядка CSV, поданого як карта «назва стовпця → текст».
     * Використовується як фабрика для {@link ua.lpnu.kzp.storage.CsvImporter}.
     *
     * @param row значення стовпців {@value #NAME}, {@value #FORM}, {@value #PRICE},
     *            {@value #EXPIRES_ON}, {@value #PRESCRIPTION}
     * @return коректний запис
     * @throws IllegalArgumentException якщо ціна не число, дата не у форматі РРРР-ММ-ДД,
     *                                  категорія невідома або порушено інваріант
     */
    public static MedicineRecord fromCsv(Map<String, String> row) {
        String priceText = required(row, PRICE).strip();
        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ціна має бути числом, отримано \"%s\"".formatted(priceText), exception);
        }
        String dateText = required(row, EXPIRES_ON).strip();
        LocalDate expiresOn;
        try {
            expiresOn = LocalDate.parse(dateText);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "дата придатності має формат РРРР-ММ-ДД, отримано \"%s\"".formatted(dateText), exception);
        }
        String kindText = required(row, PRESCRIPTION).strip();
        MedicineKind kind;
        try {
            kind = MedicineKind.valueOf(kindText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "рецепт має бути PRESCRIPTION або FREE, отримано \"%s\"".formatted(kindText), exception);
        }
        return new MedicineRecord(required(row, NAME), required(row, FORM), price, expiresOn, kind);
    }

    /**
     * Перетворює препарат моделі на запис для збереження.
     *
     * @param medicine препарат будь-якого підтипу
     * @param today    опорна дата, від якої відлічується {@code daysToExpire}
     * @return запис з абсолютною датою придатності
     */
    public static MedicineRecord from(Medicine medicine, LocalDate today) {
        return new MedicineRecord(medicine.getName(), medicine.getForm(), medicine.getPrice(),
                today.plusDays(medicine.getDaysToExpire()), medicine.getKind());
    }

    /**
     * Відновлює препарат моделі відносно опорної дати.
     *
     * @param today опорна дата
     * @return {@link PrescriptionMedicine} або {@link FreeMedicine}
     * @throws IllegalArgumentException якщо на опорну дату термін придатності вже минув
     */
    public Medicine toMedicine(LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, expiresOn);
        if (days > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("термін придатності завеликий: " + expiresOn);
        }
        return Medicine.of(name, form, price, (int) days, prescription.requiresPrescription());
    }

    /**
     * Повертає ключ запису: назва й форма — так само, як у логічній рівності {@link Medicine}.
     *
     * @return наприклад {@code Парацетамол / таблетки}
     */
    public String key() {
        return name + " / " + form;
    }

    /* Значення стовпця як є: текст не обрізаємо, щоб round-trip зберіг його точно. */
    private static String required(Map<String, String> row, String column) {
        String value = row.get(column);
        if (value == null) {
            throw new IllegalArgumentException("немає стовпця «%s»".formatted(column));
        }
        return value;
    }
}
