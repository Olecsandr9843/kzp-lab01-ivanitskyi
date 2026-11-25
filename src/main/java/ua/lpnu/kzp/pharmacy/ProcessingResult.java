package ua.lpnu.kzp.pharmacy;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат обробки вхідного файла: коректні записи та повідомлення про пропущені рядки.
 *
 * <p>Клас незмінний: списки копіюються під час створення й повертаються лише для читання.
 */
public final class ProcessingResult {
    private final List<String[]> validRecords;
    private final List<String> errors;

    /**
     * Створює результат обробки.
     *
     * @param validRecords перевірені записи, кожен — масив полів
     * @param errors       повідомлення про пропущені рядки у форматі «Рядок N: причина»
     */
    public ProcessingResult(List<String[]> validRecords, List<String> errors) {
        List<String[]> copy = new ArrayList<>(validRecords.size());
        for (String[] fields : validRecords) {
            // Масив змінний, тому зберігаємо його копію, а не посилання на чужий об'єкт.
            copy.add(fields.clone());
        }
        this.validRecords = List.copyOf(copy);
        this.errors = List.copyOf(errors);
    }

    /**
     * Повертає копії коректних записів.
     *
     * @return список масивів полів у порядку появи у файлі
     */
    public List<String[]> validRecords() {
        List<String[]> copy = new ArrayList<>(validRecords.size());
        for (String[] fields : validRecords) {
            copy.add(fields.clone());
        }
        return copy;
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
