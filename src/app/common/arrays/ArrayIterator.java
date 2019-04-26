package app.common.arrays;

import app.common.Cell;

import java.util.Iterator;

/*
* итератор для двумерного массива
*/
public class ArrayIterator implements Iterable<Cell>, Iterator<Cell> {
    private short[][] array;
    private int i = 0;
    private int j = 0;
    private Cell current = new Cell(0, 0);

    public ArrayIterator(short[][] array) {
        this.array = array;
    }

    @Override
    public Iterator<Cell> iterator() {
        i = 0;
        j = 0;
        return this;
    }

    @Override
    public boolean hasNext() {
        int lenght = array[array.length-1].length;
        while (i < array.length-1) {
            j++;
            if (j >= lenght) {
                i++;
                j = 0;
            }

            if (array[i][j] > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Cell next() {
        current.set(j, i);
        return current;
    }
}
