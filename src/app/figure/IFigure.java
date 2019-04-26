package app.figure;

import app.common.IGameLife;
import app.figure.Exceptions.FigureException;

public interface IFigure {
    void setFigure(IGameLife data, int xOffset, int yOffset) throws FigureException;
}
