package ua.lpnu.kzp.pharmacy;

/**
 * Безрецептурний препарат: продається будь-кому.
 *
 * <p>Правило доступності: стартова оцінка {@value Medicine#MAX_SCORE}, мінус штраф за ціну;
 * якщо до кінця терміну залишилося менше {@value #SHORT_EXPIRY_DAYS} днів, ще мінус
 * {@value #SHORT_EXPIRY_PENALTY} (такий товар аптека прибирає з вітрини). Якщо термін
 * спливає сьогодні — 0.
 */
public final class FreeMedicine extends Medicine {
    /** Поріг «короткого» терміну придатності, днів. */
    public static final int SHORT_EXPIRY_DAYS = 30;
    /** Штраф за короткий термін придатності. */
    public static final int SHORT_EXPIRY_PENALTY = 20;

    /**
     * Створює безрецептурний препарат. Спільні поля перевіряє {@link Medicine}.
     *
     * @param name         назва препарату
     * @param form         форма випуску
     * @param price        ціна в гривнях
     * @param daysToExpire днів до закінчення терміну придатності
     * @throws NullPointerException     якщо текстове поле дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено інваріант базового типу
     */
    public FreeMedicine(String name, String form, double price, int daysToExpire) {
        super(name, form, price, daysToExpire, MedicineKind.FREE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Для безрецептурного препарату: {@code 100 − штраф за ціну}, ще {@code − 20}, якщо до
     * кінця терміну менше 30 днів, або 0, якщо термін спливає сьогодні.
     */
    @Override
    public int availabilityScore() {
        if (expiresToday()) {
            return 0;
        }
        int score = MAX_SCORE - pricePenalty();
        if (getDaysToExpire() < SHORT_EXPIRY_DAYS) {
            score -= SHORT_EXPIRY_PENALTY;
        }
        return score;
    }
}
