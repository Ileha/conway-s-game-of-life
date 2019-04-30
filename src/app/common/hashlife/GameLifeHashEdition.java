package app.common.hashlife;

import app.common.Cell;
import app.common.IGameLife;
import app.common.IRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GameLifeHashEdition extends IGameLife {
    private Node root;

    public GameLifeHashEdition(int width, int height, IRule rule) {
        super(width, height, rule);
        int max = Math.max(width, height);
        int level = (int) Math.ceil(Math.log(max)/Math.log(2));
        root = new Node(rule, level);
    }

    @Override
    public void start() {
        root.expandUniverse();
    }

    @Override
    public void change() {

    }

    @Override
    public void calcNextStep() {
        root = root.nextStep();
        root.expandUniverse();
    }

    @Override
    public Future[] calcNextStepAsync() throws ExecutionException, InterruptedException {
        return new Future[0];
    }

    @Override
    public void setCurrent(int x, int y) {
        int max = Math.max(x, y);
        while (root.sideSize() < max) {
            root.expandUniverse();
        }
        root.setCell(x, y);
    }


    @Override
    public Iterable<Cell> getCurrent() {
        return new Iterable<Cell>() {
            ArrayList<Cell> contain = new ArrayList<Cell>();

            {
                nodeIterate(root.centeredSubnode(), 0, 0);
            }

            private void nodeIterate(Node toIterate, int x, int y) {
                if (toIterate.getLevel() == 0) {
                    if (toIterate.getAlive() > 0) { contain.add(new Cell(x, y)); }
                    return;
                }

                int size = toIterate.sideSize()/2;

                nodeIterate(toIterate.leftUpper, x, y);
                nodeIterate(toIterate.rightUpper, x+size, y);
                nodeIterate(toIterate.leftLower, x, y+size);
                nodeIterate(toIterate.rightLower, x+size, y+size);
            }

            @Override
            public Iterator<Cell> iterator() {
                return contain.iterator();
            }
        };
    }

    @Override
    public boolean isUsedInfinitePlate() {
        return true;
    }

    @Override
    public String toString() {
        return root.toString();
    }
}
