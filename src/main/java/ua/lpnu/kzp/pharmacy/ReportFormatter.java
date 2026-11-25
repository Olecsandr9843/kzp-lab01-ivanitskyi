package ua.lpnu.kzp.pharmacy;

import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        // ЛР4: цикл замінено двома конвеєрами — рядки переліку і сума оцінок.
        String lines = medicines.stream()
                .map(medicine -> line("  %s — доступність %d/%d", medicine, medicine.availabilityScore(),
                        Medicine.MAX_SCORE))
                .collect(Collectors.joining());
        // HashSet покладається на equals/hashCode: однаковий препарат у кількох рядках рахується один раз.
        int unique = new HashSet<>(medicines).size();
        double averageScore = medicines.stream()
                .mapToInt(Medicine::availabilityScore)
                .average()
                .orElse(0.0);
        return line("Асортимент:")
                + lines
                + line("Унікальних препаратів: %d", unique)
                + line("Середня оцінка доступності: %.2f", averageScore);
    }

    /**
     * Будує розділ із результатами п'яти запитів Stream API (параметр {@code --queries}).
     *
     * <p>Обчислення виконує {@link MedicineQueries}; тут лише перетворення результатів на текст.
     *
     * @param medicines коректні препарати
     * @param topN      скільки елементів показувати в рейтингах
     * @return текст розділу, кожен рядок завершується {@code %n}
     */
    public static String formatQueries(List<Medicine> medicines, int topN) {
        List<Medicine> expiring = MedicineQueries.expiringSoon(medicines);
        DoubleSummaryStatistics prices = MedicineQueries.priceStatistics(medicines);
        StringBuilder text = new StringBuilder(line("Запити Stream API:"));
        text.append(line("1. Термін придатності ≤ %d дн.: %d", MedicineQueries.EXPIRY_THRESHOLD_DAYS,
                expiring.size()));
        text.append(numbered(expiring));
        text.append(line("2. Назви: %s", String.join(", ", MedicineQueries.names(medicines))));
        text.append(line("3. Кількість за формою: %s", joinMap(MedicineQueries.countByForm(medicines))));
        MedicineQueries.countByKindAndForm(medicines).forEach((kind, byForm) ->
                text.append(line("   %s: %s", kind.label(), joinMap(byForm))));
        if (prices.getCount() == 0) {
            text.append(line("4. Статистика цін: немає даних"));
        } else {
            text.append(line("4. Статистика цін: кількість %d, сума %.2f, середня %.2f, мін %.2f, макс %.2f грн",
                    prices.getCount(), prices.getSum(), prices.getAverage(), prices.getMin(), prices.getMax()));
        }
        text.append(line("5. Топ-%d за найближчим терміном придатності:", topN));
        text.append(numbered(MedicineQueries.topNearestExpiry(medicines, topN)));
        text.append(line("   Топ-%d за доступністю:", topN));
        text.append(numbered(MedicineQueries.topByAvailability(medicines, topN)));
        return text.toString();
    }

    /**
     * Форматує результат пошуку за назвою (параметр {@code --find}).
     *
     * @param name   назва, яку шукали
     * @param result результат {@link MedicineQueries#findByName(List, String)}
     * @return один рядок: знайдений препарат або повідомлення про відсутність
     */
    public static String formatSearch(String name, Optional<Medicine> result) {
        return result
                .map(medicine -> line("Пошук «%s»: %s", name.strip(), medicine))
                .orElseGet(() -> line("Пошук «%s»: не знайдено", name.strip()));
    }

    /* Нумерований перелік препаратів із відступом; порожній рядок, якщо список порожній. */
    private static String numbered(List<Medicine> medicines) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < medicines.size(); i++) {
            text.append(line("   %d) %s", i + 1, medicines.get(i)));
        }
        return text.toString();
    }

    /* «ключ=значення, …» у порядку ітерації карти. */
    private static String joinMap(Map<String, Long> map) {
        if (map.isEmpty()) {
            return "немає даних";
        }
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /* Один рядок звіту з Locale.ROOT і платформним роздільником рядків. */
    private static String line(String format, Object... args) {
        return String.format(Locale.ROOT, format + "%n", args);
    }
}
