package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Доводить еквівалентність циклової та потокової реалізацій на контрольному наборі.
 *
 * <p>Тут навмисно записано «старий» цикловий варіант кожного запиту — так, як його було б
 * написано без Stream API, — і результат порівнюється з {@link MedicineQueries}.
 */
class LoopEquivalenceTest {
    private static final double DELTA = 1e-9;

    private static List<Medicine> controlSet() throws IOException {
        return RecordProcessor.process(FileReport.readLines(Path.of("data", "pharmacy-full.csv"))).validRecords();
    }

    @Test
    void filterLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        List<Medicine> loop = new ArrayList<>();
        for (Medicine medicine : medicines) {
            if (medicine.getDaysToExpire() <= MedicineQueries.EXPIRY_THRESHOLD_DAYS) {
                loop.add(medicine);
            }
        }

        assertEquals(loop, MedicineQueries.expiringSoon(medicines));
    }

    @Test
    void mapLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        Set<String> loop = new LinkedHashSet<>();
        for (Medicine medicine : medicines) {
            loop.add(medicine.getName());
        }

        assertEquals(new ArrayList<>(loop), MedicineQueries.names(medicines));
    }

    @Test
    void groupingLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        Map<String, Long> loop = new TreeMap<>();
        for (Medicine medicine : medicines) {
            loop.merge(medicine.getForm(), 1L, Long::sum);
        }

        assertEquals(loop, MedicineQueries.countByForm(medicines));
    }

    @Test
    void statisticsLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Medicine medicine : medicines) {
            sum += medicine.getPrice();
            min = Math.min(min, medicine.getPrice());
            max = Math.max(max, medicine.getPrice());
        }
        DoubleSummaryStatistics stream = MedicineQueries.priceStatistics(medicines);

        assertEquals(medicines.size(), stream.getCount());
        // Колектор підсумовує з компенсацією похибки (алгоритм Кехена), тому порівнюємо з допуском.
        assertEquals(sum, stream.getSum(), 1e-9);
        assertEquals(sum / medicines.size(), stream.getAverage(), 1e-9);
        assertEquals(min, stream.getMin(), DELTA);
        assertEquals(max, stream.getMax(), DELTA);
    }

    @Test
    void topNLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        List<Medicine> copy = new ArrayList<>(medicines);
        copy.sort(Comparator.comparingInt(Medicine::getDaysToExpire).thenComparing(Medicine::getName));
        List<Medicine> loop = new ArrayList<>();
        for (int i = 0; i < Math.min(5, copy.size()); i++) {
            loop.add(copy.get(i));
        }

        assertEquals(loop, MedicineQueries.topNearestExpiry(medicines, 5));
    }

    @Test
    void searchLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        Medicine loop = null;
        for (Medicine medicine : medicines) {
            if (medicine.getName().equalsIgnoreCase("інсулін")) {
                loop = medicine;
                break;
            }
        }

        assertEquals(Optional.ofNullable(loop), MedicineQueries.findByName(medicines, "інсулін"));
    }

    /* Статистика звіту, переписана на потоки, дає ті самі показники, що й цикл ЛР1–ЛР3. */
    @Test
    void reportStatisticsLoopEqualsStream() throws IOException {
        List<Medicine> medicines = controlSet();
        double total = 0;
        int minDays = Integer.MAX_VALUE;
        int prescriptions = 0;
        for (Medicine medicine : medicines) {
            total += medicine.getPrice();
            minDays = Math.min(minDays, medicine.getDaysToExpire());
            if (medicine.isPrescription()) {
                prescriptions++;
            }
        }
        PharmacyStatistics stream = new PharmacyStatistics(medicines);

        assertEquals(medicines.size(), stream.validCount());
        assertEquals(total / medicines.size(), stream.averagePrice(), 1e-9);
        assertEquals(minDays, stream.minDaysToExpire());
        assertEquals(prescriptions, stream.prescriptionCount());
    }
}
