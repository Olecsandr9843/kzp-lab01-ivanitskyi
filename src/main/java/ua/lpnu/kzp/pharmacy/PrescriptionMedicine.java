package ua.lpnu.kzp.pharmacy;

/**
 * Рецептурний препарат: купити його можна лише після візиту до лікаря.
 *
 * <p>Правило доступності: стартова оцінка {@value #BASE_SCORE} (рецепт — суттєвий бар'єр),
 * мінус штраф за ціну, але не нижче {@value #MIN_SCORE}, поки препарат придатний: призначений
 * лікарем препарат пацієнт придбає навіть дорогим. Якщо термін спливає сьогодні — 0.
 */
public final class PrescriptionMedicine extends Medicine {
    /** Стартова оцінка доступності рецептурного препарату. */
    public static final int BASE_SCORE = 60;
    /** Найнижча оцінка придатного рецептурного препарату. */
    public static final int MIN_SCORE = 10;

    /**
     * Створює рецептурний препарат. Спільні поля перевіряє {@link Medicine}.
     *
     * @param name         назва препарату
     * @param form         форма випуску
     * @param price        ціна в гривнях
     * @param daysToExpire днів до закінчення терміну придатності
     * @throws NullPointerException     якщо текстове поле дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено інваріант базового типу
     */
    public PrescriptionMedicine(String name, String form, double price, int daysToExpire) {
        super(name, form, price, daysToExpire, MedicineKind.PRESCRIPTION);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Для рецептурного препарату: {@code max(10, 60 − штраф за ціну)}, або 0, якщо термін
     * спливає сьогодні.
     */
    @Override
    public int availabilityScore() {
        if (expiresToday()) {
            return 0;
        }
        return Math.max(MIN_SCORE, BASE_SCORE - pricePenalty());
    }
}
