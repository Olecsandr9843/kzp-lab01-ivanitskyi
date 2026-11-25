package ua.lpnu.kzp.pharmacy;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Формує текст звіту, однаковий для консолі та файла. */
public final class ReportFormatter {
    /* Забороняє створення екземплярів службового класу. */
    private ReportFormatter() {
    }

    /**
     * Будує звіт. Числа форматуються з {@link Locale#ROOT}, тому дробова частина завжди
     * відокремлюється крапкою, незалежно від мовних налаштувань ОС. Рядки завершуються {@code %n},
     * тобто роздільником поточної платформи.
     *
     * @param source     назва або шлях вхідного файла для заголовка звіту
     * @param statistics обчислені показники
     * @param errors     повідомлення про пропущені рядки
     * @return готовий багаторядковий текст звіту
     */
    public static String format(String source, PharmacyStatistics statistics, List<String> errors) {
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Звіт аптеки (варіант 11)%n"));
        report.append(String.format(Locale.ROOT, "Вхідний файл: %s%n", source));
        report.append(String.format(Locale.ROOT, "Коректних записів: %d%n", statistics.validCount()));
        report.append(String.format(Locale.ROOT, "Середня ціна: %.2f грн%n", statistics.averagePrice()));
        if (statistics.isEmpty()) {
            report.append(String.format(Locale.ROOT, "Найменший термін придатності: немає даних%n"));
        } else {
            report.append(String.format(Locale.ROOT, "Найменший термін придатності: %d дн.%n",
                    statistics.minDaysToExpire()));
        }
        report.append(String.format(Locale.ROOT, "Рецептурних препаратів: %d%n", statistics.prescriptionCount()));
        report.append(String.format(Locale.ROOT, "Пропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(String.format(Locale.ROOT, "  %s%n", error));
        }
        return report.toString();
    }

    /**
     * Будує розширений розділ звіту (параметр {@code --details}): кожен препарат із категорією
     * та оцінкою доступності, кількість унікальних препаратів і середня оцінка.
     *
     * <p>Метод працює лише з базовим типом {@link Medicine}: оцінку рахує поліморфний виклик
     * {@link Medicine#availabilityScore()}, підпис дає {@link MedicineKind#label()}. Новий
     * підтип препарату не потребуватиме змін у цьому коді.
     *
     * @param medicines коректні препарати будь-яких підтипів
     * @return текст розділу, кожен рядок завершується {@code %n}
     */
    public static String formatDetails(List<Medicine> medicines) {
        StringBuilder details = new StringBuilder();
        details.append(String.format(Locale.ROOT, "Асортимент:%n"));
        int totalScore = 0;
        for (Medicine medicine : medicines) {
            int score = medicine.availabilityScore();
            totalScore += score;
            details.append(String.format(Locale.ROOT, "  %s — доступність %d/%d%n",
                    medicine, score, Medicine.MAX_SCORE));
        }
        // HashSet покладається на equals/hashCode: однаковий препарат у кількох рядках рахується один раз.
        Set<Medicine> unique = new HashSet<>(medicines);
        double averageScore = medicines.isEmpty() ? 0.0 : (double) totalScore / medicines.size();
        details.append(String.format(Locale.ROOT, "Унікальних препаратів: %d%n", unique.size()));
        details.append(String.format(Locale.ROOT, "Середня оцінка доступності: %.2f%n", averageScore));
        return details.toString();
    }
}
