package app.common.collection;

import app.common.IGameLife;
import app.common.Cell;
import app.common.IRule;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/*
* Определяет логику обработки клеток
*/
public abstract class CellHandlers extends IGameLife {
    /*
     * currentThreadRI.counts[] количество живых клеток вокруг каждой из 9, индекс это номер клетки
     * currentThreadRI.states[] массив показывающий какие клетки живые, а какие нет индекс это номер клетки
     * currentThreadRI.cellTry клетка для тестов
     */
    protected ThreadLocal<RuleInfo> ruleInfoByThread;//информация для тестов

    public CellHandlers(int width, int height, IRule rule) {
        super(width, height, rule);
        ruleInfoByThread = new ThreadLocal<RuleInfo>() {
            @Override
            protected RuleInfo initialValue() {
                return new RuleInfo();
            }
        };
    }

    /*
    * решает каким методом обрабатывать клетку основываясь на её положении и положении предыдущей
    * сортирует клетки
    */
    protected void calcFrame(Stream<Cell> stream,
                            Set<Cell> current,
                            Set<Cell> next,
                            Iterator<Cell> heap)
    {
        RuleInfo currentThreadRI = ruleInfoByThread.get();
        currentThreadRI.resetLastValues();

        stream.sorted((c1, c2) -> c1.compareTo(c2))
            .forEach(cell -> {
                int deltaY = cell.getY() - currentThreadRI.ly;

                if (deltaY == 0) {
                    int deltaX = cell.getX()-currentThreadRI.lx;
                    if (deltaX == 1) {
                        calcCellStep1(cell, current, next, heap);
                    }
                    else if (deltaX == 2) {
                        calcCellStep2(cell, current, next, heap);
                    }
                    else {
                        calcSingleCell(cell, current, next, heap);
                    }
                }
                else {
                    calcSingleCell(cell, current, next, heap);
                }

                currentThreadRI.lx = cell.getX();
                currentThreadRI.ly = cell.getY();
            }
        );
    }

    /*
     * row 0   0  0 0
     * row 1   0 с0 0
     * row 2   с с1 0 - x
     * row 3   0 с2 0
     * row 4   0  0 0
     *            |
     *            y
     *
     * c текущая живая клетка
     */
    protected void calcCellStep1(Cell cell,
                                  Set<Cell> current,
                                  Set<Cell> next,
                                  Iterator<Cell> heap)
    {
        RuleInfo currentThreadRI = ruleInfoByThread.get();
        Arrays.fill(currentThreadRI.counts, 0, 3, (short) 1);
        Arrays.fill(currentThreadRI.states, 0, 3, (short) 0);

        int x = cell.getX();
        int y = cell.getY();

        //row 0
        currentThreadRI.cellTry.set(x, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
        }
        //end row 0

        //row 1
        currentThreadRI.cellTry.set(x, y-1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y-1);
        if (current.contains(currentThreadRI.cellTry)) {//0 enemy
            currentThreadRI.states[0] = 1;

            currentThreadRI.counts[1] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y-1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
        }
        //end row 1

        //row 2
        //пропусакем x, y т.к. уже учли её

        currentThreadRI.cellTry.set(x+1, y);
        if (current.contains(currentThreadRI.cellTry)) {//1 enemy
            currentThreadRI.states[1] = 1;

            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[2] += 1;
        }
        //end row 2

        //row 3
        currentThreadRI.cellTry.set(x, y+1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y+1);
        if (current.contains(currentThreadRI.cellTry)) {//2 enemy
            currentThreadRI.states[2] = 1;

            currentThreadRI.counts[1] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y+1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[2] += 1;
        }
        //end row 3

        //row 4
        currentThreadRI.cellTry.set(x, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
        }
        //end row 4

        for (int i = 0; i < 3; i++) {
            if (rule.rule(currentThreadRI.counts[i], currentThreadRI.states[i]) > 0) {
                int index = i+((i+1)*2);

                fromLocalToGlobal(currentThreadRI.cellTry, index, x, y);
                if (!next.contains(currentThreadRI.cellTry)) {
                    Cell forAdd = null;
                    if (heap.hasNext()) {
                        forAdd = heap.next();
                    }
                    else {
                        forAdd = new Cell();
                    }
                    forAdd.set(currentThreadRI.cellTry);
                    next.add(forAdd);
                }
            }
        }
    }

