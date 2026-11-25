package ua.lpnu.kzp.pharmacy;

import java.util.List;
import java.util.Locale;

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
}
