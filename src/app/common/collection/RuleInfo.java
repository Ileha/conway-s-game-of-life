package app.common.collection;

import app.common.Cell;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class RuleInfo {
    short[] counts = new short[9];
    short[] states = new short[9];

    Cell cellTry = new Cell();
    Future<Boolean> tasks[] = new Future[25];

    int lx;
    int ly;

    public void resetLastValues() {
        ly = Integer.MIN_VALUE;
        lx = Integer.MIN_VALUE;
    }
}
