package ua.lpnu.kzp.pharmacy;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import ua.lpnu.kzp.storage.CsvExporter;
import ua.lpnu.kzp.storage.CsvImporter;
import ua.lpnu.kzp.storage.DataStorageException;
import ua.lpnu.kzp.storage.Exporter;
import ua.lpnu.kzp.storage.JsonExporter;

/**
 * Збереження асортименту аптеки у файл і відновлення з нього (параметр {@code --export}).
 *
 * <p>Тут поєднуються предметний {@link MedicineRecord} та універсальні
 * {@link CsvExporter}/{@link CsvImporter}/{@link JsonExporter} — самі вони про аптеку
 * нічого не знають.
 */
public final class MedicineArchive {
    /* Забороняє створення екземплярів службового класу. */
    private MedicineArchive() {
    }

    /**
     * Створює імпортер CSV для {@link MedicineRecord}.
     *
     * @return імпортер, що відновлює записи через {@link MedicineRecord#fromCsv}
     */
    public static CsvImporter<MedicineRecord> csvImporter() {
        return new CsvImporter<>(MedicineRecord.class, MedicineRecord::fromCsv);
    }

    /**
     * Перетворює препарати на записи відносно опорної дати.
     *
     * @param medicines препарати
     * @param today     опорна дата для обчислення {@code expiresOn}
     * @return незмінний список записів у тому самому порядку
     */
    public static List<MedicineRecord> toRecords(List<Medicine> medicines, LocalDate today) {
        return medicines.stream().map(medicine -> MedicineRecord.from(medicine, today)).toList();
    }

    /**
     * Експортує препарати у файл. Формат визначає розширення: {@code .json} — JSON, інше — CSV.
     * Для CSV файл одразу читається назад, і відновлені записи порівнюються з початковими.
     *
     * @param medicines препарати
     * @param path      файл призначення
     * @param today     опорна дата
     * @return повідомлення для консолі про результат експорту й перевірки round-trip
     * @throws DataStorageException якщо запис, читання або розбір файла неможливі, або
     *                              відновлені записи відрізняються від початкових
     */
    public static String export(List<Medicine> medicines, Path path, LocalDate today) throws DataStorageException {
        List<MedicineRecord> records = toRecords(medicines, today);
        // getFileName() може повернути null (корінь файлової системи), тому викликаємо його один раз.
        Path fileName = path.getFileName();
        boolean json = fileName != null && fileName.toString().toLowerCase(Locale.ROOT).endsWith(".json");
        Exporter<MedicineRecord> exporter = json
                ? new JsonExporter<>(MedicineRecord.class)
                : new CsvExporter<>(MedicineRecord.class);
        exporter.write(path, records);
        if (json) {
            return "Експортовано %d записів у JSON: %s".formatted(records.size(), path);
        }
        List<MedicineRecord> restored = csvImporter().read(path);
        if (!restored.equals(records)) {
            throw new DataStorageException("round-trip не збігся: записано %d, відновлено %d"
                    .formatted(records.size(), restored.size()));
        }
        return "Експортовано %d записів у CSV: %s; round-trip: відновлено %d, усі рівні початковим"
                .formatted(records.size(), path, restored.size());
    }
}
