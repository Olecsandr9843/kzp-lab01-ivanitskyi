package ua.lpnu.kzp.pharmacy;

/**
 * Категорія відпуску препарату. Фіксована множина значень, тому це {@code enum}, а не рядок
 * чи {@code boolean}: компілятор не дасть передати неіснуючу категорію.
 */
public enum MedicineKind {
    /** Відпускається лише за рецептом лікаря. */
    PRESCRIPTION("за рецептом", true),
    /** Відпускається без рецепта. */
    FREE("без рецепта", false);

    private final String label;
    private final boolean requiresPrescription;

    /* Конструктор enum викликається один раз для кожної константи під час завантаження класу. */
    MedicineKind(String label, boolean requiresPrescription) {
        this.label = label;
        this.requiresPrescription = requiresPrescription;
    }

    /**
     * Повертає підпис категорії для звіту.
     *
     * @return український підпис, наприклад «за рецептом»
     */
    public String label() {
        return label;
    }

    /**
     * Повідомляє, чи потрібен рецепт для препаратів цієї категорії.
     *
     * @return {@code true} для {@link #PRESCRIPTION}
     */
    public boolean requiresPrescription() {
        return requiresPrescription;
    }

    /**
     * Визначає категорію за значенням поля {@code prescription} із CSV.
     *
     * @param prescription значення поля
     * @return {@link #PRESCRIPTION} для {@code true}, {@link #FREE} для {@code false}
     */
    public static MedicineKind fromPrescriptionFlag(boolean prescription) {
        return prescription ? PRESCRIPTION : FREE;
    }
}
