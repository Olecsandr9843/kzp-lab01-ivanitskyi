package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Перевіряє параметризоване сховище {@link Repository}. */
class RepositoryTest {

    @Test
    void addAndAllKeepInsertionOrder() {
        Repository<String> repository = new Repository<>();

        repository.add("Б");
        repository.add("А");
        repository.addAll(List.of("В", "Г"));

        assertEquals(List.of("Б", "А", "В", "Г"), repository.all());
        assertEquals(4, repository.size());
    }

    /* Знімок незмінний і не залежить від подальших змін сховища. */
    @Test
    void allReturnsIndependentUnmodifiableSnapshot() {
        Repository<String> repository = new Repository<>();
        repository.add("А");

        List<String> snapshot = repository.all();
        repository.add("Б");

        assertEquals(List.of("А"), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("В"));
    }

    @Test
    void findFiltersByPredicate() {
        Repository<Integer> repository = new Repository<>();
        repository.addAll(List.of(5, 12, 7, 30));

        assertEquals(List.of(12, 30), repository.find(number -> number > 10));
        assertTrue(repository.find(number -> number > 100).isEmpty());
        assertEquals(Integer.valueOf(12), repository.findFirst(number -> number > 10).orElseThrow());
        assertTrue(repository.findFirst(number -> number < 0).isEmpty());
    }

    /* Predicate<? super T>: умова для надтипу підходить для сховища підтипу. */
    @Test
    void findAcceptsPredicateOfSupertype() {
        Repository<Integer> repository = new Repository<>();
        repository.addAll(List.of(1, 2, 3));
        Predicate<Number> positive = number -> number.doubleValue() > 1.5;

        assertEquals(List.of(2, 3), repository.find(positive));
    }

    @Test
    void rejectsNulls() {
        Repository<String> repository = new Repository<>();

        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertThrows(NullPointerException.class, () -> repository.find(null));
        assertThrows(NullPointerException.class, () -> repository.addAll(null));
    }

    @Test
    void emptyRepository() {
        Repository<String> repository = new Repository<>();

        assertTrue(repository.all().isEmpty());
        assertEquals(0, repository.size());
    }
}
