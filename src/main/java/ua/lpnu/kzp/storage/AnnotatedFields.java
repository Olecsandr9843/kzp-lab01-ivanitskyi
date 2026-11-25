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
 * <p>Кеш — {@link ClassValue}: пошук і сортування полів виконуються один раз на клас, а
 * не під час кожного експорту (завдання із зірочкою).
 */
final class AnnotatedFields {
    private static final ClassValue<List<Field>> CACHE = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
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
     * @return незмінний список полів, відсортований за іменем; порожній, якщо анотацій немає
     * @throws DataStorageException якщо клас не має жодного поля з {@link CsvColumn}
     */
    static List<Field> of(Class<?> type) throws DataStorageException {
        List<Field> fields = CACHE.get(type);
        if (fields.isEmpty()) {
            throw new DataStorageException("клас %s не має полів з @CsvColumn".formatted(type.getName()));
        }
        return fields;
    }

    /**
     * Те саме, що {@link #of(Class)}, але без кешу — для порівняння часу в завданні із зірочкою.
     *
     * @param type клас записів
     * @return список анотованих полів
     */
    static List<Field> scan(Class<?> type) {
        List<Field> fields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .sorted(Comparator.comparing(Field::getName))
                .toList();
        // Поля приватні: дозволяємо читання один раз, тут, а не під час кожного get().
        fields.forEach(field -> field.setAccessible(true));
        return fields;
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
