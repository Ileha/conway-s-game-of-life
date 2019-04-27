package app.common.collection.set;

import app.common.*;
import app.common.collection.CellHandlers;
import app.common.collection.GeneratorIterator;
import gnu.trove.set.hash.TLinkedHashSet;

import java.util.*;
import java.util.concurrent.*;

public class GameLifeCollectionEdition extends CellHandlers<Set<Cell>> {
    private static final int            CONTAINERS_COUNT = 2;
    private static final int            NEXT_OFFSET = 1;

    private Set<Cell>[]                 containers;          //хранилище кадров
    private GeneratorIterator<Cell>[]   heaps;               //хранилище объектов которые можно переиспользовать

    /*
    * принимает значения от 0 до CONTAINERS_COUNT
    * используется для пререключения между кадрами
    */
    private int                         currentContainerIndex;

    /*
    * currentContainerIndex+0                               - current   то что на экране
    * (currentContainerIndex+NEXT_OFFSET)%CONTAINERS_COUNT  - next      туда идёт запись будущего поколения
    *
    * каждый container имеет heap в котором храняться клетки готовые для переиспользования
    * heap при необходимости создаёт и добавляет в себя клетки при вызове iterator().next()
    * индексу containerа соответствует heap
    */

    private Future                      tasks[];
    private ExecutorService             executor;

    public GameLifeCollectionEdition(int width, int height, IRule rule) {
        super(width, height, rule);
        containers = new Set[CONTAINERS_COUNT];
        heaps = new GeneratorIterator[CONTAINERS_COUNT];
        currentContainerIndex = 0;

        int capacity = width*height;
        for (int i = 0; i < CONTAINERS_COUNT; i++ ) {
            containers[i] = new TLinkedHashSet<>();
            heaps[i] = new GeneratorIterator<Cell>(capacity) {
                @Override
                public Cell getNewElemet() {
                    return new Cell();
                }
            };
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
        currentContainerIndex = (currentContainerIndex +1)%CONTAINERS_COUNT;
    }

    @Override
    public void calcNextStep() {
        Set<Cell> current = getCurrentMap();
        Set<Cell> next = getNextMap();
        Iterator<Cell> heap = getNextHeap().iterator();
        next.clear();

        calcFrame(current.stream(), current, next, heap);
    }

    @Override
    public Future[] calcNextStepAsync() {
        tasks[0] = executor.submit(() -> {
            Set<Cell> current = getCurrentMap();
            Set<Cell> next = getNextMap();
            Iterator<Cell> heap = getNextHeap().iterator();
            next.clear();

            calcFrame(current.stream(), current, next, heap);
        });

        return tasks;
    }

    @Override
    public void setCurrent(int x, int y) {
        Cell cell = new Cell(x, y);
        containers[currentContainerIndex].add(cell);
        getCurrentHeap().add(cell);
    }

    @Override
    public Iterable<Cell> getCurrent() {
        return containers[currentContainerIndex];
    }

    @Override
    public boolean isUsedInfinitePlate() {
        return true;
    }

    private Set<Cell> getCurrentMap() {
        return containers[currentContainerIndex];
    }
    private Set<Cell> getNextMap() {
        return containers[(currentContainerIndex + NEXT_OFFSET)%CONTAINERS_COUNT];
    }

    private GeneratorIterator<Cell> getCurrentHeap() {
        return heaps[currentContainerIndex];
    }
    private GeneratorIterator<Cell> getNextHeap() {
        return heaps[(currentContainerIndex + NEXT_OFFSET)%CONTAINERS_COUNT];
    }
}