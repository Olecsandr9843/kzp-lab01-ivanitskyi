package ua.lpnu.kzp.pharmacy;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Лікарський препарат — спільний базовий тип для всіх категорій (варіант 11).
 *
 * <p>Клас абстрактний, бо «препарат узагалі» не існує: кожен рядок CSV стає або
 * {@link PrescriptionMedicine}, або {@link FreeMedicine}. Ієрархія {@code sealed}: компілятор
 * знає повний список підтипів і не дозволить додати інший без зміни {@code permits}.
 *
 * <p>Спільні поля та інваріанти зосереджені тут і не дублюються в підтипах:
 * <ul>
 *     <li>назва та форма випуску не {@code null} і не порожні;</li>
 *     <li>ціна скінченна та не від'ємна;</li>
 *     <li>термін до закінчення придатності не від'ємний;</li>
 *     <li>категорія {@link MedicineKind} не {@code null}.</li>
 * </ul>
 *
 * <p>Підтипи відрізняються поведінкою — реалізацією {@link #availabilityScore()}.
 *
 * <p>Формат рядка CSV: {@code name;form;price;daysToExpire;prescription}.
 */
public abstract sealed class Medicine permits PrescriptionMedicine, FreeMedicine {
    /** Розділювач полів у рядку CSV. */
    public static final String SEPARATOR = ";";
    /** Кількість полів у рядку CSV. */
    public static final int FIELD_COUNT = 5;
    /** Найвища можлива оцінка доступності. */
    public static final int MAX_SCORE = 100;

    /* Десяткове число лише з крапкою: відкидає "12,5", "1e3", "NaN", "Infinity", "5d". */
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    /* Ціле число без знака плюс і пробілів усередині. */
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    /* Кожні повні 10 грн ціни знижують доступність на 1 бал, але не більше ніж на 50. */
    private static final double PRICE_STEP = 10.0;
    private static final int MAX_PRICE_PENALTY = 50;

    private final String name;
    private final String form;
    private final double price;
    private final int daysToExpire;
    private final MedicineKind kind;

    /**
     * Створює препарат і перевіряє всі спільні інваріанти. Викликається з конструкторів
     * підтипів через {@code super(...)}.
     *
     * <p>Перевірки виконуються в аргументах виклику {@code this(...)}, тобто до створення
     * об'єкта: так недобудований екземпляр підкласу ніколи не з'являється (SpotBugs
     * CT_CONSTRUCTOR_THROW).
     *
     * @param name         назва препарату, непорожня
     * @param form         форма випуску, непорожня
     * @param price        ціна в гривнях, скінченна та не від'ємна
     * @param daysToExpire днів до закінчення терміну придатності, не від'ємне
     * @param kind         категорія відпуску
     * @throws NullPointerException     якщо {@code name}, {@code form} або {@code kind} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено будь-який інший інваріант
     */
    protected Medicine(String name, String form, double price, int daysToExpire, MedicineKind kind) {
        this(requireText(name, "назва препарату"), requireText(form, "форма випуску"),
                requirePrice(price), requireDays(daysToExpire),
                Objects.requireNonNull(kind, "категорія не може бути null"), true);
    }

    /* Присвоює вже перевірений стан. Параметр validated лише відрізняє сигнатуру. */
    private Medicine(String name, String form, double price, int daysToExpire, MedicineKind kind,
                     boolean validated) {
        this.name = name;
        this.form = form;
        this.price = price;
        this.daysToExpire = daysToExpire;
        this.kind = kind;
    }

    /**
     * Створює препарат потрібного підтипу за категорією, заданою ознакою рецептурності.
     *
     * <p>Це єдине місце, де вибирається конкретний клас; уся подальша обробка працює
     * через базовий тип {@code Medicine}.
     *
     * @param name         назва препарату
     * @param form         форма випуску
     * @param price        ціна в гривнях
     * @param daysToExpire днів до закінчення терміну придатності
     * @param prescription {@code true} — {@link PrescriptionMedicine}, {@code false} — {@link FreeMedicine}
     * @return коректний препарат
     * @throws NullPointerException     якщо текстове поле дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено інваріант
     */
    public static Medicine of(String name, String form, double price, int daysToExpire, boolean prescription) {
        // switch-вираз за enum вичерпний: додавання нової категорії без нової гілки не скомпілюється.
        return switch (MedicineKind.fromPrescriptionFlag(prescription)) {
            case PRESCRIPTION -> new PrescriptionMedicine(name, form, price, daysToExpire);
            case FREE -> new FreeMedicine(name, form, price, daysToExpire);
        };
    }

    /**
     * Створює препарат з одного рядка CSV.
     *
     * <p>Метод відповідає лише за структуру рядка та перетворення тексту на типи. Предметні
     * правила перевіряє конструктор, тому вони не дублюються.
     *
     * @param line рядок без символу завершення, наприклад {@code Парацетамол;таблетки;45.50;365;false}
     * @return препарат підтипу, що відповідає полю {@code prescription}
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
        String name = fields[0].strip();
        String form = fields[1].strip();
        double price = parsePrice(fields[2].strip());
        int days = parseDaysToExpire(fields[3].strip());
        boolean prescription = parsePrescription(fields[4].strip());
        return of(name, form, price, days, prescription);
    }

    /**
     * Оцінює доступність препарату для покупця за шкалою від 0 до {@value #MAX_SCORE}.
     *
     * <p>Поліморфна операція варіанта: кожен підтип рахує її по-своєму, а код звітності
     * викликає її через базовий тип і не знає, з яким підтипом працює.
     *
     * @return оцінка від 0 (недоступний) до {@value #MAX_SCORE} (доступний без обмежень)
     */
    public abstract int availabilityScore();

    /**
     * Повертає назву препарату.
     *
     * @return непорожня назва
     */
    public final String getName() {
        return name;
    }

    /**
     * Повертає форму випуску.
     *
     * @return непорожня форма випуску
     */
    public final String getForm() {
        return form;
    }

    /**
     * Повертає ціну.
     *
     * @return ціна в гривнях, не від'ємна
     */
    public final double getPrice() {
        return price;
    }

    /**
     * Повертає кількість днів до закінчення терміну придатності.
     *
     * @return кількість днів, не від'ємна
     */
    public final int getDaysToExpire() {
        return daysToExpire;
    }

    /**
     * Повертає категорію відпуску.
     *
     * @return категорія, не {@code null}
     */
    public final MedicineKind getKind() {
        return kind;
    }

    /**
     * Повідомляє, чи препарат рецептурний. Відповідь бере з категорії, а не з перевірки класу.
     *
     * @return {@code true}, якщо потрібен рецепт
     */
    public final boolean isPrescription() {
        return kind.requiresPrescription();
    }

    /**
     * Повертає штраф до доступності за ціну: 1 бал за кожні повні 10 грн, не більше 50.
     * Спільна частина розрахунку для всіх підтипів.
     *
     * @return штраф від 0 до 50
     */
    protected final int pricePenalty() {
        return (int) Math.min(MAX_PRICE_PENALTY, Math.floor(price / PRICE_STEP));
    }

    /**
     * Повідомляє, чи термін придатності спливає сьогодні (такий препарат не продається).
     *
     * @return {@code true}, якщо {@code daysToExpire == 0}
     */
    protected final boolean expiresToday() {
        return daysToExpire == 0;
    }

    /**
     * Порівнює препарати логічно: рівні препарати мають однаковий підтип, назву та форму
     * випуску. Ціна й термін не враховуються — це властивості партії, а не препарату.
     *
     * <p>Метод {@code final}, щоб підтипи не порушили симетричність рівності.
     *
     * @param other об'єкт для порівняння
     * @return {@code true}, якщо {@code other} — той самий препарат
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // getClass() замість instanceof: рецептурний і безрецептурний препарати з однаковою
        // назвою — різні об'єкти, і рівність лишається симетричною.
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Medicine that = (Medicine) other;
        return name.equals(that.name) && form.equals(that.form);
    }

    /**
     * Повертає хеш-код, узгоджений з {@link #equals(Object)}: використовує ті самі поля.
     *
     * @return хеш-код
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), name, form);
    }

    /**
     * Повертає зрозуміле текстове подання для діагностики й журналів (не формат CSV).
     *
     * @return наприклад {@code Парацетамол (таблетки): 45.50 грн, 365 дн., без рецепта}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s): %.2f грн, %d дн., %s",
                name, form, price, daysToExpire, kind.label());
    }

    /* Перевіряє обов'язковий текст: null — помилка програміста, порожній рядок — хибні дані. */
    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, "поле «%s» не може бути null".formatted(fieldName));
        if (value.isBlank()) {
            throw new IllegalArgumentException("порожнє поле «%s»".formatted(fieldName));
        }
        return value;
    }

    /* NaN перевіряється окремо: порівняння NaN < 0 повертає false. */
    private static double requirePrice(double price) {
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна має бути скінченним числом");
        }
        if (price < 0) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "від'ємна ціна %.2f", price));
        }
        return price;
    }

    private static int requireDays(int daysToExpire) {
        if (daysToExpire < 0) {
            throw new IllegalArgumentException("від'ємний термін придатності %d".formatted(daysToExpire));
        }
        return daysToExpire;
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
