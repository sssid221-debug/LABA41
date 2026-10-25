package storage;

import exceptions.FigureFormatException;
import exceptions.FigureStorageException;
import exceptions.InvalidFigureParameterException;
import geometry2d.Circle;
import geometry2d.Figure;
import geometry2d.Rectangle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Текстовое хранение фигур (java.nio: Files.newBufferedReader / newBufferedWriter, UTF-8).
 *
 * Формат: одна фигура на строку, поля разделены ';'
 *   CIRCLE;radius
 *   RECTANGLE;width;height
 * Тип нечувствителен к регистру, пробелы вокруг полей игнорируются.
 * Пустые строки и строки, начинающиеся с '#', пропускаются.
 *
 * Поведение при ошибке: загрузка отклоняется целиком. Выбрасывается
 * FigureFormatException с номером строки, уже разобранные фигуры отбрасываются —
 * вызывающий код получает либо весь корректный список, либо исключение.
 */
public class TextFigureStorage implements FigureStorage {

    public static final String SEPARATOR = ";";
    public static final String CIRCLE = "CIRCLE";
    public static final String RECTANGLE = "RECTANGLE";

    @Override
    public List<Figure> read(Path path) throws IOException, FigureFormatException {
        List<Figure> figures = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("﻿")) {
                    line = line.substring(1); // BOM, который добавляет Блокнот Windows
                }
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                figures.add(parseLine(trimmed, lineNumber));
            }
        }
        return figures;
    }

    /** Разбирает одну непустую строку в фигуру. */
    Figure parseLine(String line, int lineNumber) throws FigureFormatException {
        String[] parts = line.split(SEPARATOR, -1);
        String type = parts[0].strip().toUpperCase(Locale.ROOT);

        int expected = switch (type) {
            case CIRCLE -> 1;
            case RECTANGLE -> 2;
            default -> throw new FigureFormatException(lineNumber,
                    "неизвестный тип фигуры '" + parts[0].strip() + "'");
        };
        int actual = parts.length - 1;
        if (actual != expected) {
            throw new FigureFormatException(lineNumber, "для " + type + " ожидается "
                    + expected + " парам., получено " + actual);
        }

        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            String raw = parts[i + 1].strip();
            try {
                values[i] = Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                throw new FigureFormatException(lineNumber,
                        "параметр № " + (i + 1) + " не является числом: '" + raw + "'", e);
            }
        }

        try {
            return switch (type) {
                case CIRCLE -> new Circle(values[0]);
                case RECTANGLE -> new Rectangle(values[0], values[1]);
                default -> throw new AssertionError(type);
            };
        } catch (InvalidFigureParameterException e) {
            throw new FigureFormatException(lineNumber, e.getMessage(), e);
        }
    }

    @Override
    public void write(Path path, List<Figure> figures) throws IOException, FigureStorageException {
        // Сначала форматируем всё в память: если в списке неподдерживаемая фигура,
        // существующий файл не будет испорчен наполовину записанными данными.
        List<String> lines = new ArrayList<>(figures.size());
        for (int i = 0; i < figures.size(); i++) {
            lines.add(formatFigure(figures.get(i), i));
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private String formatFigure(Figure figure, int index) throws FigureStorageException {
        // Double.toString даёт кратчайшее точное представление — значение
        // при повторном чтении совпадает побитно.
        if (figure instanceof Circle c) {
            return CIRCLE + SEPARATOR + c.getRadius();
        }
        if (figure instanceof Rectangle r) {
            return RECTANGLE + SEPARATOR + r.getWidth() + SEPARATOR + r.getHeight();
        }
        throw new FigureStorageException("Элемент № " + index + ": тип "
                + (figure == null ? "null" : figure.getClass().getName())
                + " не поддерживается текстовым форматом");
    }
}
