package app.common.collection.hashlife;

import app.common.Cell;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class NodeIterator implements Iterable<Cell>, Iterator<Cell> {
    private ArrayList<Cell> contain = new ArrayList<Cell>();
    private int index = 0;
    private Iterator<Cell> currentIterator;

    public abstract Node getTarget();

    @Override
    public Iterator<Cell> iterator() {
        index = 0;
        nodeIterate(getTarget(), 0, 0);
        currentIterator = contain.iterator();
        return this;
    }

    private Cell nextCell() {
        if (index < contain.size()-1) {
            index++;
            return contain.get(index);
        }
        else {
            index++;
            Cell res = new Cell();
            contain.add(res);
            return res;
        }
    }

    private void nodeIterate(Node toIterate, int x, int y) {
        if (toIterate.getAlive() == 0) { return; }

        if (toIterate.getLevel() == 0) {
            Cell forAdd = nextCell();
            forAdd.set(x, y);
            return;
        }

        int size = toIterate.sideSize()/2;

        nodeIterate(toIterate.leftUpper, x, y);
        nodeIterate(toIterate.rightUpper, x+size, y);
        nodeIterate(toIterate.leftLower, x, y+size);
        nodeIterate(toIterate.rightLower, x+size, y+size);
    }

    @Override
    public boolean hasNext() {
        return index > 0;
    }

    @Override
    public Cell next() {
        index--;
        return currentIterator.next();
    }
}
