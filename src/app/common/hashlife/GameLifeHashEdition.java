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

        handler = new NodeHandler() {
            @Override
            public short rule(short neiborsCount, short state) {
                return lifeRule.rule(neiborsCount, state);
            }
        };

        Node.setHandler(handler);

        root = handler.getZeroByLevel(level);

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

        shaduleRoot = root;
        root = root.expandUniverse();

        handler.collectAllGarbage(root);//при загрузке от каждой клетки остаётся старый путь до неё
    }

    @Override
    public void change() {
        shaduleRoot = root;
        root = root.expandUniverse();

        handler.nextGeneration();
        handler.collectGarbageIfNeeded(root);
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
        while (root.sideSize() <= max) {
            root = root.relativeExpandUniverse();
        }
        root = root.setCell(x, y);

        handler.collectGarbageIfNeeded(root);//от каждой клетки остаётся старый путь до неё
    }


    @Override
    public Iterable<Cell> getCurrent() {
        return iterator;
    }

    /*
    * рисует дерево напрямую: пустые и невидимые ноды пропускаются,
    * нод размером не больше пикселя рисуется одним пикселем без спуска к клеткам
    * (при сильном отдалении часть точек сдвигается на пиксель относительно рисования по клеткам,
    * точный вариант требует спуска до клеток вдоль границ пикселей и в разы медленнее)
    */
    @Override
    public void render(int[] pixels, int width, int height, double offsetX, double offsetY, double pixelsPerCell) {
        renderNode(shaduleRoot, 0, 0, pixels, width, height, offsetX, offsetY, pixelsPerCell);
    }

    private static void renderNode(Node node, long x, long y, int[] pixels, int width, int height,
                                   double offsetX, double offsetY, double pixelsPerCell) {
        if (node.getAlive() == 0) {
            return;
        }

        long side = node.sideSize();
        double x0 = (x + offsetX) * pixelsPerCell;
        double y0 = (y + offsetY) * pixelsPerCell;
        double x1 = (x + side + offsetX) * pixelsPerCell;
        double y1 = (y + side + offsetY) * pixelsPerCell;

        if (x1 <= 0 || y1 <= 0 || x0 >= width || y0 >= height) {
            return;
        }
        if (node.getLevel() == 0 || side * pixelsPerCell <= 1) {
            fillRect(pixels, width, height, x0, y0, x1, y1);
            return;
        }

        long half = side / 2;
        renderNode(node.leftUpper,  x,        y,        pixels, width, height, offsetX, offsetY, pixelsPerCell);
        renderNode(node.rightUpper, x + half, y,        pixels, width, height, offsetX, offsetY, pixelsPerCell);
        renderNode(node.leftLower,  x,        y + half, pixels, width, height, offsetX, offsetY, pixelsPerCell);
        renderNode(node.rightLower, x + half, y + half, pixels, width, height, offsetX, offsetY, pixelsPerCell);
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
