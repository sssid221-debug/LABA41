package geometry2d;

import exceptions.InvalidFigureParameterException;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;

public class Rectangle implements Figure {

    private static final long serialVersionUID = 1L;

    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = InvalidFigureParameterException.requirePositiveFinite("width", width);
        this.height = InvalidFigureParameterException.requirePositiveFinite("height", height);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public double perimeter() {
        return 2 * (width + height);
    }

    @Override
    public String toString() {
        return "Rectangle{width=" + width + ", height=" + height + "}";
    }

    /** Повторная проверка инвариантов после десериализации (конструктор не вызывается). */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            InvalidFigureParameterException.requirePositiveFinite("width", width);
            InvalidFigureParameterException.requirePositiveFinite("height", height);
        } catch (InvalidFigureParameterException e) {
            InvalidObjectException ioe = new InvalidObjectException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }
}
