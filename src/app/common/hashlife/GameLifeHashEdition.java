package app.common.hashlife;

import app.common.Cell;
import app.common.IGameLife;
import app.common.IRule;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GameLifeHashEdition extends IGameLife {
    private Node                root;
    private Node                shaduleRoot;
    private NodeHandler         handler;
    private NodeIterator        iterator;

    private Future              tasks[];
    private ExecutorService     executor;

    public GameLifeHashEdition(int width, int height, IRule lifeRule) {
        super(width, height, lifeRule);
        int max = Math.max(width, height);
        int level = (int) Math.ceil(Math.log(max)/Math.log(2));
        root = Node.createEmptyNode((short) level);

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
                return shaduleRoot;
            }
        };

        executor = Executors.newSingleThreadExecutor();
        tasks = new Future[1];
    }

    @Override
    public void start() {
        System.out.println(root.sideSize());
        root = Node.hashAll(root);


        shaduleRoot = Node.uniteAll(root.leftUpper, root.rightUpper,
                                    root.leftLower, root.rightLower);
        root.expandUniverse();
    }

    @Override
    public void change() {
        handler.printCashSize();

        shaduleRoot = Node.uniteAll(root.leftUpper, root.rightUpper,
                                    root.leftLower, root.rightLower);
        root.expandUniverse();
    }

    @Override
    public void calcNextStep() {
        root = root.nextStep();
    }

    @Override
    public Future[] calcNextStepAsync() throws ExecutionException, InterruptedException {
        tasks[0] = executor.submit(() -> {
            root = root.nextStep();
        });

        return tasks;
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
