package app;

import exceptions.FigureFormatException;
import exceptions.FigureStorageException;
import exceptions.InvalidFigureParameterException;
import geometry2d.Figure;
import geometry3d.Cylinder;
import storage.BinaryFigureStorage;
import storage.FigureStorage;
import storage.TextFigureStorage;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Сценарий приложения.
 *
 * Запуск: java -cp out app.Main [входной.txt] [выходной.bin] [высота]
 * Отсутствующие аргументы запрашиваются у пользователя.
 */
public class Main {

    private static final double DEFAULT_HEIGHT = 10.0;

    public static void main(String[] args) {
        Scanner console = new Scanner(System.in);

        Path input;
        Path output;
        try {
            input = Path.of(args.length > 0 ? args[0] : ask(console, "Путь к входному текстовому файлу: "));
            output = Path.of(args.length > 1 ? args[1] : ask(console, "Путь к выходному бинарному файлу: "));
        } catch (InvalidPathException e) {
            System.out.println("Некорректный путь: " + e.getMessage());
            System.exit(2);
            return;
        }

        // Работаем только через тип интерфейса.
        FigureStorage textStorage = new TextFigureStorage();
        FigureStorage binaryStorage = new BinaryFigureStorage();

        // 2. Чтение текстового списка.
        // System.exit не вызываем внутри catch: иначе блок finally не выполнится.
        List<Figure> figures = null;
        try {
            figures = textStorage.read(input);
        } catch (NoSuchFileException e) {
            System.out.println("Входной файл не найден: " + e.getFile());
        } catch (FigureFormatException e) {
            System.out.println("Ошибка формата входного файла. " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Причина: " + e.getCause());
            }
            System.out.println("Загрузка отклонена, ни одна фигура не принята.");
        } catch (IOException | FigureStorageException e) {
            System.out.println("Не удалось прочитать " + input + ": " + e);
        } finally {
            System.out.println("[finally] Попытка чтения " + input + " завершена");
        }
        if (figures == null) {
            System.exit(1);
            return;
        }

        // 3. Вывод фигур и расчётов через интерфейс Figure.
        System.out.println();
        System.out.println("Прочитано фигур: " + figures.size());
        for (int i = 0; i < figures.size(); i++) {
            Figure f = figures.get(i);
            System.out.printf("%2d. %-12s %-40s S = %12.4f  P = %10.4f%n",
                    i + 1, f.getClass().getSimpleName(), f, f.area(), f.perimeter());
        }

        // 4. Цилиндр со случайным основанием.
        System.out.println();
        if (figures.isEmpty()) {
            System.out.println("Список фигур пуст — цилиндр не создаётся.");
        } else {
            Figure base = figures.get(new Random().nextInt(figures.size()));
            double height = readHeight(args, console);
            try {
                Cylinder cylinder = new Cylinder(base, height);
                System.out.println("Цилиндр: основание = " + cylinder.getBase()
                        + ", высота = " + cylinder.getHeight());
                System.out.printf("Объём цилиндра: %.4f%n", cylinder.volume());
            } catch (InvalidFigureParameterException e) {
                System.out.println("Цилиндр не создан: " + e.getMessage());
            }
        }

        // 5. Сохранение списка в бинарный файл.
        System.out.println();
        try {
            binaryStorage.write(output, figures);
            // Сюда попадаем только после выхода из try-with-resources внутри write,
            // то есть поток уже закрыт и данные сброшены на диск.
            System.out.println("Список (" + figures.size() + " фиг.) сохранён в " + output.toAbsolutePath());
        } catch (IOException | FigureStorageException e) {
            System.out.println("Не удалось записать " + output + ": " + e);
            System.exit(1);
        }
    }

    private static String ask(Scanner console, String prompt) {
        System.out.print(prompt);
        return console.hasNextLine() ? console.nextLine().strip() : "";
    }

    /** Высота: из 3-го аргумента, иначе с консоли; пустой ввод — значение по умолчанию. */
    private static double readHeight(String[] args, Scanner console) {
        String raw = args.length > 2 ? args[2]
                : ask(console, "Высота цилиндра [Enter = " + DEFAULT_HEIGHT + "]: ");
        while (true) {
            if (raw.isEmpty()) {
                return DEFAULT_HEIGHT;
            }
            try {
                return Double.parseDouble(raw.replace(',', '.'));
            } catch (NumberFormatException e) {
                if (args.length > 2) {
                    System.out.println("Высота '" + raw + "' не число, используется " + DEFAULT_HEIGHT);
                    return DEFAULT_HEIGHT;
                }
                raw = ask(console, "Это не число, повторите: ");
            }
        }
    }
}
