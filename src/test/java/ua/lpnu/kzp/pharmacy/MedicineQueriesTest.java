package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Перевіряє п'ять запитів Stream API і пошук на контрольному наборі {@code data/pharmacy-full.csv}
 * (13 препаратів: межа 30/31 день, дублікати, рівний термін у різних препаратів).
 */
class MedicineQueriesTest {
    private static final double DELTA = 1e-9;
    private static List<Medicine> medicines;

    @BeforeAll
    static void loadControlSet() throws IOException {
        // Maven запускає тести з кореня проєкту, тому відносний шлях працює на всіх ОС.
        List<String> lines = FileReport.readLines(Path.of("data", "pharmacy-full.csv"));
        ProcessingResult result = RecordProcessor.process(lines);
        assertTrue(result.errors().isEmpty(), result.errors().toString());
        medicines = result.validRecords();
    }

    private static List<String> namesOf(List<Medicine> list) {
        return list.stream().map(Medicine::getName).toList();
    }

    // ---------- 1. filter ----------

    @Test
    void expiringSoonKeepsOnlyThirtyDaysOrLess() {
        List<Medicine> result = MedicineQueries.expiringSoon(medicines);

        assertEquals(List.of("Назол", "Левомеколь", "Цетиризин", "Інсулін", "Парацетамол", "Мелоксикам"),
                namesOf(result));
    }

    /* Межа умови: 30 днів входить, 31 — ні. */
    @Test
    void expiringSoonBoundaryIsInclusive() {
        List<Medicine> result = MedicineQueries.expiringSoon(List.of(
                Medicine.of("Тридцять", "таблетки", 1, 30, false),
                Medicine.of("Тридцять один", "таблетки", 1, 31, false)));

        assertEquals(List.of("Тридцять"), namesOf(result));
    }

    // ---------- 2. map ----------

    /* Парацетамол тричі у вхідних даних (дві форми, дві партії) — у результаті один раз. */
    @Test
    void namesAreDistinctInFirstAppearanceOrder() {
        List<String> names = MedicineQueries.names(medicines);

        assertEquals(11, names.size());
        assertEquals("Парацетамол", names.get(0));
        assertEquals("Мелоксикам", names.get(10));
    }

    // ---------- 3. groupingBy ----------

    @Test
    void countByFormGroupsAndSortsKeys() {
        Map<String, Long> byForm = MedicineQueries.countByForm(medicines);

        assertEquals(List.of("капсули", "краплі", "мазь", "розчин", "сироп", "спрей", "суспензія", "таблетки"),
                new ArrayList<>(byForm.keySet()));
        assertEquals(5L, byForm.get("таблетки"));
        assertEquals(2L, byForm.get("розчин"));
        assertEquals(13L, byForm.values().stream().mapToLong(Long::longValue).sum());
    }

    /* Зірочка: дворівневе групування. */
    @Test
    void countByKindAndFormBuildsNestedMap() {
        Map<MedicineKind, Map<String, Long>> nested = MedicineQueries.countByKindAndForm(medicines);

        assertEquals(List.of(MedicineKind.PRESCRIPTION, MedicineKind.FREE), new ArrayList<>(nested.keySet()));
        assertEquals(Map.of("капсули", 1L, "розчин", 2L, "таблетки", 2L), nested.get(MedicineKind.PRESCRIPTION));
        assertEquals(3L, nested.get(MedicineKind.FREE).get("таблетки"));
    }

    // ---------- 4. statistics ----------

    @Test
    void priceStatisticsSummarizesAllPrices() {
        DoubleSummaryStatistics stats = MedicineQueries.priceStatistics(medicines);

        assertEquals(13, stats.getCount());
        assertEquals(1880.95, stats.getSum(), 1e-6);
        assertEquals(1880.95 / 13, stats.getAverage(), 1e-6);
        assertEquals(39.90, stats.getMin(), DELTA);
        assertEquals(640.00, stats.getMax(), DELTA);
    }

