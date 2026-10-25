package app;

import exceptions.FigureFormatException;
import exceptions.FigureStorageException;
import exceptions.InvalidFigureParameterException;
import geometry2d.Circle;
import geometry2d.Figure;
import geometry2d.Rectangle;
import geometry3d.Cylinder;
import storage.BinaryFigureStorage;
import storage.FigureStorage;
import storage.TextFigureStorage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Воспроизводимые проверки из чек-листа приёмки.
 * Для каждого случая печатается ожидаемый и фактический результат и OK/FAIL.
 *
 * Запуск: java -cp out app.Checks
 */
public class Checks {

    private static final double EPS = 1e-9;
    private static int passed = 0;
    private static int failed = 0;

    private static final FigureStorage TEXT = new TextFigureStorage();
    private static final FigureStorage BINARY = new BinaryFigureStorage();

    @FunctionalInterface
    interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) throws IOException {
        Path tmp = Files.createTempDirectory("lab3-checks");
        try {
            section("1. Площади, периметры, объёмы");
            checkCalculations();

            section("2. Проверка параметров в конструкторах");
            checkValidation();

            section("3. Работа finally");
            checkFinally();

            section("4. Текстовый формат: корректные и ошибочные файлы");
            checkTextFormat(tmp);

            section("5. Пустой файл / пустой список");
            checkEmpty(tmp);

            section("6. Файловые ошибки");
            checkFileErrors(tmp);

            section("7. Совпадение после записи и чтения (оба формата)");
            checkRoundTrip(tmp);

            section("8. Повреждённый и неподходящий бинарный файл");
            checkBadBinary(tmp);
        } finally {
            deleteRecursively(tmp);
        }

        System.out.println();
        System.out.println("ИТОГО: пройдено " + passed + ", провалено " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- проверки

    private static void checkCalculations() {
        Circle c = new Circle(2);
        approx("Circle r=2, площадь = 4π", 4 * Math.PI, c.area());
        approx("Circle r=2, периметр = 4π", 4 * Math.PI, c.perimeter());

        Rectangle r = new Rectangle(3, 4);
        approx("Rectangle 3×4, площадь", 12, r.area());
        approx("Rectangle 3×4, периметр", 14, r.perimeter());

        approx("Cylinder(Circle r=2, h=5), объём = 20π", 20 * Math.PI, new Cylinder(c, 5).volume());
        approx("Cylinder(Rectangle 3×4, h=5), объём", 60, new Cylinder(r, 5).volume());

        expectEquals("toString круга", "Circle{radius=2.0}", c.toString());
        expectEquals("toString прямоугольника", "Rectangle{width=3.0, height=4.0}", r.toString());
    }

    private static void checkValidation() {
        double[] bad = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double v : bad) {
            expectThrows("new Circle(" + v + ")", InvalidFigureParameterException.class, () -> new Circle(v));
            expectThrows("new Rectangle(1, " + v + ")", InvalidFigureParameterException.class,
                    () -> new Rectangle(1, v));
            expectThrows("new Cylinder(круг, " + v + ")", InvalidFigureParameterException.class,
                    () -> new Cylinder(new Circle(1), v));
        }
        expectThrows("new Cylinder(null, 1)", InvalidFigureParameterException.class, () -> new Cylinder(null, 1));
    }

    private static void checkFinally() {
        for (double radius : new double[]{1, -1}) {
            StringBuilder log = new StringBuilder();
            try {
                log.append("try;");
                new Circle(radius);
                log.append("ok;");
            } catch (InvalidFigureParameterException e) {
                log.append("catch;");
            } finally {
                log.append("finally");
            }
            String expected = radius > 0 ? "try;ok;finally" : "try;catch;finally";
            expectEquals("finally при radius=" + radius + (radius > 0 ? " (успех)" : " (исключение)"),
                    expected, log.toString());
        }
    }

    private static void checkTextFormat(Path tmp) throws IOException {
        Path good = write(tmp, "good.txt", """
                # комментарий
                CIRCLE;1.5
                rectangle ; 2 ; 3

                Circle;10
                """);
        expectNoThrow("Чтение кругов и прямоугольников", () -> {
            List<Figure> list = TEXT.read(good);
            expectEquals("  количество", 3, list.size());
            expectEquals("  порядок и параметры",
                    "[Circle{radius=1.5}, Rectangle{width=2.0, height=3.0}, Circle{radius=10.0}]",
                    list.toString());
        });

        expectFormatError("Неизвестный тип фигуры",
                write(tmp, "unknown.txt", "CIRCLE;1\nTRIANGLE;1;2;3\n"), 2, null);
        expectFormatError("Неверное кол-во параметров (круг)",
                write(tmp, "count1.txt", "CIRCLE;1;2\n"), 1, null);
        expectFormatError("Неверное кол-во параметров (прямоугольник)",
                write(tmp, "count2.txt", "CIRCLE;1\n\nRECTANGLE;5\n"), 3, null);
        expectFormatError("Ошибка преобразования числа",
                write(tmp, "nan.txt", "RECTANGLE;2;abc\n"), 1, NumberFormatException.class);
        expectFormatError("Некорректное значение (отрицательный радиус)",
                write(tmp, "neg.txt", "CIRCLE;5\nCIRCLE;-3\n"), 2, InvalidFigureParameterException.class);
        expectFormatError("NaN в файле",
                write(tmp, "nan2.txt", "CIRCLE;NaN\n"), 1, InvalidFigureParameterException.class);
    }

    private static void checkEmpty(Path tmp) throws IOException {
        Path empty = write(tmp, "empty.txt", "");
        expectNoThrow("Пустой текстовый файл → пустой список",
                () -> expectEquals("  размер", 0, TEXT.read(empty).size()));

        for (FigureStorage s : List.of(TEXT, BINARY)) {
            String name = s.getClass().getSimpleName();
            Path p = tmp.resolve("empty-" + name);
            expectNoThrow(name + ": запись и чтение пустого списка", () -> {
                s.write(p, List.of());
                expectEquals("  размер после чтения", 0, s.read(p).size());
            });
        }
    }

    private static void checkFileErrors(Path tmp) {
        Path missing = tmp.resolve("no-such-file.txt");
        expectThrows("Текст: чтение отсутствующего файла", NoSuchFileException.class, () -> TEXT.read(missing));
        expectThrows("Бинарный: чтение отсутствующего файла", NoSuchFileException.class, () -> BINARY.read(missing));

        List<Figure> one = List.of(new Circle(1));
        expectThrows("Текст: запись по пути каталога", IOException.class, () -> TEXT.write(tmp, one));
        expectThrows("Бинарный: запись по пути каталога", IOException.class, () -> BINARY.write(tmp, one));
    }

    private static void checkRoundTrip(Path tmp) {
        List<Figure> original = List.of(
                new Circle(1.5), new Rectangle(2, 3), new Circle(0.1),
                new Rectangle(1e-3, 12345.678), new Circle(Math.PI));

        for (FigureStorage s : List.of(TEXT, BINARY)) {
            String name = s.getClass().getSimpleName();
            Path p = tmp.resolve("roundtrip-" + name);
            expectNoThrow(name + ": запись → чтение", () -> {
                s.write(p, original);
                List<Figure> restored = s.read(p);
                expectEquals("  размер", original.size(), restored.size());
                for (int i = 0; i < original.size(); i++) {
                    Figure a = original.get(i);
                    Figure b = restored.get(i);
                    boolean same = a.getClass() == b.getClass()
                            && a.toString().equals(b.toString())
                            && a.area() == b.area()
                            && a.perimeter() == b.perimeter();
                    report("  [" + i + "] " + a + " ↔ " + b, "совпадают тип, параметры, S и P",
                            same ? "совпадают" : "РАЗЛИЧАЮТСЯ", same);
                }
            });
        }
    }

    private static void checkBadBinary(Path tmp) throws IOException {
        Path garbage = tmp.resolve("garbage.bin");
        Files.write(garbage, "это не сериализованный объект".getBytes(StandardCharsets.UTF_8));
        expectThrows("Мусор вместо сериализации", FigureStorageException.class, () -> BINARY.read(garbage));

        Path truncated = tmp.resolve("truncated.bin");
        try {
            BINARY.write(truncated, List.of(new Circle(1), new Rectangle(2, 3)));
        } catch (FigureStorageException e) {
            throw new IllegalStateException(e);
        }
        byte[] full = Files.readAllBytes(truncated);
        Files.write(truncated, java.util.Arrays.copyOf(full, full.length / 2));
        expectThrows("Обрезанный бинарный файл", FigureStorageException.class, () -> BINARY.read(truncated));

        Path string = tmp.resolve("string.bin");
        serialize(string, "просто строка");
        expectThrows("Сериализован String вместо списка", FigureStorageException.class, () -> BINARY.read(string));

        Path wrongList = tmp.resolve("strings.bin");
        serialize(wrongList, new ArrayList<>(List.of("a", "b")));
        expectThrows("Сериализован List<String>", FigureStorageException.class, () -> BINARY.read(wrongList));

        Path textAsBinary = tmp.resolve("text-as-binary.txt");
        Files.writeString(textAsBinary, "CIRCLE;1\n", StandardCharsets.UTF_8);
        expectThrows("Текстовый файл, прочитанный как бинарный", FigureStorageException.class,
                () -> BINARY.read(textAsBinary));
    }

    // ---------------------------------------------------------------- утилиты

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void report(String name, Object expected, Object actual, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "[OK]   " : "[FAIL] ") + name);
        System.out.println("         ожидалось: " + expected);
        System.out.println("         получено:  " + actual);
    }

    private static void approx(String name, double expected, double actual) {
        report(name, expected, actual, Math.abs(expected - actual) < EPS);
    }

    private static void expectEquals(String name, Object expected, Object actual) {
        report(name, expected, actual, expected.equals(actual));
    }

    private static void expectThrows(String name, Class<? extends Throwable> type, Action action) {
        try {
            action.run();
            report(name, type.getSimpleName(), "исключения не было", false);
        } catch (Throwable t) {
            report(name, type.getSimpleName(), t.getClass().getSimpleName() + ": " + t.getMessage(),
                    type.isInstance(t));
        }
    }

    private static void expectNoThrow(String name, Action action) {
        try {
            action.run();
            report(name, "без исключений", "без исключений", true);
        } catch (Throwable t) {
            report(name, "без исключений", t, false);
        }
    }

    private static void expectFormatError(String name, Path file, int line, Class<? extends Throwable> cause) {
        try {
            List<Figure> list = TEXT.read(file);
            report(name, "FigureFormatException", "прочитано " + list.size() + " фигур", false);
        } catch (FigureFormatException e) {
            boolean lineOk = e.getLineNumber() == line;
            boolean causeOk = cause == null || cause.isInstance(e.getCause());
            String expected = "FigureFormatException, строка " + line
                    + (cause != null ? ", причина " + cause.getSimpleName() : "");
            String actual = e.getMessage()
                    + (e.getCause() != null ? " | причина: " + e.getCause().getClass().getSimpleName() : "");
            report(name, expected, actual, lineOk && causeOk);
        } catch (Exception e) {
            report(name, "FigureFormatException", e, false);
        }
    }

    private static Path write(Path dir, String name, String content) throws IOException {
        return Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static void serialize(Path path, Object obj) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(obj);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
