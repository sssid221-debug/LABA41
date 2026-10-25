package exceptions;

/**
 * Проверяемое исключение: содержимое файла не удалось превратить в список фигур
 * (неверный текстовый формат, повреждённый бинарный файл, объект другого типа,
 * отсутствующий при десериализации класс).
 *
 * Наследуется от Exception, а не от RuntimeException: такая ошибка зависит
 * от внешних данных, а не от кода, и вызывающий код обязан решить, что делать
 * (сообщить пользователю, выбрать другой файл и т.п.). Компилятор это проверит.
 */
public class FigureStorageException extends Exception {

    private static final long serialVersionUID = 1L;

    public FigureStorageException(String message) {
        super(message);
    }

    public FigureStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
