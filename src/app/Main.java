package app;

import app.common.IGameLife;
import app.common.Cell;
import app.common.collection.GameLifeCollectionEdition;
import app.common.hashlife.GameLifeHashEdition;
import app.figure.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.*;
import javax.swing.JFrame;

import app.figure.Exceptions.FigureException;
import app.figure.RLE.RLEReader;
import edu.princeton.cs.introcs.StdDraw;

public class Main {
    static IGameLife data;
    static double offsetX = 0;
    static double offsetY = 0;

    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 700;

    static double scaleX = CANVAS_WIDTH;
    static double scaleY = CANVAS_HEIGHT;

    static double deltaTime = 0;                        //секунды, длительность предыдущего кадра

    private static final float MOVE_SPEED = 200;        //пикселей экрана в секунду
    private static final double MAX_DELTA_TIME = 0.25;  //ограничение, чтобы после зависания экран не прыгал
    private static final double VIEW_MARGIN = 1.1;      //запас вокруг фигуры при подгонке масштаба
    private static final double MAX_CELL_PIXELS = 10;   //максимальный размер клетки на экране при подгонке
    private static final int MIN_WORLD_SIZE = 10 * 1022;//минимальная сторона мира: ~10 pp8primecalculator.rle (1022x618)
    private static final double KEY_ZOOM_STEP = 1.5;    //масштаб за одно нажатие +/-
    private static final double WHEEL_ZOOM_STEP = 1.2;  //масштаб за один щелчок колеса
    private static final double MAX_ZOOM_CELL_PIXELS = 50;//максимальный размер клетки на экране при приближении

    /*
    * колесо мыши обрабатывается в потоке Swing,
    * поэтому прокрутка копится здесь и применяется в checkKeys()
    */
    private static final Object wheelLock = new Object();
    private static double wheelRotation = 0;
    private static int wheelPixelX = 0;
    private static int wheelPixelY = 0;

    private static void init() {
        StdDraw.setCanvasSize(CANVAS_WIDTH, CANVAS_HEIGHT);
        setScale(scaleX, scaleY);
        StdDraw.enableDoubleBuffering();
        initMouseWheel();
    }

