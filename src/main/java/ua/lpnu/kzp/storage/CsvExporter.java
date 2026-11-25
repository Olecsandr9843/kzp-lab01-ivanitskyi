package ua.lpnu.kzp.storage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Універсальний рефлексивний CSV-експортер.
 *
 * <p>Працює з будь-яким класом, поля якого позначені {@link CsvColumn}, і не містить жодних
 * назв предметних класів. Формат:
 * <ul>
 *     <li>роздільник полів — кома, роздільник записів — {@link System#lineSeparator()};</li>
 *     <li>перший рядок — заголовок із назв {@link CsvColumn#value()};</li>
 *     <li>стовпці впорядковані за Java-іменами полів;</li>
 *     <li>значення з комою, лапками чи перенесенням рядка беруться в лапки, а лапки
 *     всередині подвоюються (RFC 4180);</li>
 *     <li>{@code null} записується як порожнє поле, решта — через {@link String#valueOf(Object)}
 *     (для {@code double} це не залежить від локалі, для {@code LocalDate} — формат ISO,
 *     для {@code enum} — ім'я константи).</li>
 * </ul>
 *
 * @param <T> тип записів
 */
public final class CsvExporter<T> implements Exporter<T> {
    private final Class<T> type;

    /**
     * Створює експортер для класу.
     *
     * @param type клас записів; має містити хоча б одне поле з {@link CsvColumn}
     */
    public CsvExporter(Class<T> type) {
        this.type = Objects.requireNonNull(type, "клас не може бути null");
    }

    /**
     * Повертає рядок заголовка без символу завершення рядка.
     *
     * @return назви стовпців через кому, з екрануванням
     * @throws DataStorageException якщо клас не має анотованих полів
     */
    public String header() throws DataStorageException {
        StringBuilder header = new StringBuilder();
        for (Field field : AnnotatedFields.of(type)) {
            if (!header.isEmpty()) {
                header.append(',');
            }
            header.append(escape(AnnotatedFields.column(field)));
        }
        return header.toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Результат: заголовок і по одному запису на рядок; кожен рядок, включно з останнім,
     * завершується {@link System#lineSeparator()}.
     */
    @Override
    public String export(List<? extends T> records) throws DataStorageException {
        Objects.requireNonNull(records, "записи не можуть бути null");
        List<Field> fields = AnnotatedFields.of(type);
        String newline = System.lineSeparator();
        StringBuilder csv = new StringBuilder(header()).append(newline);
        for (T record : records) {
            Objects.requireNonNull(record, "запис не може бути null");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    csv.append(',');
                }
                Object value = AnnotatedFields.read(fields.get(i), record);
                csv.append(value == null ? "" : escape(String.valueOf(value)));
            }
            csv.append(newline);
        }
        return csv.toString();
    }

    /**
     * Екранує одне значення за правилами RFC 4180.
     *
     * @param value значення поля
     * @return значення без змін або в лапках із подвоєними внутрішніми лапками
     */
    public static String escape(String value) {
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
