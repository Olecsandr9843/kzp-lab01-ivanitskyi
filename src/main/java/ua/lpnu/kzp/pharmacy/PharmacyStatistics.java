package ua.lpnu.kzp.pharmacy;

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
 */
public final class PharmacyStatistics {
    private final int validCount;
    private final double averagePrice;
    private final int minDaysToExpire;
    private final int prescriptionCount;

    /**
     * Обчислює показники за перевіреними записами.
     *
     * @param records записи, які вже пройшли {@link MedicineLineParser#parse(String)}
     */
    public PharmacyStatistics(List<String[]> records) {
        double totalPrice = 0.0;
        int minDays = Integer.MAX_VALUE;
        int prescriptions = 0;
        for (String[] fields : records) {
            totalPrice += Double.parseDouble(fields[MedicineLineParser.PRICE]);
            minDays = Math.min(minDays, Integer.parseInt(fields[MedicineLineParser.DAYS_TO_EXPIRE]));
            if (MedicineLineParser.parsePrescription(fields[MedicineLineParser.PRESCRIPTION])) {
                prescriptions++;
            }
        }
        this.validCount = records.size();
        // Ділимо в double; для порожнього набору повертаємо 0, щоб не отримати NaN.
        this.averagePrice = validCount == 0 ? 0.0 : totalPrice / validCount;
        this.minDaysToExpire = validCount == 0 ? 0 : minDays;
        this.prescriptionCount = prescriptions;
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
        return minDaysToExpire;
    }

    /**
     * Повертає кількість рецептурних препаратів.
     *
     * @return кількість записів із {@code prescription = true}
     */
    public int prescriptionCount() {
        return prescriptionCount;
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
