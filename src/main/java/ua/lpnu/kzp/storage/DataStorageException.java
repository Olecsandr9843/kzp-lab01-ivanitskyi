package ua.lpnu.kzp.storage;

/**
 * Помилка збереження або відновлення даних: файл недоступний, CSV пошкоджений, значення
 * має хибний формат або рефлексія не змогла прочитати поле.
 *
 * <p>Виняток перевірюваний (checked): компілятор змушує код, що експортує чи імпортує дані,
 * явно вирішити, що робити з помилкою. Первинна причина (наприклад, {@link java.io.IOException}
 * або {@link NumberFormatException}) зберігається й доступна через {@link #getCause()}.
 */
public class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток без первинної причини (помилку виявив наш власний код).
     *
     * @param message пояснення для користувача
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток, що обгортає первинну причину, не втрачаючи її стек викликів.
     *
     * @param message пояснення для користувача
     * @param cause   первинний виняток
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
