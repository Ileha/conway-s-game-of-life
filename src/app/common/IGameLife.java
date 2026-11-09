package app.common;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class IGameLife {
    protected IRule rule;
    private int w;
    private int h;

    public IGameLife(int width, int height, IRule rule) {
        this.rule = rule;
        w = width;
        h = height;
    }

    public void start() { } //вызывается однажды перед стартом

    public abstract void change();

    /*
    * вычисляют следующий кадр
    * вычисленный кадр можно получить вызовом getCurrent(), пред этим вызвав метод change()
    */
    public abstract void calcNextStep();
    public abstract Future[] calcNextStepAsync() throws ExecutionException, InterruptedException;

    //размеры холста
    public int getHeight() { return h; }
    public int getWidth() { return w; }

    /*
    * утсанавливает клетку в текущем кадре
    * используется для начальной генерации клеток
    */
    public abstract void setCurrent(int x, int y);

    //возвращает итератор для текущего кадра
    public abstract Iterable<Cell> getCurrent();

    /*
    * рисует текущий кадр в pixels (width x height, по строкам) цветом CELL_COLOR,
    * клетка (x, y) занимает пиксели от (x + offsetX) * pixelsPerCell до (x + 1 + offsetX) * pixelsPerCell
    * можно вызывать параллельно с calcNextStepAsync()
    */
    public void render(int[] pixels, int width, int height, double offsetX, double offsetY, double pixelsPerCell) {
        for (Cell e : getCurrent()) {
            fillRect(pixels, width, height,
                    (e.getX() + offsetX) * pixelsPerCell, (e.getY() + offsetY) * pixelsPerCell,
                    (e.getX() + 1 + offsetX) * pixelsPerCell, (e.getY() + 1 + offsetY) * pixelsPerCell);
        }
    }

    public static final int CELL_COLOR = 0x000000;

    /*
    * закрашивает прямоугольник экрана [x0, x1) x [y0, y1), обрезая его по краям,
    * но не меньше одного пикселя, чтобы мелкие клетки не пропадали
    */
    protected static void fillRect(int[] pixels, int width, int height, double x0, double y0, double x1, double y1) {
        if (x1 <= 0 || y1 <= 0 || x0 >= width || y0 >= height) {
            return;
        }

        int left = Math.max(0, (int) Math.floor(x0));
        int top = Math.max(0, (int) Math.floor(y0));
        int right = Math.min(width, Math.max(left + 1, (int) Math.floor(x1)));
        int bottom = Math.min(height, Math.max(top + 1, (int) Math.floor(y1)));

        for (int y = top; y < bottom; y++) {
            Arrays.fill(pixels, y * width + left, y * width + right, CELL_COLOR);
        }
    }

    /*
    * используется для начальной генерации/загрузки из RLE клеток
    * при значении true не проверяется выход за границы холста
    */
    public abstract boolean isUsedInfinitePlate();
}