    /* Поведінка порожнього набору: 0 для кількості, суми й середнього, ±нескінченність для меж. */
    @Test
    void priceStatisticsOfEmptyInput() {
        DoubleSummaryStatistics stats = MedicineQueries.priceStatistics(List.of());

        assertEquals(0, stats.getCount());
        assertEquals(0.0, stats.getSum(), DELTA);
        assertEquals(0.0, stats.getAverage(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, stats.getMin(), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, stats.getMax(), DELTA);
    }

    // ---------- 5. top-N ----------

    /* Левомеколь і Назол мають однаковий термін (14) — порядок вирішує другий критерій (назва). */
    @Test
    void topNearestExpiryUsesNameAsTieBreaker() {
        List<Medicine> top = MedicineQueries.topNearestExpiry(medicines, 5);

        assertEquals(List.of("Мелоксикам", "Парацетамол", "Левомеколь", "Назол", "Інсулін"), namesOf(top));
        assertEquals(top.get(2).getDaysToExpire(), top.get(3).getDaysToExpire());
    }

    @Test
    void topByAvailabilitySortsDescendingThenByName() {
        List<Medicine> top = MedicineQueries.topByAvailability(medicines, 3);

        assertEquals(List.of(96, 95, 92), top.stream().map(Medicine::availabilityScore).toList());
        assertEquals(List.of("Парацетамол", "Цетиризин", "Парацетамол"), namesOf(top));
    }

    /* При рівній оцінці назви йдуть за зростанням: reversed() стосується лише першого ключа. */
    @Test
    void topByAvailabilityTieBreakerIsAscending() {
        List<Medicine> top = MedicineQueries.topByAvailability(List.of(
                Medicine.of("Бета", "таблетки", 10, 100, false),
                Medicine.of("Альфа", "таблетки", 10, 100, false)), 2);

        assertEquals(List.of("Альфа", "Бета"), namesOf(top));
    }

    @Test
    void topNHandlesZeroTooLargeAndNegative() {
        assertTrue(MedicineQueries.topNearestExpiry(medicines, 0).isEmpty());
        assertEquals(13, MedicineQueries.topNearestExpiry(medicines, 100).size());
        assertThrows(IllegalArgumentException.class, () -> MedicineQueries.topNearestExpiry(medicines, -1));
    }

    // ---------- Optional ----------

    @Test
    void findByNameIgnoresCaseAndSpaces() {
        Optional<Medicine> found = MedicineQueries.findByName(medicines, "  назол ");

        assertTrue(found.isPresent());
        assertEquals("спрей", found.get().getForm());
    }

    /* Серед дублікатів повертається перший за порядком у файлі. */
    @Test
    void findByNameReturnsFirstMatch() {
        assertEquals(45.50, MedicineQueries.findByName(medicines, "Парацетамол").orElseThrow().getPrice(), DELTA);
    }

    @Test
    void findByNameReturnsEmptyWhenAbsent() {
        assertTrue(MedicineQueries.findByName(medicines, "Аспірин").isEmpty());
        assertTrue(MedicineQueries.findByName(List.of(), "Назол").isEmpty());
        assertThrows(NullPointerException.class, () -> MedicineQueries.findByName(medicines, null));
    }

    // ---------- Порожній вхід і відсутність побічних ефектів ----------

    @Test
    void allQueriesHandleEmptyInput() {
        List<Medicine> empty = List.of();

        assertTrue(MedicineQueries.expiringSoon(empty).isEmpty());
        assertTrue(MedicineQueries.names(empty).isEmpty());
        assertTrue(MedicineQueries.countByForm(empty).isEmpty());
        assertTrue(MedicineQueries.countByKindAndForm(empty).isEmpty());
        assertTrue(MedicineQueries.topNearestExpiry(empty, 5).isEmpty());
    }

    /* Запити не змінюють ні склад, ні порядок вхідної колекції. */
    @Test
    void queriesDoNotModifyInput() {
        List<Medicine> input = new ArrayList<>(medicines);
        List<Medicine> snapshot = List.copyOf(input);

        MedicineQueries.expiringSoon(input);
        MedicineQueries.names(input);
        MedicineQueries.countByForm(input);
        MedicineQueries.priceStatistics(input);
        MedicineQueries.topNearestExpiry(input, 5);
        MedicineQueries.topByAvailability(input, 5);
        MedicineQueries.findByName(input, "Назол");

        assertEquals(snapshot, input);
        assertEquals(namesOf(snapshot), namesOf(input));
    }

    /* Результат — незмінний список (Stream.toList), тож його не можна випадково змінити. */
    @Test
    void resultsAreUnmodifiable() {
        List<Medicine> result = MedicineQueries.expiringSoon(medicines);

        assertThrows(UnsupportedOperationException.class, () -> result.add(medicines.get(0)));
    }
}
