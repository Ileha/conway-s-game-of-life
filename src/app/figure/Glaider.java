package app.figure;

import app.common.IGameLife;

public class Glaider implements IFigure {

    @Override
    public void setFigure(IGameLife data, int xOffset, int yOffset) {
        data.setCurrent(1, 0);
        data.setCurrent(2, 1);
        data.setCurrent(0, 2);
        data.setCurrent(1, 2);
        data.setCurrent(2, 2);
    }
}
