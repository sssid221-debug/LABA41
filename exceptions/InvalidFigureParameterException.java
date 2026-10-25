package exceptions;

/**
 * Непроверяемое исключение для некорректных параметров фигур.
 *
 * Наследуется от IllegalArgumentException (т.е. от RuntimeException):
 * неверный аргумент конструктора — ошибка программиста или входных данных,
 * которую нельзя «исправить» на месте вызова, поэтому компилятор не должен
 * заставлять оборачивать каждое создание фигуры в try/catch.
 */
public class InvalidFigureParameterException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String parameterName;

    public InvalidFigureParameterException(String parameterName, String reason) {
        super("Некорректный параметр '" + parameterName + "': " + reason);
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }

    /**
     * Проверяет, что размер — положительное конечное число.
     *
     * @return переданное значение, если оно корректно
     */
    public static double requirePositiveFinite(String parameterName, double value) {
        if (Double.isNaN(value)) {
            throw new InvalidFigureParameterException(parameterName, "значение NaN недопустимо");
        }
        if (Double.isInfinite(value)) {
            throw new InvalidFigureParameterException(parameterName,
                    "значение должно быть конечным, получено " + value);
        }
        if (value <= 0) {
            throw new InvalidFigureParameterException(parameterName,
                    "значение должно быть положительным, получено " + value);
        }
        return value;
    }
}
