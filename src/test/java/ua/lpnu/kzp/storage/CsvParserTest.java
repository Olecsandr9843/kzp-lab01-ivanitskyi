package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Перевіряє документний CSV-парсер. */
class CsvParserTest {

    private static List<List<String>> fields(String text) throws DataStorageException {
        return CsvParser.parse(text).stream().map(CsvParser.Row::fields).toList();
    }

    @Test
    void parsesSimpleRecords() throws DataStorageException {
        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), fields("a,b\n1,2\n"));
    }

    /* Кома в лапках — частина значення; простий split(",") розбив би її на два поля. */
    @Test
    void commaInsideQuotesBelongsToValue() throws DataStorageException {
        assertEquals(List.of(List.of("Мазь, 40 г", "1")), fields("\"Мазь, 40 г\",1"));
        assertEquals(3, "\"Мазь, 40 г\",1".split(",").length);
    }

    @Test
    void doubledQuotesBecomeOneQuote() throws DataStorageException {
        assertEquals(List.of(List.of("Мазь \"Левомеколь\"")), fields("\"Мазь \"\"Левомеколь\"\"\""));
    }

    /* Перенесення рядка в лапках не завершує запис; номер наступного запису враховує його. */
    @Test
    void newlineInsideQuotesDoesNotEndRecord() throws DataStorageException {
        List<CsvParser.Row> rows = CsvParser.parse("h1,h2\n\"Інсулін\nшвидкої дії\",x\nА,Б\n");

        assertEquals(3, rows.size());
        assertEquals(List.of("Інсулін\nшвидкої дії", "x"), rows.get(1).fields());
        assertEquals(2, rows.get(1).line());
        assertEquals(4, rows.get(2).line());
    }

    /* Файли з Windows (CRLF) і Unix (LF) дають однаковий результат. */
    @Test
    void crlfAndLfAreEquivalent() throws DataStorageException {
        assertEquals(fields("a,b\n1,2\n"), fields("a,b\r\n1,2\r\n"));
        assertEquals(List.of(List.of("x\r\ny")), fields("\"x\r\ny\"\r\n"));
    }

    @Test
    void emptyFieldsAndMissingFinalNewline() throws DataStorageException {
        assertEquals(List.of(List.of("", "b", "")), fields(",b,"));
        assertEquals(List.of(List.of("", "")), fields("\"\",\"\""));
    }

    @Test
    void blankLinesAndBomAreIgnored() throws DataStorageException {
        assertEquals(List.of(List.of("a"), List.of("b")), fields("﻿a\n\n\nb\n"));
        assertTrue(fields("").isEmpty());
        assertTrue(fields("\n\n").isEmpty());
    }

    @Test
    void unclosedQuoteIsError() {
        DataStorageException error = assertThrows(DataStorageException.class, () -> CsvParser.parse("a\n\"незакрите,1\n"));

        assertEquals("рядок 2: незакриті лапки", error.getMessage());
    }

    @Test
    void characterAfterClosingQuoteIsError() {
        assertThrows(DataStorageException.class, () -> CsvParser.parse("\"abc\"x,1"));
    }

    @Test
    void quoteInsideUnquotedFieldIsError() {
        assertThrows(DataStorageException.class, () -> CsvParser.parse("ab\"c,1"));
    }
}
