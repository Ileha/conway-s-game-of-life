package app.common.collection;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class GeneratorIterator<T> implements Iterable<T> {
    private ArrayList<T> container;

    public GeneratorIterator() {
        container = new ArrayList<T>();
    }
    public GeneratorIterator(int startCapacity) {
        container = new ArrayList<T>(startCapacity);
    }

    public abstract T getNewElemet();

    public void add(T element) {
        container.add(element);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Iterator<T> containersIterator = container.iterator();
            private boolean lock = false;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                if (!lock && containersIterator.hasNext()) {
                    return containersIterator.next();
                }
                else {
                    lock = true;
                    T next = getNewElemet();
                    container.add(next);
                    return next;
                }
            }
        };
    }
}
