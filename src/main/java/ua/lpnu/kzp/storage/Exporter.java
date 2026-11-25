package ua.lpnu.kzp.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Спільний інтерфейс форматів експорту (завдання із зірочкою): CSV і JSON беруть дані з тих
 * самих полів, позначених {@link CsvColumn}.
 *
 * @param <T> тип записів
 */
public interface Exporter<T> {

    /**
     * Перетворює записи на текст у форматі експортера.
     *
     * @param records записи; порожній список дає документ без даних
     * @return готовий текст
     * @throws DataStorageException якщо поле не вдалося прочитати
     */
    String export(List<? extends T> records) throws DataStorageException;

    /**
     * Записує експортований текст у файл у кодуванні UTF-8, створюючи батьківський каталог.
     *
     * @param path    шлях до файла
     * @param records записи
     * @throws DataStorageException якщо експорт або запис файла неможливий; причина — у {@code getCause()}
     */
    default void write(Path path, List<? extends T> records) throws DataStorageException {
        String text = export(records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("не вдалося записати файл %s".formatted(path), exception);
        }
    }
}
