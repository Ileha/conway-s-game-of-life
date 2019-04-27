package app.common.collection.set;

import app.common.Cell;
import app.common.IRule;
import app.common.collection.CellHandlers;
import gnu.trove.set.hash.TLinkedHashSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GameLifeTripleMapEdition extends CellHandlers<Set<Cell>> {
    private static final int    CONTAINERS_COUNT = 3;
    private static final int    NEXT_OFFSET = 1;
    private static final int    HEAP_OFFSET = 2;

    private Set<Cell>[]         containers;                     //хранилище кадров

    /*
     * принимает значения от 0 до CONTAINERS_COUNT
     * используется для пререключения между кадрами
     */
    private int                 currentContainerIndex;

    /*
     * currentContainerIndex+0                                  - current   то что на экране
     * (currentContainerIndex+NEXT_OFFSET)%CONTAINERS_COUNT     - next      туда идёт запись будущего поколения
     * (currentContainerIndex+HEAP_OFFSET)%CONTAINERS_COUNT     - heap      хранит неактуальные клетки используется
     *                                                                      для переиспользования
     *
     * каждый container крутится в цикле current -> heap -> next -> current
     *
     * index     0       1       2
     * frame1    current next    heap
     * frame2    heap    current next
     * frame3    next    heap    current
     * frame4    current next    heap
     *                   ...
     *
     */

    private Future              tasks[];
    private ExecutorService     executor;

    public GameLifeTripleMapEdition(int width, int height, IRule rule) {
        super(width, height, rule);
        containers = new Set[CONTAINERS_COUNT];
        currentContainerIndex = 0;

        int capacity = width*height;
        for (int i = 0; i < CONTAINERS_COUNT; i++ ) {
            containers[i] = new TLinkedHashSet<>();
        }

        executor = Executors.newSingleThreadExecutor();
        tasks = new Future[1];
    }

    @Override
    protected boolean contains(Set<Cell> collection, Cell cell) {
        return collection.contains(cell);
    }

    @Override
    protected void add(Set<Cell> collection, Cell cell) {
        collection.add(cell);
    }

    @Override
    public void change() {
        currentContainerIndex = (currentContainerIndex +1)% CONTAINERS_COUNT;
    }

    @Override
    public void calcNextStep() {
        Set<Cell> current = getCurrentMap();
        Set<Cell> next = getNextMap();
        Iterator<Cell> heap = getHeapMap().iterator();
        next.clear();

        calcFrame(current.stream(), current, next, heap);
    }

    @Override
    public Future[] calcNextStepAsync() throws ExecutionException, InterruptedException {
        tasks[0] = executor.submit(() -> {
            Set<Cell> current = getCurrentMap();
            Set<Cell> next = getNextMap();
            Iterator<Cell> heap = getHeapMap().iterator();
            next.clear();

            calcFrame(current.stream(), current, next, heap);
        });

        return tasks;
    }

    @Override
    public void setCurrent(int x, int y) {
        Cell cell = new Cell(x, y);
        containers[currentContainerIndex].add(cell);
    }

    @Override
    public Iterable<Cell> getCurrent() {
        return getCurrentMap();
    }

    private Set<Cell> getCurrentMap() {
        return containers[currentContainerIndex];
    }
    private Set<Cell> getNextMap() {
        return containers[(currentContainerIndex + NEXT_OFFSET)% CONTAINERS_COUNT];
    }
    private Set<Cell> getHeapMap() {
        return containers[(currentContainerIndex + HEAP_OFFSET)% CONTAINERS_COUNT];
    }

    @Override
    public boolean isUsedInfinitePlate() {
        return true;
    }
}
