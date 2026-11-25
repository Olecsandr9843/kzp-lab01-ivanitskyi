package ua.lpnu.kzp.pharmacy;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Розбирає та перевіряє один рядок CSV про лікарський препарат.
 *
 * <p>Формат рядка (варіант 11): {@code name;form;price;daysToExpire;prescription}, де
 * {@code name} і {@code form} — непорожній текст, {@code price} — невід'ємне число з крапкою,
 * {@code daysToExpire} — невід'ємне ціле, {@code prescription} — {@code true} або {@code false}.
 */
public final class MedicineLineParser {
    /** Розділювач полів у вхідному файлі. */
    public static final String SEPARATOR = ";";
    /** Очікувана кількість полів у записі. */
    public static final int FIELD_COUNT = 5;

    /** Індекс поля «назва препарату». */
    public static final int NAME = 0;
    /** Індекс поля «форма випуску». */
    public static final int FORM = 1;
    /** Індекс поля «ціна, грн». */
    public static final int PRICE = 2;
    /** Індекс поля «днів до закінчення терміну придатності». */
    public static final int DAYS_TO_EXPIRE = 3;
    /** Індекс поля «рецептурний препарат». */
    public static final int PRESCRIPTION = 4;

    /* Десяткове число лише з крапкою: відкидає "12,5", "1e3", "NaN", "Infinity", "5d". */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    /* Ціле число без знака плюс і без пробілів усередині. */
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    /* Забороняє створення екземплярів службового класу. */
    private MedicineLineParser() {
    }

    /**
     * Перевіряє рядок і повертає його поля без зайвих пробілів.
     *
     * @param line один рядок вхідного файла (без символу завершення рядка)
     * @return масив із {@value #FIELD_COUNT} перевірених полів
     * @throws IllegalArgumentException якщо рядок порожній, має неправильну кількість полів
     *                                  або містить хибне значення; повідомлення пояснює причину
     */
    public static String[] parse(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        // Ліміт -1 зберігає порожні поля в кінці: "a;b;1;2;" дасть 5 полів, а не 4.
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "очікується %d полів, отримано %d".formatted(FIELD_COUNT, fields.length));
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].strip();
        }

        requireText(fields[NAME], "назва препарату");
        requireText(fields[FORM], "форма випуску");
        parsePrice(fields[PRICE]);
        parseDaysToExpire(fields[DAYS_TO_EXPIRE]);
        parsePrescription(fields[PRESCRIPTION]);
        // Нормалізуємо логічне поле, щоб далі порівнювати лише з "true".
        fields[PRESCRIPTION] = fields[PRESCRIPTION].toLowerCase(Locale.ROOT);
        return fields;
    }

    /**
     * Перетворює текст ціни на число та перевіряє, що вона не від'ємна.
     *
     * @param text текст поля ціни
     * @return ціна в гривнях
     * @throws IllegalArgumentException якщо формат хибний або значення від'ємне
     */
    public static double parsePrice(String text) {
        requireText(text, "ціна");
        if (!DECIMAL.matcher(text).matches()) {
            throw new IllegalArgumentException("ціна має бути числом з крапкою, отримано \"%s\"".formatted(text));
        }
        double price = Double.parseDouble(text);
        if (price < 0) {
            throw new IllegalArgumentException("від'ємна ціна %s".formatted(text));
        }
        return price;
    }

    /**
     * Перетворює текст терміну придатності на ціле число днів.
     *
     * @param text текст поля daysToExpire
     * @return кількість днів до закінчення терміну
     * @throws IllegalArgumentException якщо формат хибний або значення від'ємне
     */
    public static int parseDaysToExpire(String text) {
        requireText(text, "термін придатності");
        if (!INTEGER.matcher(text).matches()) {
            throw new IllegalArgumentException(
                    "термін придатності має бути цілим числом, отримано \"%s\"".formatted(text));
        }
        int days;
        try {
            days = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            // Сюди потрапляють лише надто великі числа, бо формат уже перевірено.
            throw new IllegalArgumentException("термін придатності поза межами int: %s".formatted(text), exception);
        }
        if (days < 0) {
            throw new IllegalArgumentException("від'ємний термін придатності %s".formatted(text));
        }
        return days;
    }

    /**
     * Перетворює текст на логічне значення, приймаючи лише {@code true} або {@code false}.
     *
     * <p>{@link Boolean#parseBoolean(String)} не підходить: будь-який хибний текст він мовчки
     * перетворює на {@code false}.
     *
     * @param text текст поля prescription
     * @return {@code true}, якщо препарат рецептурний
     * @throws IllegalArgumentException якщо текст не є {@code true} або {@code false}
     */
    public static boolean parsePrescription(String text) {
        requireText(text, "ознака рецептурності");
        String normalized = text.toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new IllegalArgumentException(
                "ознака рецептурності має бути true або false, отримано \"%s\"".formatted(text));
    }

    /* Перевіряє, що обов'язкове текстове поле не порожнє. */
    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("порожнє поле «%s»".formatted(fieldName));
        }
    }
}
