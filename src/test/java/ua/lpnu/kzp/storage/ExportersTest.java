package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Перевіряє анотацію та універсальність експортерів на класах, не пов'язаних з аптекою.
 */
class ExportersTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path tempDir;

    /** Звичайний (не record) клас з анотованими, неанотованим і статичним полями. */
    static final class Book {
        @CsvColumn("year")
        private final int year;
        @CsvColumn("title")
        private final String title;
        @CsvColumn("author")
        private final String author;
        /* Без анотації — не експортується. */
        private final String internalNote = "приховано";
        /* Статичне поле не належить об'єкту — ігнорується навіть з анотацією. */
        @CsvColumn("static")
        private static final String CONSTANT = "x";

        Book(String title, String author, int year) {
            this.title = title;
            this.author = author;
            this.year = year;
        }

        @Override
        public String toString() {
            return internalNote + CONSTANT;
        }
    }

    /** Record з іншим набором типів. */
    record Point(@CsvColumn("x") double x, @CsvColumn("label") String label) {
    }

    /** Клас без анотацій. */
    record Plain(String value) {
    }

    // ---------- Анотація ----------

    @Test
    void annotationIsRuntimeAndFieldOnly() {
        assertEquals(RetentionPolicy.RUNTIME, CsvColumn.class.getAnnotation(Retention.class).value());
        assertEquals(List.of(ElementType.FIELD), List.of(CsvColumn.class.getAnnotation(Target.class).value()));
    }

    // ---------- CSV ----------

    /* Порядок стовпців — за Java-іменами полів (author, title, year), а не за оголошенням. */
    @Test
    void csvHeaderIsSortedByFieldName() throws DataStorageException {
        assertEquals("author,title,year", new CsvExporter<>(Book.class).header());
        assertEquals("label,x", new CsvExporter<>(Point.class).header());
    }

    @Test
    void csvExportsAnyAnnotatedClass() throws DataStorageException {
        String csv = new CsvExporter<>(Book.class).export(List.of(
                new Book("Кобзар", "Шевченко", 1840),
                new Book("Лісова пісня", "Українка", 1911)));

        assertEquals("author,title,year" + NL
                + "Шевченко,Кобзар,1840" + NL
                + "Українка,Лісова пісня,1911" + NL, csv);
    }

    @Test
    void csvEscapesCommasQuotesAndNewlines() {
        assertEquals("просто", CsvExporter.escape("просто"));
        assertEquals("\"а, б\"", CsvExporter.escape("а, б"));
        assertEquals("\"Мазь \"\"Левомеколь\"\"\"", CsvExporter.escape("Мазь \"Левомеколь\""));
        assertEquals("\"рядок1\nрядок2\"", CsvExporter.escape("рядок1\nрядок2"));
        assertEquals("\"a\rb\"", CsvExporter.escape("a\rb"));
    }

    @Test
    void csvWritesNullAsEmptyField() throws DataStorageException {
        String csv = new CsvExporter<>(Point.class).export(List.of(new Point(1.5, null)));

        assertEquals("label,x" + NL + ",1.5" + NL, csv);
    }

    /* Порожній набір — лише заголовок. */
    @Test
    void csvExportOfEmptyListIsHeaderOnly() throws DataStorageException {
        assertEquals("author,title,year" + NL, new CsvExporter<>(Book.class).export(List.of()));
    }

    @Test
    void classWithoutAnnotationsIsRejected() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> new CsvExporter<>(Plain.class).export(List.of(new Plain("a"))));

        assertTrue(error.getMessage().contains("@CsvColumn"), error.getMessage());
    }

    /* Запис через Path + Files у UTF-8 зберігає кирилицю й створює каталог. */
    @Test
    void writeUsesUtf8AndCreatesDirectories() throws DataStorageException, IOException {
        Path file = tempDir.resolve("nested").resolve("books.csv");

        new CsvExporter<>(Book.class).write(file, List.of(new Book("Кобзар", "Шевченко", 1840)));

        assertEquals("author,title,year" + NL + "Шевченко,Кобзар,1840" + NL,
                Files.readString(file, StandardCharsets.UTF_8));
    }

    /* Файлова помилка: шлях указує на каталог. Причина — IOException. */
    @Test
    void writeErrorKeepsIoCause() {
        DataStorageException error = assertThrows(DataStorageException.class,
                () -> new CsvExporter<>(Book.class).write(tempDir, List.of()));

        assertInstanceOf(IOException.class, error.getCause());
    }

    // ---------- Імпорт довільного класу ----------

    @Test
    void importerWorksWithAnyClass() throws DataStorageException {
        CsvImporter<Point> importer = new CsvImporter<>(Point.class,
                (Map<String, String> row) -> new Point(Double.parseDouble(row.get("x")), row.get("label")));

        List<Point> points = importer.parse("label,x\n\"A, B\",2.5\n");

        assertEquals(List.of(new Point(2.5, "A, B")), points);
    }

    // ---------- JSON (зірочка) ----------

    @Test
    void jsonUsesSameAnnotatedFields() throws DataStorageException {
        String json = new JsonExporter<>(Book.class).export(List.of(new Book("Кобзар", "Шевченко", 1840)));

        assertEquals("[" + NL + "  {\"author\": \"Шевченко\", \"title\": \"Кобзар\", \"year\": 1840}" + NL + "]" + NL,
                json);
    }

    @Test
    void jsonEscapesSpecialCharactersAndNull() throws DataStorageException {
        String json = new JsonExporter<>(Point.class).export(List.of(new Point(1.0, "a\"b\\c\nd"), new Point(2.0, null)));

        assertTrue(json.contains("{\"label\": \"a\\\"b\\\\c\\nd\", \"x\": 1.0}"), json);
        assertTrue(json.contains("{\"label\": null, \"x\": 2.0}"), json);
        assertEquals("\"\\u0001\"", JsonExporter.quote("\u0001"));
    }

    @Test
    void jsonOfEmptyListIsEmptyArray() throws DataStorageException {
        assertEquals("[]" + NL, new JsonExporter<>(Point.class).export(List.of()));
    }

    /* Обидва формати — через спільний інтерфейс. */
    @Test
    void exportersShareInterface() throws DataStorageException {
        List<Exporter<Point>> exporters = List.of(new CsvExporter<>(Point.class), new JsonExporter<>(Point.class));

        for (Exporter<Point> exporter : exporters) {
            assertTrue(exporter.export(List.of(new Point(3.0, "z"))).contains("3.0"));
        }
    }

    /* Кеш повертає той самий список для класу; сканування без кешу дає рівний за змістом. */
    @Test
    void annotatedFieldsAreCachedPerClass() throws DataStorageException {
        assertTrue(AnnotatedFields.of(Book.class) == AnnotatedFields.of(Book.class));
        assertEquals(AnnotatedFields.of(Book.class), AnnotatedFields.scan(Book.class));
    }
}
