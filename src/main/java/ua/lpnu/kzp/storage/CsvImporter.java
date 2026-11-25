package ua.lpnu.kzp.storage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Універсальний CSV-імпортер — пара до {@link CsvExporter}.
 *
 * <p>Очікуваний заголовок імпортер бере з анотацій {@link CsvColumn} класу, а створення
 * об'єкта з одного запису делегує предметному класу (фабриці {@code rowFactory}), який
 * отримує карту «назва стовпця → текст». Так імпортер не знає ні полів, ні правил валідації
 * конкретного класу.
 *
 * <p>Імпорт атомарний: перша ж помилка зупиняє його з {@link DataStorageException}, у якій
 * вказано номер рядка, а первинна причина збережена в {@code getCause()}. Відновлення з
 * файла має дати або всі записи, або жодного.
 *
 * @param <T> тип записів
 */
public final class CsvImporter<T> {
    private final Class<T> type;
    private final Function<Map<String, String>, T> rowFactory;

    /**
     * Створює імпортер.
     *
     * @param type       клас записів; має містити поля з {@link CsvColumn}
     * @param rowFactory створює запис із карти «стовпець → значення»; про хибні дані повідомляє
     *                   {@link IllegalArgumentException} або {@link DateTimeException}
     */
    public CsvImporter(Class<T> type, Function<Map<String, String>, T> rowFactory) {
        this.type = Objects.requireNonNull(type, "клас не може бути null");
        this.rowFactory = Objects.requireNonNull(rowFactory, "фабрика не може бути null");
    }

    /**
     * Читає файл у кодуванні UTF-8 і відновлює записи.
     *
     * @param path шлях до CSV-файла
     * @return незмінний список записів у порядку файла
     * @throws DataStorageException якщо файл не прочитано (причина — {@link IOException}) або
     *                              вміст некоректний
     */
    public List<T> read(Path path) throws DataStorageException {
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("не вдалося прочитати файл %s".formatted(path), exception);
        }
        return parse(text);
    }

    /**
     * Відновлює записи з тексту CSV.
     *
     * @param text увесь вміст файла
     * @return незмінний список записів; порожній, якщо після заголовка немає даних
     * @throws DataStorageException якщо немає заголовка, заголовок не збігається з анотаціями,
     *                              кількість полів хибна або фабрика відхилила значення
     */
    public List<T> parse(String text) throws DataStorageException {
        List<String> expectedHeader = new ArrayList<>();
        for (Field field : AnnotatedFields.of(type)) {
            expectedHeader.add(AnnotatedFields.column(field));
        }
        List<CsvParser.Row> rows = CsvParser.parse(text);
        if (rows.isEmpty()) {
            throw new DataStorageException("файл порожній: немає рядка заголовка");
        }
        List<String> header = rows.get(0).fields();
        if (!header.equals(expectedHeader)) {
            throw new DataStorageException("очікується заголовок %s, отримано %s".formatted(expectedHeader, header));
        }

        List<T> records = new ArrayList<>();
        for (CsvParser.Row row : rows.subList(1, rows.size())) {
            if (row.fields().size() != header.size()) {
                throw new DataStorageException("рядок %d: очікується %d полів, отримано %d"
                        .formatted(row.line(), header.size(), row.fields().size()));
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
                values.put(header.get(i), row.fields().get(i));
            }
            try {
                records.add(rowFactory.apply(values));
            } catch (IllegalArgumentException | DateTimeException exception) {
                throw new DataStorageException("рядок %d: %s".formatted(row.line(), exception.getMessage()), exception);
            }
        }
        return List.copyOf(records);
    }
}
