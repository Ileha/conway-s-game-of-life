package app;

import app.common.IGameLife;
import app.common.Cell;
import app.common.hashlife.GameLifeHashEdition;
import app.figure.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.*;

import app.figure.Exceptions.FigureException;
import app.figure.RLE.RLEReader;
import edu.princeton.cs.introcs.StdDraw;

public class Main {
    static IGameLife data;
    static int offsetX = 0;
    static int offsetY = 0;

    static double scaleX = 1200;
    static double scaleY = 700;

    static double deltaTime = 1;

    private static final float MOVE_SPEED = 200;

    private static void init() {
        StdDraw.setCanvasSize(1200, 700);
        setScale(scaleX, scaleY);
        StdDraw.enableDoubleBuffering();
    }
    private static void setScale(double x, double y) {
        StdDraw.setXscale(0, x);
        StdDraw.setYscale(y, 0);

        scaleX = x;
        scaleY = y;
    }

    private static void draw() {
        StdDraw.clear(Color.WHITE);

        StdDraw.setPenColor(Color.black);

        for (Cell e : data.getCurrent()) {
            StdDraw.filledRectangle(e.getX()+offsetX, e.getY()+offsetY, 0.5, 0.5);
        }
    }

    public static void checkKeys() {
        if (StdDraw.isKeyPressed(KeyEvent.VK_UP)) {//up
            offsetY+=deltaTime*MOVE_SPEED;
        }

        if (StdDraw.isKeyPressed(KeyEvent.VK_DOWN)) {//down
            offsetY-=deltaTime*MOVE_SPEED;
        }

        if (StdDraw.isKeyPressed(KeyEvent.VK_LEFT)) {//left
            offsetX+=deltaTime*MOVE_SPEED;
        }
        if (StdDraw.isKeyPressed(KeyEvent.VK_RIGHT)) {//right
            offsetX-=deltaTime*MOVE_SPEED;
        }


        if (StdDraw.hasNextKeyTyped()) {
            switch(StdDraw.nextKeyTyped()) {
                case '+':
                    setScale((int)scaleX/1.5d, (int)scaleY/1.5d);
                    break;
                case '-':
                    setScale(scaleX*1.5d, scaleY*1.5d);
                    break;
                default:
                    break;
            }
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, FigureException {
        init();
        short live = 1;
        short dead = 0;

        data = new GameLifeHashEdition(1024, 1024, (short count, short state)-> {
            if (count == 3) {
                return live;
            }
            else if (count == 2 && state > 0) {
                return live;
            }
            else {
                return dead;
            }
        });

        //IFigure figure = new GlaiderGun();
        //IFigure figure = new RandomFill(0.45);
        IFigure figure = new RLEReader("./RLE/6enginecordershipgun.rle");
        //IFigure figure = new RowColomn();
        //IFigure figure = new Task();

        figure.setFigure(data, 512, 650);

        data.start();
        while (true) {
            long tStart = System.currentTimeMillis();

            Future[] future = data.calcNextStepAsync();
            //data.calcNextStep();

            checkKeys();
            draw();

            for (int i = 0; i < future.length; i++) {
                future[i].get();
            }

            data.change();

            long tFrame = System.currentTimeMillis() - tStart;

            deltaTime = tFrame / 1000.0;
            String time = "frame: " + tFrame + "ms";
            String fps = "fps: " + (1000.0 / tFrame);

            StdDraw.setPenColor(Color.red);

            StdDraw.textLeft(20, 20, time);
            StdDraw.textLeft(20, 40, fps);
            StdDraw.show();
        }
    }
}