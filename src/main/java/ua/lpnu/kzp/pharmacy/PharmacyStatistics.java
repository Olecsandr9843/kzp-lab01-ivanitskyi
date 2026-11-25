package ua.lpnu.kzp.pharmacy;

import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Обчислює чотири показники варіанта 11 «Аптека».
 *
 * <ol>
 *     <li>кількість коректних записів;</li>
 *     <li>середня ціна препарату;</li>
 *     <li>найменший термін до закінчення придатності;</li>
 *     <li>кількість рецептурних препаратів.</li>
 * </ol>
 *
 * <p>Показники 3 і 4 також доступні разом як record {@link ExpiryPrescription}.
 */
public final class PharmacyStatistics {
    private final int validCount;
    private final double averagePrice;
    private final ExpiryPrescription expiryPrescription;

    /**
     * Обчислює показники за списком препаратів.
     *
     * @param medicines коректні препарати; кожен об'єкт уже перевірено конструктором {@link Medicine}
     */
    public PharmacyStatistics(List<Medicine> medicines) {
        // ЛР4: цикл замінено потоками. Статистика цін рахується одним проходом колектора.
        DoubleSummaryStatistics prices = MedicineQueries.priceStatistics(medicines);
        this.validCount = (int) prices.getCount();
        // getAverage() для порожнього набору повертає 0.0 — як і попередня реалізація.
        this.averagePrice = prices.getAverage();
        // min() повертає OptionalInt: порожній для порожнього набору, тоді 0, як раніше.
        int minDays = medicines.stream()
                .mapToInt(Medicine::getDaysToExpire)
                .min()
                .orElse(0);
        int prescriptions = (int) medicines.stream()
                .filter(Medicine::isPrescription)
                .count();
        this.expiryPrescription = new ExpiryPrescription(minDays, prescriptions);
    }

    /**
     * Повертає кількість коректних записів.
     *
     * @return кількість записів
     */
    public int validCount() {
        return validCount;
    }

    /**
     * Повертає середню ціну препарату.
     *
     * @return середня ціна в гривнях або 0, якщо записів немає
     */
    public double averagePrice() {
        return averagePrice;
    }

    /**
     * Повертає найменшу кількість днів до закінчення терміну придатності.
     *
     * @return мінімум днів; має сенс лише якщо {@link #isEmpty()} повертає {@code false}
     */
    public int minDaysToExpire() {
        return expiryPrescription.minDaysToExpire();
    }

    /**
     * Повертає кількість рецептурних препаратів.
     *
     * @return кількість препаратів, для яких потрібен рецепт
     */
    public int prescriptionCount() {
        return expiryPrescription.prescriptionCount();
    }

    /**
     * Повертає підсумок «найменший термін + кількість рецептурних» як незмінне значення.
     *
     * @return record із двома показниками
     */
    public ExpiryPrescription expiryPrescription() {
        return expiryPrescription;
    }

    /**
     * Повідомляє, чи не було жодного коректного запису.
     *
     * @return {@code true}, якщо показники обчислено за порожнім набором
     */
    public boolean isEmpty() {
        return validCount == 0;
    }
}
