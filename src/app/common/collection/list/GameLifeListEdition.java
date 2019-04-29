package app.common.collection.list;

import app.common.Cell;
import app.common.IRule;
import app.common.collection.CellHandlers;
import app.common.collection.GeneratorIterator;
import gnu.trove.set.hash.THashSet;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GameLifeListEdition extends CellHandlers<List<Cell>> {
    private static final int                        CONTAINERS_COUNT = 2;
    private static final int                        NEXT_OFFSET = 1;

    private List<Cell>[]                            containers; //хранилище кадров
    private GeneratorIterator<Cell>[]               heaps;      //хранилище объектов которые можно переиспользовать

    /*
     * принимает значения от 0 до CONTAINERS_COUNT
     * используется для пререключения между кадрами
     */
    private int                                     currentContainerIndex;

    private Future                                  tasks[];
    private ExecutorService                         executor;

    private Set<Cell>                               cash;       //кэширование для повторного поиска

    public GameLifeListEdition(int width, int height, IRule rule) {
        super(width, height, rule);
        containers = new ArrayList[CONTAINERS_COUNT];
        heaps = new GeneratorIterator[CONTAINERS_COUNT];
        currentContainerIndex = 0;

        int capacity = width*height;
        for (int i = 0; i < CONTAINERS_COUNT; i++ ) {
            containers[i] = new ArrayList<>(capacity);
            heaps[i] = new GeneratorIterator<Cell>() {
                @Override
                public Cell getNewElemet() {
                    return new Cell();
                }
            };
        }

        cash = new THashSet<>();

        executor = Executors.newSingleThreadExecutor();
        tasks = new Future[1];
    }

    @Override
    protected boolean contains(List<Cell> collection, Cell cell) {
        if (collection == getCurrentContainer()) {
            if (Collections.binarySearch(collection, cell, Cell::compareTo) >= 0) {
                return true;
            } else {
                return false;
            }
        }
        else {
            return cash.contains(cell);
//            for (int i = collection.size()-1; i >= 0; i--) {
//                if (collection.get(i).equals(cell)) {
//                    return true;
//                }
//            }
//            return false;
        }
    }

    @Override
    protected void add(List<Cell> collection, Cell cell) {
        if (collection == getNextContainer()) {
            cash.add(cell);
        }

        collection.add(cell);
    }

    @Override
    public void change() {
        cash.clear();
        currentContainerIndex = (currentContainerIndex +1)%CONTAINERS_COUNT;
    }

    /*
    * Производит сортировку текущего поколения (для поиска)
    */
    @Override
    public void preCalculation() {
        getCurrentContainer().sort(Cell::compareTo);
    }

    @Override
    public void calcNextStep() {
        List<Cell> current = getCurrentContainer();
        List<Cell> next = getNextContainer();
        Iterator<Cell> heap = getNextHeap().iterator();
        next.clear();

        calcSortedFrame(current, current, next, heap);
    }

    @Override
    public Future[] calcNextStepAsync() throws ExecutionException, InterruptedException {
        tasks[0] = executor.submit(() -> {
            List<Cell> current = getCurrentContainer();
            List<Cell> next = getNextContainer();
            Iterator<Cell> heap = getNextHeap().iterator();
            next.clear();

            calcSortedFrame(current, current, next, heap);
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

    private List<Cell> getCurrentContainer() { return containers[currentContainerIndex]; }
    private List<Cell> getNextContainer() { return containers[(currentContainerIndex + NEXT_OFFSET)%CONTAINERS_COUNT]; }

    private GeneratorIterator<Cell> getCurrentHeap() {
        return heaps[currentContainerIndex];
    }
    private GeneratorIterator<Cell> getNextHeap() {
        return heaps[(currentContainerIndex + NEXT_OFFSET)%CONTAINERS_COUNT];
    }
}
