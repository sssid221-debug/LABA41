package exceptions;

/**
 * Проверяемое исключение для некорректной строки текстового файла.
 * Хранит номер строки; исходная ошибка (NumberFormatException,
 * InvalidFigureParameterException) сохраняется как причина (getCause()).
 */
public class FigureFormatException extends FigureStorageException {

    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    public FigureFormatException(int lineNumber, String reason) {
        super("Строка " + lineNumber + ": " + reason);
        this.lineNumber = lineNumber;
    }

    public FigureFormatException(int lineNumber, String reason, Throwable cause) {
        super("Строка " + lineNumber + ": " + reason, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
