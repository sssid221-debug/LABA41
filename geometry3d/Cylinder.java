package geometry3d;

import exceptions.InvalidFigureParameterException;
import geometry2d.Figure;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/** Прямой цилиндр с произвольным плоским основанием. */
public class Cylinder implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Figure base;
    private final double height;

    public Cylinder(Figure base, double height) {
        if (base == null) {
            throw new InvalidFigureParameterException("base", "основание не должно быть null");
        }
        this.base = base;
        this.height = InvalidFigureParameterException.requirePositiveFinite("height", height);
    }

    public Figure getBase() {
        return base;
    }

    public double getHeight() {
        return height;
    }

    /** Объём = площадь основания × высота; площадь берётся через интерфейс Figure. */
    public double volume() {
        return base.area() * height;
    }

    @Override
    public String toString() {
        return "Cylinder{base=" + base + ", height=" + height + "}";
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (base == null) {
            throw new InvalidObjectException("Некорректный параметр 'base': основание не должно быть null");
        }
        try {
            InvalidFigureParameterException.requirePositiveFinite("height", height);
        } catch (InvalidFigureParameterException e) {
            InvalidObjectException ioe = new InvalidObjectException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }
}
