package ua.lpnu.kzp.pharmacy;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Запити Stream API до асортименту аптеки (варіант 11).
 *
 * <p>Кожен метод — один конвеєр «джерело → проміжні операції → кінцева операція» з одним
 * результатом. Методи не мають побічних ефектів: вхідний список не змінюється, а результат
 * створюється заново. Форматування для консолі сюди не входить — ним займається
 * {@link ReportFormatter}.
 *
 * <p>Усі методи працюють із базовим типом {@link Medicine}, без перевірок конкретного підтипу.
 */
public final class MedicineQueries {
    /** Поріг «короткого» терміну придатності для фільтра, днів (включно). */
    public static final int EXPIRY_THRESHOLD_DAYS = 30;

    /**
     * Порядок «найближчий термін придатності першим, при рівному терміні — за назвою».
     * Обидва ключі за зростанням, тому {@code reversed()} тут не потрібен.
     */
    public static final Comparator<Medicine> BY_NEAREST_EXPIRY =
            Comparator.comparingInt(Medicine::getDaysToExpire)
                    .thenComparing(Medicine::getName);

    /**
     * Порядок «найвища оцінка доступності першою, при рівній оцінці — за назвою».
     * {@code reversed()} застосовано лише до першого ключа: інакше назви теж пішли б
     * у зворотному порядку.
     */
    public static final Comparator<Medicine> BY_AVAILABILITY_DESC =
            Comparator.comparingInt(Medicine::availabilityScore).reversed()
                    .thenComparing(Medicine::getName);

    /* Забороняє створення екземплярів службового класу. */
    private MedicineQueries() {
    }

    /**
     * Запит 1 (filter): препарати, термін придатності яких спливає не пізніше ніж за
     * {@value #EXPIRY_THRESHOLD_DAYS} днів.
     *
     * @param medicines препарати
     * @return новий незмінний список у вихідному порядку; порожній, якщо таких немає
     */
    public static List<Medicine> expiringSoon(List<Medicine> medicines) {
        return medicines.stream()
                .filter(medicine -> medicine.getDaysToExpire() <= EXPIRY_THRESHOLD_DAYS)
                .toList();
    }

    /**
     * Запит 2 (map): назви препаратів без повторів у порядку першої появи.
     *
     * <p>{@code distinct} порівнює рядки через {@code equals}, тому «Парацетамол» у таблетках і
     * в сиропі дадуть одну назву.
     *
     * @param medicines препарати
     * @return незмінний список назв
     */
    public static List<String> names(List<Medicine> medicines) {
        return medicines.stream()
                .map(Medicine::getName)
                .distinct()
                .toList();
    }

    /**
     * Запит 3 (groupingBy): кількість препаратів за формою випуску.
     *
     * <p>{@link TreeMap} дає сталий алфавітний порядок ключів; downstream-колектор
     * {@link Collectors#counting()} рахує елементи групи.
     *
     * @param medicines препарати
     * @return карта «форма → кількість», ключі відсортовані; порожня для порожнього входу
     */
    public static Map<String, Long> countByForm(List<Medicine> medicines) {
        return medicines.stream()
                .collect(Collectors.groupingBy(Medicine::getForm, TreeMap::new, Collectors.counting()));
    }

    /**
     * Завдання із зірочкою: дворівневе групування «категорія відпуску → форма → кількість».
     *
     * @param medicines препарати
     * @return карта з ключами {@link MedicineKind} у порядку оголошення enum; внутрішні карти
     *         відсортовані за формою
     */
    public static Map<MedicineKind, Map<String, Long>> countByKindAndForm(List<Medicine> medicines) {
        return medicines.stream()
                .collect(Collectors.groupingBy(Medicine::getKind,
                        () -> new EnumMap<>(MedicineKind.class),
                        Collectors.groupingBy(Medicine::getForm, TreeMap::new, Collectors.counting())));
    }

    /**
     * Запит 4 (summarizingDouble): статистика цін — кількість, сума, середнє, мінімум, максимум.
     *
     * <p>Для порожнього входу: кількість 0, сума 0.0, середнє 0.0, мінімум
     * {@code +Infinity}, максимум {@code -Infinity}. Тому перед виведенням мінімуму й
     * максимуму слід перевіряти {@code getCount() > 0}.
     *
     * @param medicines препарати
     * @return новий об'єкт статистики
     */
    public static DoubleSummaryStatistics priceStatistics(List<Medicine> medicines) {
        return medicines.stream()
                .collect(Collectors.summarizingDouble(Medicine::getPrice));
    }

    /**
     * Запит 5 (sorted + limit): перші {@code n} препаратів за {@link #BY_NEAREST_EXPIRY}.
     *
     * <p>Спочатку {@code sorted}, потім {@code limit}: навпаки ми взяли б перші {@code n}
     * за порядком у файлі й лише їх відсортували.
     *
     * @param medicines препарати
     * @param n         скільки елементів повернути; 0 — порожній список, більше за розмір — усі
     * @return незмінний список довжиною {@code min(n, size)}
     * @throws IllegalArgumentException якщо {@code n < 0}
     */
    public static List<Medicine> topNearestExpiry(List<Medicine> medicines, int n) {
        return top(medicines, n, BY_NEAREST_EXPIRY);
    }

    /**
     * Додатковий top-N: перші {@code n} препаратів за {@link #BY_AVAILABILITY_DESC}.
     * Ключ сортування — поліморфний метод {@link Medicine#availabilityScore()}.
     *
     * @param medicines препарати
     * @param n         скільки елементів повернути
     * @return незмінний список довжиною {@code min(n, size)}
     * @throws IllegalArgumentException якщо {@code n < 0}
     */
    public static List<Medicine> topByAvailability(List<Medicine> medicines, int n) {
        return top(medicines, n, BY_AVAILABILITY_DESC);
    }

    /**
     * Пошук (Optional): перший препарат із вказаною назвою без урахування регістру й
     * зайвих пробілів.
     *
     * @param medicines препарати
     * @param name      назва для пошуку
     * @return знайдений препарат або {@link Optional#empty()}, якщо такого немає
     * @throws NullPointerException якщо {@code name} дорівнює {@code null}
     */
    public static Optional<Medicine> findByName(List<Medicine> medicines, String name) {
        String wanted = Objects.requireNonNull(name, "назва для пошуку не може бути null")
                .strip().toLowerCase(Locale.ROOT);
        return medicines.stream()
                .filter(medicine -> medicine.getName().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    /* Спільна частина обох top-N: перевірка n, сортування, обмеження. */
    private static List<Medicine> top(List<Medicine> medicines, int n, Comparator<Medicine> order) {
        if (n < 0) {
            throw new IllegalArgumentException("n не може бути від'ємним: " + n);
        }
        return medicines.stream()
                .sorted(order)
                .limit(n)
                .toList();
    }
}
