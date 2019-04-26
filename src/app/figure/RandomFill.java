package app.figure;

import app.common.IGameLife;

import java.util.Random;

public class RandomFill implements IFigure {
    private  double range;

    public  RandomFill(double range) {
        this.range = range;
    }

    @Override
    public void setFigure(IGameLife data, int xOffset, int yOffset) {
        Random rnd = new Random();

        for (int i = 0; i < data.getHeight()-1; i++) {
            for (int j = 0; j < data.getWidth()-1; j++) {
                if (rnd.nextDouble() <= range) {
                    data.setCurrent(j, i);
                }
            }
        }
    }
}
