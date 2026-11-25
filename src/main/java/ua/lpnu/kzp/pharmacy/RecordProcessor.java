package ua.lpnu.kzp.pharmacy;

import java.util.ArrayList;
import java.util.List;

/** Проходить рядками файла, відбирає коректні записи й накопичує пояснення для хибних. */
public final class RecordProcessor {
    /* Маркер порядку байтів, який Блокнот Windows може додати на початок UTF-8 файла. */
    private static final char BOM = '﻿';

    /* Забороняє створення екземплярів службового класу. */
    private RecordProcessor() {
    }

    /**
     * Обробляє всі рядки. Хибний рядок не зупиняє обробку: його номер і причину додають до помилок.
     *
     * @param lines рядки вхідного файла
     * @return коректні записи та список помилок
     */
    public static ProcessingResult process(List<String> lines) {
        List<String[]> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == 0 && !line.isEmpty() && line.charAt(0) == BOM) {
                line = line.substring(1);
            }
            try {
                valid.add(MedicineLineParser.parse(line));
            } catch (IllegalArgumentException exception) {
                // Користувач бачить нумерацію з 1, як у текстовому редакторі.
                errors.add("Рядок %d: %s".formatted(index + 1, exception.getMessage()));
            }
        }
        return new ProcessingResult(valid, errors);
    }
}
