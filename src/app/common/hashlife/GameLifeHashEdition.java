package app.common.hashlife;

import app.common.Cell;
import app.common.IGameLife;
import app.common.IRule;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GameLifeHashEdition extends IGameLife {
    Node root;

    public GameLifeHashEdition(int width, int height, IRule rule) {
        super(width, height, rule);

    }

    @Override
    public void change() {

    }

    @Override
    public void calcNextStep() {

    }

    @Override
    public Future[] calcNextStepAsync() throws ExecutionException, InterruptedException {
        return new Future[0];
    }

    @Override
    public void setCurrent(int x, int y) {

    }

    @Override
    public Iterable<Cell> getCurrent() {
        return null;
    }

    @Override
    public boolean isUsedInfinitePlate() {
        return true;
    }
}
