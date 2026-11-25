package ua.lpnu.kzp.pharmacy;

import java.util.List;

/**
 * Результат обробки вхідного файла: коректні препарати та повідомлення про пропущені рядки.
 *
 * <p>Клас незмінний: списки копіюються під час створення через {@link List#copyOf}. Оскільки
 * {@link Medicine} сам незмінний, копіювати окремі об'єкти (як масиви в ЛР1) уже не потрібно.
 */
public final class ProcessingResult {
    private final List<Medicine> medicines;
    private final List<String> errors;

    /**
     * Створює результат обробки.
     *
     * @param medicines коректні препарати в порядку появи у файлі
     * @param errors    повідомлення про пропущені рядки у форматі «Рядок N: причина»
     */
    public ProcessingResult(List<Medicine> medicines, List<String> errors) {
        this.medicines = List.copyOf(medicines);
        this.errors = List.copyOf(errors);
    }

    /**
     * Повертає коректні препарати.
     *
     * @return незмінний список у порядку появи у файлі
     */
    public List<Medicine> validRecords() {
        return medicines;
    }

    /**
     * Повертає повідомлення про пропущені рядки.
     *
     * @return незмінний список повідомлень
     */
    public List<String> errors() {
        return errors;
    }
}
