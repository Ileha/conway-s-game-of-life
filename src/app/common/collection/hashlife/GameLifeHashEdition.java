package app.common.collection.hashlife;

import app.common.Cell;
import app.common.IGameLife;
import app.common.IRule;
import app.common.collection.GeneratorIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GameLifeHashEdition extends IGameLife {
    private Node                    root;
    private NodeHandler             handler;
    private NodeIterator            iterator;

    public GameLifeHashEdition(int width, int height, IRule lifeRule) {
        super(width, height, lifeRule);
        int max = Math.max(width, height);
        int level = (int) Math.ceil(Math.log(max)/Math.log(2));
        root = Node.createEmptyNode(level);
        handler = new NodeHandler() {
            @Override
            public short rule(short neiborsCount, short state) {
                return lifeRule.rule(neiborsCount, state);
            }
        };
        Node.setHandler(handler);
        iterator = new NodeIterator() {
            @Override
            public Node getTarget() {
                return root;
            }
        };
    }

    @Override
    public void start() {
        System.out.println(root.sideSize());
        root = Node.hashAll(root);
        root.expandUniverse();
    }

    @Override
    public void change() {
        handler.printCashSize();
        root.expandUniverse();
    }

    @Override
    public void calcNextStep() {
        root = root.nextStep();
    }

    @Override
    public Future[] calcNextStepAsync() throws ExecutionException, InterruptedException {
        return new Future[0];
    }

    @Override
    public void setCurrent(int x, int y) {
        int max = Math.max(x, y);
        while (root.sideSize() < max) {
            root.relativeExpandUniverse();
        }
        root.setCell(x, y);
    }


    @Override
    public Iterable<Cell> getCurrent() {
        return iterator;
    }

    @Override
    public boolean isUsedInfinitePlate() {
        return true;
    }

    /*@Override
    public String toString() {
        return root.toString();
    }*/
}
