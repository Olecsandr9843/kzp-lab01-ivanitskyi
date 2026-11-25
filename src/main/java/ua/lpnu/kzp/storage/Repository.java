package ua.lpnu.kzp.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Параметризоване сховище записів у пам'яті.
 *
 * <p>Тип-параметр {@code T} фіксується під час створення ({@code new Repository<Medicine>()}),
 * тому компілятор не дасть додати запис іншого типу, а методи повертають {@code T} без
 * приведення типів. Внутрішній список недоступний ззовні: {@link #all()} і {@link #find}
 * повертають незмінні знімки.
 *
 * @param <T> тип записів
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /**
     * Створює порожнє сховище.
     */
    public Repository() {
        // Поле items уже ініціалізоване; явний конструктор потрібен лише для javadoc.
    }

    /**
     * Додає запис у кінець сховища.
     *
     * @param item запис, не {@code null}
     * @throws NullPointerException якщо {@code item} дорівнює {@code null}
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "запис не може бути null"));
    }

    /**
     * Додає всі записи колекції в порядку її обходу.
     *
     * <p>{@code ? extends T} дозволяє додати, наприклад, {@code List<FreeMedicine>} до
     * {@code Repository<Medicine>}.
     *
     * @param newItems записи, жоден не {@code null}
     * @throws NullPointerException якщо колекція або будь-який запис дорівнює {@code null}
     */
    public void addAll(Collection<? extends T> newItems) {
        Objects.requireNonNull(newItems, "колекція не може бути null").forEach(this::add);
    }

    /**
     * Повертає знімок усіх записів.
     *
     * @return незмінна копія у порядку додавання; подальші {@code add} її не змінюють
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову.
     *
     * <p>{@code Predicate<? super T>} дозволяє передати умову для надтипу, наприклад
     * {@code Predicate<Object>}.
     *
     * @param condition умова відбору
     * @return незмінний список у порядку додавання
     * @throws NullPointerException якщо {@code condition} дорівнює {@code null}
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "умова не може бути null");
        return items.stream().filter(condition).toList();
    }

    /**
     * Повертає перший запис, що задовольняє умову.
     *
     * @param condition умова пошуку
     * @return знайдений запис або {@link Optional#empty()}
     * @throws NullPointerException якщо {@code condition} дорівнює {@code null}
     */
    public Optional<T> findFirst(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "умова не може бути null");
        return items.stream().filter(condition).findFirst();
    }

    /**
     * Повертає кількість записів.
     *
     * @return кількість записів
     */
    public int size() {
        return items.size();
    }
}
