package geometry2d;

import exceptions.InvalidFigureParameterException;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;

public class Circle implements Figure {

    private static final long serialVersionUID = 1L;

    private final double radius;

    public Circle(double radius) {
        this.radius = InvalidFigureParameterException.requirePositiveFinite("radius", radius);
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }

    @Override
    public String toString() {
        return "Circle{radius=" + radius + "}";
    }

    /**
     * Десериализация не вызывает конструктор, поэтому повторяем проверку здесь:
     * иначе подделанный бинарный файл мог бы содержать круг с радиусом -1.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            InvalidFigureParameterException.requirePositiveFinite("radius", radius);
        } catch (InvalidFigureParameterException e) {
            InvalidObjectException ioe = new InvalidObjectException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }
}
