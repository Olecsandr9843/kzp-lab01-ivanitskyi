package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.lpnu.kzp.storage.CsvExporter;
import ua.lpnu.kzp.storage.CsvImporter;
import ua.lpnu.kzp.storage.DataStorageException;

/** Перевіряє {@link MedicineRecord}, імпорт CSV, помилки з причинами та round-trip. */
class MedicineRecordTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final String HEADER = "придатність,форма,назва,рецепт,ціна\n";

    @TempDir
    Path tempDir;

    private final CsvExporter<MedicineRecord> exporter = new CsvExporter<>(MedicineRecord.class);
    private final CsvImporter<MedicineRecord> importer = MedicineArchive.csvImporter();

    /* Набір для round-trip: звичайні записи й записи з комою, лапками, перенесенням рядка. */
    private static List<MedicineRecord> tricky() {
        return List.of(
                new MedicineRecord("Парацетамол", "таблетки", 45.5, LocalDate.of(2027, 11, 25), MedicineKind.FREE),
                new MedicineRecord("Амоксицилін", "капсули", 128.9, LocalDate.of(2027, 5, 24), MedicineKind.PRESCRIPTION),
                new MedicineRecord("Мазь \"Левомеколь\", 40 г", "мазь", 89.75, LocalDate.of(2026, 12, 9),
                        MedicineKind.FREE),
                new MedicineRecord("Інсулін\nшвидкої дії", "розчин", 640.0, LocalDate.of(2026, 12, 15),
                        MedicineKind.PRESCRIPTION),
                new MedicineRecord("  з пробілами  ", "краплі", 0.0, LocalDate.of(2028, 1, 1), MedicineKind.FREE));
    }

    // ---------- Модель ----------

    /* Заголовок: стовпці за Java-іменами полів expiresOn, form, name, prescription, price. */
    @Test
    void headerFollowsSortedFieldNames() throws DataStorageException {
        assertEquals("придатність,форма,назва,рецепт,ціна", exporter.header());
    }

    @Test
    void constructorValidatesState() {
        LocalDate date = LocalDate.of(2027, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> new MedicineRecord(" ", "мазь", 1, date, MedicineKind.FREE));
        assertThrows(IllegalArgumentException.class, () -> new MedicineRecord("А", "мазь", -1, date, MedicineKind.FREE));
        assertThrows(IllegalArgumentException.class,
                () -> new MedicineRecord("А", "мазь", Double.NaN, date, MedicineKind.FREE));
        assertThrows(NullPointerException.class, () -> new MedicineRecord("А", "мазь", 1, null, MedicineKind.FREE));
        assertThrows(NullPointerException.class, () -> new MedicineRecord("А", "мазь", 1, date, null));
    }

    @Test
    void keyIsNameAndForm() {
        assertEquals("Парацетамол / таблетки", tricky().get(0).key());
    }

    @Test
    void convertsFromAndToMedicineRelativeToDate() {
        Medicine medicine = Medicine.of("Еналаприл", "таблетки", 73.6, 45, true);

        MedicineRecord record = MedicineRecord.from(medicine, TODAY);
        Medicine restored = record.toMedicine(TODAY);

        assertEquals(LocalDate.of(2027, 1, 9), record.expiresOn());
        assertEquals(MedicineKind.PRESCRIPTION, record.prescription());
        assertEquals(medicine, restored);
        assertEquals(45, restored.getDaysToExpire());
        assertEquals(73.6, restored.getPrice(), 1e-9);
        assertInstanceOf(PrescriptionMedicine.class, restored);
    }

    /* Прострочений на опорну дату запис не можна перетворити на препарат моделі. */
    @Test
    void expiredRecordCannotBecomeMedicine() {
        MedicineRecord record = new MedicineRecord("А", "мазь", 1, TODAY.minusDays(1), MedicineKind.FREE);

        assertThrows(IllegalArgumentException.class, () -> record.toMedicine(TODAY));
    }

    // ---------- Round-trip ----------

    @Test
    void roundTripRestoresEqualRecords() throws DataStorageException {
        Path file = tempDir.resolve("medicines.csv");

        exporter.write(file, tricky());
        List<MedicineRecord> restored = importer.read(file);

        assertEquals(tricky(), restored);
    }

    /* Повний шлях: препарати моделі → CSV → препарати моделі (той самий підтип, ціна й термін). */
    @Test
    void roundTripOfModelMedicines() throws DataStorageException {
        List<Medicine> medicines = List.of(
                Medicine.of("Парацетамол", "таблетки", 45.5, 365, false),
                Medicine.of("Мелоксикам", "розчин", 155.0, 0, true));
        Path file = tempDir.resolve("model.csv");

        exporter.write(file, MedicineArchive.toRecords(medicines, TODAY));
        List<Medicine> restored = importer.read(file).stream().map(record -> record.toMedicine(TODAY)).toList();

        assertEquals(medicines, restored);
        assertEquals(List.of(365, 0), restored.stream().map(Medicine::getDaysToExpire).toList());
        assertEquals(List.of(45.5, 155.0), restored.stream().map(Medicine::getPrice).toList());
    }

    @Test
    void readsCommittedSampleFile() throws DataStorageException {
        List<MedicineRecord> sample = importer.read(Path.of("data", "medicines-sample.csv"));

        assertEquals(6, sample.size());
        assertEquals("Мазь \"Левомеколь\", 40 г", sample.get(2).name());
        assertEquals("Інсулін\nшвидкої дії", sample.get(3).name());
    }

    // ---------- Порожні дані ----------

    @Test
    void headerOnlyFileGivesEmptyList() throws DataStorageException {
        assertTrue(importer.read(Path.of("src", "test", "resources", "medicines-empty.csv")).isEmpty());
        assertEquals(HEADER.replace("\n", System.lineSeparator()), exporter.export(List.of()));
    }

    @Test
    void completelyEmptyFileIsError() {
        DataStorageException error = assertThrows(DataStorageException.class, () -> importer.parse(""));

        assertTrue(error.getMessage().contains("немає рядка заголовка"), error.getMessage());
    }

    // ---------- Помилки та getCause() ----------

    @Test
    void wrongFieldCountReportsLine() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> importer.parse(HEADER + "2027-01-01,мазь,А,FREE\n"));

        assertEquals("рядок 2: очікується 5 полів, отримано 4", error.getMessage());
    }

    @Test
    void badNumberKeepsNumberFormatCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> importer.parse(HEADER + "2027-01-01,мазь,А,FREE,дорого\n"));

        assertTrue(error.getMessage().startsWith("рядок 2: ціна має бути числом"), error.getMessage());
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
        assertInstanceOf(NumberFormatException.class, error.getCause().getCause());
    }

    /* Хибний enum у файлі medicines-invalid.csv (рядок 3). */
    @Test
    void badEnumKeepsCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> importer.read(Path.of("src", "test", "resources", "medicines-invalid.csv")));

        assertTrue(error.getMessage().startsWith("рядок 3: рецепт має бути PRESCRIPTION або FREE"), error.getMessage());
        assertInstanceOf(IllegalArgumentException.class, error.getCause().getCause());
    }

    @Test
    void badDateKeepsDateTimeParseCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> importer.parse(HEADER + "25.11.2027,мазь,А,FREE,1.0\n"));

        assertInstanceOf(DateTimeParseException.class, error.getCause().getCause());
    }

    @Test
    void wrongHeaderIsRejected() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> importer.parse("name,form,price,expiresOn,prescription\n"));

        assertTrue(error.getMessage().startsWith("очікується заголовок"), error.getMessage());
    }

    @Test
    void missingFileKeepsIoCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> importer.read(tempDir.resolve("missing.csv")));

        assertInstanceOf(IOException.class, error.getCause());
        assertInstanceOf(NoSuchFileException.class, error.getCause());
    }

    @Test
    void fromCsvRequiresAllColumns() {
        assertThrows(IllegalArgumentException.class, () -> MedicineRecord.fromCsv(Map.of(MedicineRecord.NAME, "А")));
    }
}
