package ua.lpnu.kzp.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Документний CSV-парсер за RFC 4180.
 *
 * <p>Розбирає весь текст файла посимвольно. Простий {@code split(",")} чи читання по рядках
 * тут не підходять: кома й перенесення рядка всередині лапок належать значенню, а не є
 * межею поля чи запису.
 *
 * <ul>
 *     <li>роздільник полів — кома; межа запису — {@code \n}, {@code \r\n} або {@code \r};</li>
 *     <li>поле в лапках може містити кому, перенесення рядка й подвоєні лапки {@code ""};</li>
 *     <li>порожні рядки між записами пропускаються, BOM на початку ігнорується;</li>
 *     <li>незакриті лапки, лапки всередині поля без обрамлення чи символ після закривальних
 *     лапок — помилка з номером рядка.</li>
 * </ul>
 */
public final class CsvParser {

    /**
     * Один запис CSV.
     *
     * @param line   номер фізичного рядка файла, з якого запис починається (від 1)
     * @param fields значення полів у порядку стовпців
     */
    public record Row(int line, List<String> fields) {
        /**
         * Створює запис із незмінною копією полів.
         *
         * @param line   номер рядка
         * @param fields значення полів
         */
        public Row {
            fields = List.copyOf(fields);
        }
    }

    /* Забороняє створення екземплярів службового класу. */
    private CsvParser() {
    }

    /**
     * Розбирає CSV-документ на записи.
     *
     * @param text увесь вміст файла
     * @return незмінний список записів; порожній, якщо в тексті немає даних
     * @throws DataStorageException якщо порушено синтаксис CSV
     */
    public static List<Row> parse(String text) throws DataStorageException {
        List<Row> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterClosingQuote = false;
        boolean fieldStarted = false;
        int line = 1;
        int rowStartLine = 1;
        int start = !text.isEmpty() && text.charAt(0) == '﻿' ? 1 : 0;

        for (int i = start; i < text.length(); i++) {
            char symbol = text.charAt(i);
            if (quoted) {
                if (symbol == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        afterClosingQuote = true;
                    }
                } else {
                    if (symbol == '\n' || symbol == '\r' && !(i + 1 < text.length() && text.charAt(i + 1) == '\n')) {
                        line++;
                    }
                    field.append(symbol);
                }
                continue;
            }
            switch (symbol) {
                case '"' -> {
                    if (fieldStarted) {
                        throw new DataStorageException("рядок %d: лапки всередині поля без обрамлення".formatted(line));
                    }
                    quoted = true;
                    fieldStarted = true;
                }
                case ',' -> {
                    fields.add(field.toString());
                    field.setLength(0);
                    afterClosingQuote = false;
                    fieldStarted = false;
                }
                case '\r', '\n' -> {
                    if (symbol == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    // Порожній фізичний рядок (жодного символу) не є записом.
                    if (fieldStarted || !fields.isEmpty()) {
                        fields.add(field.toString());
                        rows.add(new Row(rowStartLine, fields));
                    }
                    fields = new ArrayList<>();
                    field.setLength(0);
                    afterClosingQuote = false;
                    fieldStarted = false;
                    line++;
                    rowStartLine = line;
                }
                default -> {
                    if (afterClosingQuote) {
                        throw new DataStorageException("рядок %d: зайвий символ після закривальних лапок".formatted(line));
                    }
                    field.append(symbol);
                    fieldStarted = true;
                }
            }
        }
        if (quoted) {
            throw new DataStorageException("рядок %d: незакриті лапки".formatted(rowStartLine));
        }
        // Останній запис без завершального перенесення рядка.
        if (fieldStarted || !fields.isEmpty()) {
            fields.add(field.toString());
            rows.add(new Row(rowStartLine, fields));
        }
        return List.copyOf(rows);
    }
}