    /*
     * row 0   0  0  0 0
     * row 1   0 c0 с1 0
     * row 2   0 с2 с3 0 - x
     * row 3   0 c4 с5 0
     * row 4   0  0  0 0
     *            |
     *            y
     *
     * c2 текущая живая клетка
     */
    protected void calcCellStep2(Cell cell,
                                 Set<Cell> current,
                                 Set<Cell> next,
                                 Iterator<Cell> heap)
    {
        RuleInfo currentThreadRI = ruleInfoByThread.get();
        Arrays.fill(currentThreadRI.counts, 0, 6, (short) 1);
        Arrays.fill(currentThreadRI.states, 0, 6, (short) 0);
        currentThreadRI.states[2] = 1;
        currentThreadRI.counts[2] = 0;

        int x = cell.getX();
        int y = cell.getY();

        //row 0
        currentThreadRI.cellTry.set(x-1, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
        }
        currentThreadRI.cellTry.set(x, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[1] += 1;
        }
        //end row 0


        //row 1
        currentThreadRI.cellTry.set(x-1, y-1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x, y-1);
        if (current.contains(currentThreadRI.cellTry)) {//0 enemy
            currentThreadRI.states[0] = 1;

            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[3] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y-1);
        if (current.contains(currentThreadRI.cellTry)) {//1 enemy
            currentThreadRI.states[1] = 1;

            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[3] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y-1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[3] += 1;
        }
        //end row 1


        //row 2
        currentThreadRI.cellTry.set(x-1, y);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[4] += 1;
        }

        //пропускаем x, y т.к. уже её учли
        currentThreadRI.cellTry.set(x+1, y);
        if (current.contains(currentThreadRI.cellTry)) {//3 enemy
            currentThreadRI.states[3] = 1;

            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[5] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[5] += 1;
        }
        //end row 2


        //row 3
        currentThreadRI.cellTry.set(x-1, y+1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[4] += 1;
        }
        currentThreadRI.cellTry.set(x, y+1);
        if (current.contains(currentThreadRI.cellTry)) {//4 enemy
            currentThreadRI.states[4] = 1;

            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[5] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y+1);
        if (current.contains(currentThreadRI.cellTry)) {//5 enemy
            currentThreadRI.states[5] = 1;

            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[4] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y+1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[5] += 1;
        }
        //end row 3


        //row 4
        currentThreadRI.cellTry.set(x-1, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[4] += 1;
        }
        currentThreadRI.cellTry.set(x, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[5] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[5] += 1;
            currentThreadRI.counts[4] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[5] += 1;
        }
        //end row 4

