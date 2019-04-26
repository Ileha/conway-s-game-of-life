package app.common.arrays;

import app.common.IGameLife;
import app.common.Cell;
import app.common.IRule;

import java.util.concurrent.*;

public class GameLifeArraysEdition extends IGameLife {
    private short[][] array1;
    private short[][] array2;
    private boolean current = false;
    private int executorCount = 0;

    private ArrayIterator array1Iterator;
    private ArrayIterator array2Iterator;

    private ExecutorService executor;
    private Future tasks[];

    public GameLifeArraysEdition(int width, int height, IRule rule) {
        super(width, height, rule);

        executorCount = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);

        array1 = new short[height][width];
        array2 = new short[height][width];

        array1Iterator = new ArrayIterator(array1);
        array2Iterator = new ArrayIterator(array2);

        executor = Executors.newFixedThreadPool(executorCount);
        tasks = new Future[executorCount];
    }

    @Override
    public void change() {
        current = !current;
    }

    private short[][] getCurrentBuffer() {
        if (current) { return array1; }
        else { return array2; }
    }

    private short[][] getLastBuffer() {
        if (!current) { return array1; }
        else { return array2; }
    }

    @Override
    public void calcNextStep() {
        calcNextStep(0, getHeight());
    }

    @Override
    public Future[] calcNextStepAsync() {
        int w = getHeight();

        for (int i = 0; i < executorCount; i++) {
            int g = i;

            tasks[i] = executor.submit(() -> {
                int from = w/executorCount*g;
                int to = 0;
                if (g == executorCount-1) {
                    to = w;
                }
                else {
                    to = w/executorCount*(g+1);
                }
                calcNextStep(from, to);
            });
        }
        return tasks;
    }

    @Override
    public void setCurrent(int x, int y) {
        if (current) { array1[y][x] = 1; }
        else { array2[y][x] = 1; }
    }

    @Override
    public Iterable<Cell> getCurrent() {
        if (current) { return array1Iterator; }
        else { return array2Iterator; }
    }

    @Override
    public boolean isUsedInfinitePlate() {
        return false;
    }

    private void calcNextStep(int from, int to) {
        short[][] last = getLastBuffer();
        short[][] current = getCurrentBuffer();

        for (int i = from; i < to; i++) {
            for (int g = 0; g < current[i].length; g++) {
                int from_g = 0;
                int to_g = 2;
                int from_v = 0;
                int to_v = 2;

                if (i == 0) {//uper row
                    from_v = 1;
                }
                if (g == 0) {//left side
                    from_g = 1;
                }
                if (g == current[i].length-1) {//right side
                    to_g = 1;
                }
                if (i == current.length-1) {//lowest row
                    to_v = 1;
                }
                short count = 0;
                for (int ii = from_v; ii <= to_v; ii++) {
                    for (int gg = from_g; gg <= to_g; gg++) {
                        if (ii == 1 && gg == 1) {continue;}
                        count+=current[i+(ii-1)][g+(gg-1)];
                    }
                }
                last[i][g] = rule.rule(count, current[i][g]);
            }
        }
    }
}
