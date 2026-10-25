package storage;

import exceptions.FigureStorageException;
import geometry2d.Figure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Общий контракт файлового хранения списка фигур.
 *
 * Проверяемые исключения:
 *  - IOException — файловые ошибки (нет файла, нет прав, путь — каталог и т.п.);
 *  - FigureStorageException — файл прочитан, но его содержимое не является
 *    корректным списком фигур. Для текстового формата выбрасывается подкласс
 *    FigureFormatException с номером строки. ClassNotFoundException при
 *    десериализации оборачивается в FigureStorageException с сохранением причины.
 */
public interface FigureStorage {

    /**
     * Читает список фигур из файла.
     *
     * @return список фигур; для корректного файла без фигур — пустой список
     */
    List<Figure> read(Path path) throws IOException, FigureStorageException;

    /**
     * Записывает список фигур, заменяя прежнее содержимое файла.
     */
    void write(Path path, List<Figure> figures) throws IOException, FigureStorageException;
}