        for (int i = 0; i < 6; i++) {
            if (rule.rule(currentThreadRI.counts[i], currentThreadRI.states[i]) > 0) {
                int index = i+(Math.floorDiv(i, 2)+1);

                fromLocalToGlobal(currentThreadRI.cellTry, index, x, y);
                if (!next.contains(currentThreadRI.cellTry)) {
                    Cell forAdd = null;
                    if (heap.hasNext()) {
                        forAdd = heap.next();
                    }
                    else {
                        forAdd = new Cell();
                    }
                    forAdd.set(currentThreadRI.cellTry);
                    next.add(forAdd);
                }
            }
        }
    }


    /*
     * row 0   0  0  0  0 0
     * row 1   0 с0 с1 с2 0
     * row 2   0 с3 с4 с5 0 - x
     * row 3   0 с6 с7 с8 0
     * row 4   0  0  0  0 0
     *               |
     *               y
     *
     * c4 текущая живая клетка
     */
    protected void calcSingleCell(Cell cell,
                                  Set<Cell> current,
                                  Set<Cell> next,
                                  Iterator<Cell> heap)
    {
        RuleInfo currentThreadRI = ruleInfoByThread.get();

        Arrays.fill(currentThreadRI.counts, (short) 1);
        Arrays.fill(currentThreadRI.states, (short) 0);
        currentThreadRI.states[4] = 1;
        currentThreadRI.counts[4] = 0;

        int x = cell.getX();
        int y = cell.getY();

        //row 0
        currentThreadRI.cellTry.set(x-2, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
        }
        currentThreadRI.cellTry.set(x-1, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
        }
        currentThreadRI.cellTry.set(x, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y-2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
        }
        //end row 0

        //row 1
        currentThreadRI.cellTry.set(x-2, y-1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[3] += 1;
        }
        currentThreadRI.cellTry.set(x-1, y-1);
        if (current.contains(currentThreadRI.cellTry)) {//0 enemy
            currentThreadRI.states[0] = 1;

            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[3] += 1;
        }
        currentThreadRI.cellTry.set(x, y-1);
        if (current.contains(currentThreadRI.cellTry)) {//1 enemy
            currentThreadRI.states[1] = 1;

            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[5] += 1;
            currentThreadRI.counts[2] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y-1);
        if (current.contains(currentThreadRI.cellTry)) {//2 enemy
            currentThreadRI.states[2] = 1;

            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[5] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y-1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[5] += 1;
        }
        //end row 1

        //row 2
        currentThreadRI.cellTry.set(x-2, y);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[6] += 1;
        }
        currentThreadRI.cellTry.set(x-1, y);
        if (current.contains(currentThreadRI.cellTry)) {//3 enemy
            currentThreadRI.states[3] = 1;

            currentThreadRI.counts[0] += 1;
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[7] += 1;
            currentThreadRI.counts[6] += 1;
        }

        //пропускаем 4 клетку т.к. зарание исвестно, что она живая
        currentThreadRI.cellTry.set(x+1, y);
        if (current.contains(currentThreadRI.cellTry)) {//5 enemy
            currentThreadRI.states[5] = 1;

            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[1] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[7] += 1;
            currentThreadRI.counts[8] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[2] += 1;
            currentThreadRI.counts[5] += 1;
            currentThreadRI.counts[8] += 1;
        }
        //end row 2

        //row 3
        currentThreadRI.cellTry.set(x-2, y+1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[6] += 1;
        }
        currentThreadRI.cellTry.set(x-1, y+1);
        if (current.contains(currentThreadRI.cellTry)) {//6 enemy
            currentThreadRI.states[6] = 1;

            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[7] += 1;
        }
        currentThreadRI.cellTry.set(x, y+1);
        if (current.contains(currentThreadRI.cellTry)) {//7 enemy
            currentThreadRI.states[7] = 1;

            currentThreadRI.counts[6] += 1;
            currentThreadRI.counts[3] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[5] += 1;
            currentThreadRI.counts[8] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y+1);
        if (current.contains(currentThreadRI.cellTry)) {//8 enemy
            currentThreadRI.states[8] = 1;

            currentThreadRI.counts[7] += 1;
            currentThreadRI.counts[4] += 1;
            currentThreadRI.counts[5] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y+1);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[5] += 1;
            currentThreadRI.counts[8] += 1;
        }
        //end row 3

        //row 4
        currentThreadRI.cellTry.set(x-2, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[6] += 1;
        }
        currentThreadRI.cellTry.set(x-1, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[6] += 1;
            currentThreadRI.counts[7] += 1;
        }
        currentThreadRI.cellTry.set(x, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[6] += 1;
            currentThreadRI.counts[7] += 1;
            currentThreadRI.counts[8] += 1;
        }
        currentThreadRI.cellTry.set(x+1, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[7] += 1;
            currentThreadRI.counts[8] += 1;
        }
        currentThreadRI.cellTry.set(x+2, y+2);
        if (current.contains(currentThreadRI.cellTry)) {
            currentThreadRI.counts[8] += 1;
        }
        //end row 4
        for (int i = 0; i < currentThreadRI.states.length; i++) {
            if (rule.rule(currentThreadRI.counts[i], currentThreadRI.states[i]) > 0) {
                fromLocalToGlobal(currentThreadRI.cellTry, i, x, y);
                if (!next.contains(currentThreadRI.cellTry)) {
                    Cell forAdd = null;
                    if (heap.hasNext()) {
                        forAdd = heap.next();
//                        currentThreadRI.reused++;
                    }
                    else {
                        forAdd = new Cell();
//                        currentThreadRI.create++;
                    }
                    forAdd.set(currentThreadRI.cellTry);
                    next.add(forAdd);
                }
            }
        }
    }

    /*
     * рассчитывает координаты клетки основываясь на её индексе index и координатах x,y центра блока
     * центр - клетка с индексом 4. Её координаты x,y
     * записывает координаты в переданную клетку
     */
    private void fromLocalToGlobal(Cell cell, int index, int x, int y) {
        int nx = ((index%3)-1)+x;
        int ny = (Math.floorDiv(index, 3)-1)+y;

        cell.set(nx, ny);
    }

}