    /*
    * StdDraw не поддерживает колесо мыши, поэтому слушатель
    * добавляется напрямую в его окно (координаты события в пикселях холста)
    */
    private static void initMouseWheel() {
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof JFrame && frame.isVisible()) {
                ((JFrame) frame).getContentPane().addMouseWheelListener(e -> {
                    synchronized (wheelLock) {
                        wheelRotation += e.getPreciseWheelRotation();
                        wheelPixelX = e.getX();
                        wheelPixelY = e.getY();
                    }
                });
            }
        }
    }

    /*
    * меняет масштаб в factor раз так, чтобы точка (x, y) на экране
    * (в текущих единицах мира) осталась на месте
    */
    private static void zoomAt(double factor, double x, double y) {
        double minScaleX = CANVAS_WIDTH / MAX_ZOOM_CELL_PIXELS;
        factor = Math.max(factor, minScaleX / scaleX);

        offsetX += x * (factor - 1);
        offsetY += y * (factor - 1);
        setScale(scaleX * factor, scaleY * factor);
    }

    /*
    * подбирает масштаб так, чтобы фигура (x, y, width, height) целиком
    * помещалась на экране, и ставит её в центр
    */
    private static void fitView(int x, int y, int width, int height) {
        double cellsPerPixel = Math.max(width / (double) CANVAS_WIDTH, height / (double) CANVAS_HEIGHT) * VIEW_MARGIN;
        cellsPerPixel = Math.max(cellsPerPixel, 1 / MAX_CELL_PIXELS);

        setScale(CANVAS_WIDTH * cellsPerPixel, CANVAS_HEIGHT * cellsPerPixel);

        offsetX = (scaleX - width) / 2 - x;
        offsetY = (scaleY - height) / 2 - y;
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

    /*
    * переводит пиксели экрана в единицы мира,
    * чтобы статистика не зависела от масштаба
    */
    private static double pixelsToWorld(double pixels) {
        return pixels * scaleX / CANVAS_WIDTH;
    }

    /*
    * статистика в левом верхнем углу на белой подложке
    */
    private static void drawStats(double frameMs) {
        String time = String.format("frame: %.1fms", frameMs);
        String fps = String.format("fps: %.1f", 1000.0 / Math.max(frameMs, 0.001));

        StdDraw.setPenColor(Color.WHITE);
        StdDraw.filledRectangle(pixelsToWorld(90), pixelsToWorld(28), pixelsToWorld(85), pixelsToWorld(23));

        StdDraw.setPenColor(Color.red);
        StdDraw.textLeft(pixelsToWorld(10), pixelsToWorld(17), time);
        StdDraw.textLeft(pixelsToWorld(10), pixelsToWorld(38), fps);
    }

    public static void checkKeys() {
        //скорость в клетках, чтобы при любом масштабе экран двигался одинаково
        double move = deltaTime*MOVE_SPEED*scaleX/CANVAS_WIDTH;

        if (StdDraw.isKeyPressed(KeyEvent.VK_UP)) {//up
            offsetY+=move;
        }

        if (StdDraw.isKeyPressed(KeyEvent.VK_DOWN)) {//down
            offsetY-=move;
        }

        if (StdDraw.isKeyPressed(KeyEvent.VK_LEFT)) {//left
            offsetX+=move;
        }
        if (StdDraw.isKeyPressed(KeyEvent.VK_RIGHT)) {//right
            offsetX-=move;
        }


        //все нажатия за кадр, иначе при низком fps они копятся в очереди
        while (StdDraw.hasNextKeyTyped()) {
            switch(StdDraw.nextKeyTyped()) {
                case '+':
                    zoomAt(1 / KEY_ZOOM_STEP, scaleX / 2, scaleY / 2);
                    break;
                case '-':
                    zoomAt(KEY_ZOOM_STEP, scaleX / 2, scaleY / 2);
                    break;
                default:
                    break;
            }
        }

        //колесо: вниз - отдалить, вверх - приблизить, относительно указателя мыши
        double rotation;
        int pixelX;
        int pixelY;
        synchronized (wheelLock) {
            rotation = wheelRotation;
            pixelX = wheelPixelX;
            pixelY = wheelPixelY;
            wheelRotation = 0;
        }
        if (rotation != 0) {
            zoomAt(Math.pow(WHEEL_ZOOM_STEP, rotation),
                   pixelX * scaleX / CANVAS_WIDTH,
                   pixelY * scaleY / CANVAS_HEIGHT);
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, FigureException {
        init();
        short live = 1;
        short dead = 0;

        //IFigure figure = new GlaiderGun();
//        IFigure figure = new RandomFill(0.45);
        RLEReader figure = new RLEReader("./RLE/pp8primecalculator.rle");
        //IFigure figure = new RowColomn();
        //IFigure figure = new Task();

        //размеры фигуры берутся из заголовка RLE (для других фигур задать вручную)
        int[] figureSize = figure.readSize();
        int figureWidth = Math.max(figureSize[0], 1);
        int figureHeight = Math.max(figureSize[1], 1);

        /*
        * мир hashlife не растёт, всё что выходит за его границы пропадает,
        * поэтому вокруг фигуры оставляется пустое поле в половину её размера,
        * но мир не меньше MIN_WORLD_SIZE; фигура ставится в центр мира
        */
        int figureMax = Math.max(figureWidth, figureHeight);
        int worldSize = Math.max(figureMax + 2*(figureMax/2), MIN_WORLD_SIZE);
        worldSize = Integer.highestOneBit(worldSize - 1) << 1;//hashlife всё равно округлит до степени двойки
        int paddingX = (worldSize - figureWidth) / 2;
        int paddingY = (worldSize - figureHeight) / 2;

        data = new GameLifeHashEdition(worldSize, worldSize, (short count, short state)-> {
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

        figure.setFigure(data, paddingX, paddingY);
        fitView(paddingX, paddingY, figureWidth, figureHeight);

        data.start();
        long lastFrameStart = System.nanoTime();
        while (true) {
            /*
            * время считается от начала прошлого кадра до начала текущего,
            * чтобы учитывался весь кадр вместе с StdDraw.show()
            * nanoTime точнее currentTimeMillis (на Windows шаг ~15.6ms)
            */
            long frameStart = System.nanoTime();
            double frameMs = (frameStart - lastFrameStart) / 1_000_000.0;
            lastFrameStart = frameStart;

            deltaTime = Math.min(frameMs / 1000.0, MAX_DELTA_TIME);

            Future[] future = data.calcNextStepAsync();
            //data.calcNextStep();

            checkKeys();
            draw();

            for (int i = 0; i < future.length; i++) {
                future[i].get();
            }

            data.change();

            drawStats(frameMs);
            StdDraw.show();
        }
    }
}