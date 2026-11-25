package ua.lpnu.kzp.pharmacy;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Лікарський препарат в асортименті аптеки (варіант 11).
 *
 * <p>Об'єкт незмінний: усі поля {@code private final}, сеттерів немає. Будь-який створений
 * екземпляр гарантовано коректний, бо всі інваріанти перевіряє конструктор:
 * <ul>
 *     <li>назва та форма випуску не {@code null} і не порожні;</li>
 *     <li>ціна скінченна та не від'ємна;</li>
 *     <li>термін до закінчення придатності не від'ємний.</li>
 * </ul>
 *
 * <p>Формат рядка CSV: {@code name;form;price;daysToExpire;prescription}.
 */
public final class Medicine {
    /** Розділювач полів у рядку CSV. */
    public static final String SEPARATOR = ";";
    /** Кількість полів у рядку CSV. */
    public static final int FIELD_COUNT = 5;

    /* Десяткове число лише з крапкою: відкидає "12,5", "1e3", "NaN", "Infinity", "5d". */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    /* Ціле число без знака плюс і пробілів усередині. */
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final String name;
    private final String form;
    private final double price;
    private final int daysToExpire;
    private final boolean prescription;

    /**
     * Створює препарат і перевіряє всі інваріанти.
     *
     * @param name         назва препарату, непорожня
     * @param form         форма випуску (таблетки, сироп…), непорожня
     * @param price        ціна в гривнях, скінченна та не від'ємна
     * @param daysToExpire кількість днів до закінчення терміну придатності, не від'ємна
     * @param prescription {@code true}, якщо препарат відпускається за рецептом
     * @throws NullPointerException     якщо {@code name} або {@code form} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено будь-який інший інваріант
     */
    public Medicine(String name, String form, double price, int daysToExpire, boolean prescription) {
        this.name = requireText(name, "назва препарату");
        this.form = requireText(form, "форма випуску");
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна має бути скінченним числом");
        }
        if (price < 0) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "від'ємна ціна %.2f", price));
        }
        if (daysToExpire < 0) {
            throw new IllegalArgumentException("від'ємний термін придатності %d".formatted(daysToExpire));
        }
        this.price = price;
        this.daysToExpire = daysToExpire;
        this.prescription = prescription;
    }

    /**
     * Створює препарат з одного рядка CSV.
     *
     * <p>Метод відповідає лише за структуру рядка та перетворення тексту на типи
     * (кількість полів, формат чисел і логічного значення). Предметні правила — непорожні
     * назви, невід'ємні значення — перевіряє конструктор, тому вони не дублюються.
     *
     * @param line рядок без символу завершення, наприклад {@code Парацетамол;таблетки;45.50;365;false}
     * @return коректний препарат
     * @throws NullPointerException     якщо {@code line} дорівнює {@code null}
     * @throws IllegalArgumentException якщо рядок порожній, має не {@value #FIELD_COUNT} полів,
     *                                  містить хибне значення або порушує інваріант
     */
    public static Medicine fromCsv(String line) {
        Objects.requireNonNull(line, "рядок не може бути null");
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        // Ліміт -1 зберігає порожні поля в кінці: "a;b;1;2;" дасть 5 полів, а не 4.
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "очікується %d полів, отримано %d".formatted(FIELD_COUNT, fields.length));
        }
        return new Medicine(
                fields[0].strip(),
                fields[1].strip(),
                parsePrice(fields[2].strip()),
                parseDaysToExpire(fields[3].strip()),
                parsePrescription(fields[4].strip()));
    }

    /**
     * Повертає назву препарату.
     *
     * @return непорожня назва
     */
    public String getName() {
        return name;
    }

    /**
     * Повертає форму випуску.
     *
     * @return непорожня форма випуску
     */
    public String getForm() {
        return form;
    }

    /**
     * Повертає ціну.
     *
     * @return ціна в гривнях, не від'ємна
     */
    public double getPrice() {
        return price;
    }

    /**
     * Повертає кількість днів до закінчення терміну придатності.
     *
     * @return кількість днів, не від'ємна
     */
    public int getDaysToExpire() {
        return daysToExpire;
    }

    /**
     * Повідомляє, чи препарат рецептурний.
     *
     * @return {@code true}, якщо потрібен рецепт
     */
    public boolean isPrescription() {
        return prescription;
    }

    /**
     * Повертає зрозуміле текстове подання для діагностики й журналів (не формат CSV).
     *
     * @return наприклад {@code Парацетамол (таблетки): 45.50 грн, 365 дн., без рецепта}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s): %.2f грн, %d дн., %s",
                name, form, price, daysToExpire, prescription ? "за рецептом" : "без рецепта");
    }

    /* Перевіряє обов'язковий текст: null — помилка програміста, порожній рядок — хибні дані. */
    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, "поле «%s» не може бути null".formatted(fieldName));
        if (value.isBlank()) {
            throw new IllegalArgumentException("порожнє поле «%s»".formatted(fieldName));
        }
        return value;
    }

    /* Перевіряє лише формат ціни; знак перевіряє конструктор. */
    private static double parsePrice(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("порожнє поле «ціна»");
        }
        if (!DECIMAL.matcher(text).matches()) {
            throw new IllegalArgumentException("ціна має бути числом з крапкою, отримано \"%s\"".formatted(text));
        }
        return Double.parseDouble(text);
    }

    /* Перевіряє лише формат терміну; знак перевіряє конструктор. */
    private static int parseDaysToExpire(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("порожнє поле «термін придатності»");
        }
        if (!INTEGER.matcher(text).matches()) {
            throw new IllegalArgumentException(
                    "термін придатності має бути цілим числом, отримано \"%s\"".formatted(text));
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            // Сюди потрапляють лише надто великі числа, бо формат уже перевірено.
            throw new IllegalArgumentException("термін придатності поза межами int: %s".formatted(text), exception);
        }
    }

    /* Приймає лише true/false: Boolean.parseBoolean("ні") мовчки повернув би false. */
    private static boolean parsePrescription(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("порожнє поле «ознака рецептурності»");
        }
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
}
