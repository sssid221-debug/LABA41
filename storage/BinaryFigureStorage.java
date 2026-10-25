package storage;

import exceptions.FigureStorageException;
import geometry2d.Figure;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Бинарное хранение через сериализацию Java.
 * В файл записывается один объект — ArrayList<Figure>.
 */
public class BinaryFigureStorage implements FigureStorage {

    @Override
    public List<Figure> read(Path path) throws IOException, FigureStorageException {
        Object obj;
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            obj = in.readObject();
        } catch (ClassNotFoundException e) {
            throw new FigureStorageException("Файл " + path
                    + " содержит объект неизвестного класса: " + e.getMessage(), e);
        } catch (ObjectStreamException | EOFException e) {
            // StreamCorruptedException, InvalidClassException, InvalidObjectException,
            // обрыв файла — это не файловая ошибка, а повреждённое содержимое.
            throw new FigureStorageException("Файл " + path
                    + " повреждён или не является сериализованным списком фигур: " + e, e);
        }

        if (!(obj instanceof List<?> rawList)) {
            throw new FigureStorageException("Файл " + path + " содержит объект типа "
                    + (obj == null ? "null" : obj.getClass().getName()) + ", ожидался List<Figure>");
        }
        List<Figure> figures = new ArrayList<>(rawList.size());
        for (int i = 0; i < rawList.size(); i++) {
            Object element = rawList.get(i);
            if (!(element instanceof Figure figure)) {
                throw new FigureStorageException("Элемент № " + i + " имеет тип "
                        + (element == null ? "null" : element.getClass().getName())
                        + ", ожидалась Figure");
            }
            figures.add(figure);
        }
        return figures;
    }

    @Override
    public void write(Path path, List<Figure> figures) throws IOException {
        // Копируем в ArrayList: переданная реализация List (например, List.of или subList)
        // может оказаться несериализуемой.
        ArrayList<Figure> copy = new ArrayList<>(figures);
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            out.writeObject(copy);
        }
    }
}
