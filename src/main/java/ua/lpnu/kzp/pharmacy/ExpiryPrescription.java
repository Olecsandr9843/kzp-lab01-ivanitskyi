package ua.lpnu.kzp.pharmacy;

/**
 * Незмінний підсумок про терміни придатності та рецептурність асортименту.
 *
 * <p>Record автоматично отримує приватні фінальні поля, канонічний конструктор, методи
 * доступу {@code minDaysToExpire()} і {@code prescriptionCount()}, а також {@code equals},
 * {@code hashCode} і {@code toString}, що порівнюють і показують значення компонентів.
 *
 * @param minDaysToExpire   найменша кількість днів до закінчення терміну придатності
 *                          (0, якщо препаратів немає)
 * @param prescriptionCount кількість рецептурних препаратів
 */
public record ExpiryPrescription(int minDaysToExpire, int prescriptionCount) {

    /**
     * Компактний канонічний конструктор: перевіряє, що обидва показники не від'ємні.
     *
     * @throws IllegalArgumentException якщо будь-який показник від'ємний
     */
    public ExpiryPrescription {
        if (minDaysToExpire < 0 || prescriptionCount < 0) {
            throw new IllegalArgumentException("показники не можуть бути від'ємними");
        }
    }
}
