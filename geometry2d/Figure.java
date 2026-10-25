package geometry2d;

import java.io.Serializable;

/**
 * Плоская фигура. Расширяет Serializable, чтобы любую фигуру
 * можно было сохранить средствами сериализации Java.
 */
public interface Figure extends Serializable {

    /** Площадь фигуры. */
    double area();

    /** Периметр фигуры. */
    double perimeter();
}
