package ua.lpnu.kzp.storage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Знаходить рефлексією поля, позначені {@link CsvColumn}, і кешує результат для кожного класу.
 *
 * <p>Порядок стовпців детермінований — за алфавітним сортуванням Java-імен полів, бо
 * {@link Class#getDeclaredFields()} порядку не гарантує.
 *
 * <p>Кеш — {@link ClassValue}: пошук, сортування й відкриття доступу виконуються один раз на
 * клас, а не під час кожного експорту (завдання із зірочкою).
 */
final class AnnotatedFields {

    /**
     * Результат сканування класу.
     *
     * @param fields       анотовані поля, відсортовані за іменем
     * @param inaccessible імена полів, до яких {@link Field#trySetAccessible()} не відкрив доступ
     */
    record Scan(List<Field> fields, List<String> inaccessible) {
        Scan {
            fields = List.copyOf(fields);
            inaccessible = List.copyOf(inaccessible);
        }
    }

    private static final ClassValue<Scan> CACHE = new ClassValue<>() {
        @Override
        protected Scan computeValue(Class<?> type) {
            return scan(type);
        }
    };

    /* Забороняє створення екземплярів службового класу. */
    private AnnotatedFields() {
    }

    /**
     * Повертає анотовані поля класу з кешу.
     *
     * @param type клас записів
     * @return незмінний список полів, відсортований за іменем
     * @throws DataStorageException якщо клас не має полів з {@link CsvColumn} або доступ до
     *                              якогось із них не вдалося відкрити
     */
    static List<Field> of(Class<?> type) throws DataStorageException {
        Scan scan = CACHE.get(type);
        if (scan.fields().isEmpty()) {
            throw new DataStorageException("клас %s не має полів з @CsvColumn".formatted(type.getName()));
        }
        if (!scan.inaccessible().isEmpty()) {
            throw new DataStorageException("немає доступу до полів %s класу %s"
                    .formatted(scan.inaccessible(), type.getName()));
        }
        return scan.fields();
    }

    /**
     * Сканує клас без кешу (використовується кешем і для порівняння часу в завданні із зірочкою).
     *
     * @param type клас записів
     * @return анотовані нестатичні поля за іменем і список полів без доступу
     */
    static Scan scan(Class<?> type) {
        List<Field> fields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .toList();
        // Поля приватні: просимо доступ один раз, тут, а не під час кожного get().
        // trySetAccessible() не кидає виняток, а повертає false (наприклад, для класу з іншого
        // модуля, який не відкриває пакет) — такі поля запам'ятовуємо й повідомляємо в of().
        List<String> inaccessible = fields.stream()
                .filter(field -> !field.trySetAccessible())
                .map(Field::getName)
                .toList();
        return new Scan(fields, inaccessible);
    }

    /**
     * Повертає назву стовпця поля з анотації.
     *
     * @param field анотоване поле
     * @return значення {@link CsvColumn#value()}
     */
    static String column(Field field) {
        return field.getAnnotation(CsvColumn.class).value();
    }

    /**
     * Читає значення поля об'єкта, обгортаючи помилки рефлексії у {@link DataStorageException}.
     *
     * @param field  анотоване поле
     * @param record об'єкт
     * @return значення поля (може бути {@code null})
     * @throws DataStorageException якщо поле недоступне для читання
     */
    static Object read(Field field, Object record) throws DataStorageException {
        try {
            return field.get(record);
        } catch (IllegalAccessException exception) {
            throw new DataStorageException("не вдалося прочитати поле %s".formatted(field.getName()), exception);
        }
    }
}
