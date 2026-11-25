package ua.lpnu.kzp.storage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Універсальний рефлексивний JSON-експортер (завдання із зірочкою).
 *
 * <p>Використовує ті самі поля й назви з {@link CsvColumn}, що й {@link CsvExporter}: масив
 * об'єктів, по одному на рядок. Числа ({@link Number}) і логічні значення записуються без
 * лапок, решта — рядками JSON з екрануванням, {@code null} — як {@code null}.
 *
 * @param <T> тип записів
 */
public final class JsonExporter<T> implements Exporter<T> {
    private final Class<T> type;

    /**
     * Створює експортер для класу.
     *
     * @param type клас записів; має містити хоча б одне поле з {@link CsvColumn}
     */
    public JsonExporter(Class<T> type) {
        this.type = Objects.requireNonNull(type, "клас не може бути null");
    }

    @Override
    public String export(List<? extends T> records) throws DataStorageException {
        Objects.requireNonNull(records, "записи не можуть бути null");
        List<Field> fields = AnnotatedFields.of(type);
        String newline = System.lineSeparator();
        StringBuilder json = new StringBuilder("[");
        for (int r = 0; r < records.size(); r++) {
            T record = Objects.requireNonNull(records.get(r), "запис не може бути null");
            json.append(r == 0 ? newline : "," + newline).append("  {");
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                json.append(i == 0 ? "" : ", ")
                        .append(quote(AnnotatedFields.column(field)))
                        .append(": ")
                        .append(value(AnnotatedFields.read(field, record)));
            }
            json.append('}');
        }
        return json.append(records.isEmpty() ? "" : newline).append(']').append(newline).toString();
    }

    /* Число чи логічне значення — як є; null — null; решта — рядок у лапках. */
    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return quote(String.valueOf(value));
    }

    /**
     * Перетворює текст на рядок JSON: лапки, зворотна скісна риска й керівні символи
     * екрануються.
     *
     * @param text текст
     * @return текст у подвійних лапках
     */
    public static String quote(String text) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char symbol = text.charAt(i);
            switch (symbol) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (symbol < 0x20) {
                        quoted.append("\\u%04x".formatted((int) symbol));
                    } else {
                        quoted.append(symbol);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }
}
