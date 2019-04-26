package app.common;

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
    * используется для начальной генерации/загрузки из RLE клеток
    * при значении true не проверяется выход за границы холста
    */
    public abstract boolean isUsedInfinitePlate();
}
