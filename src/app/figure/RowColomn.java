package app.figure;

import app.common.IGameLife;

public class RowColomn implements IFigure {
    @Override
    public void setFigure(IGameLife data, int xOffset, int yOffset) {
        data.setCurrent(1, 0);
        data.setCurrent(1, 1);
        data.setCurrent(1, 2);

//        data.setCurrent(5, 0);
//        data.setCurrent(5, 1);
//        data.setCurrent(5, 2);
    }
}
