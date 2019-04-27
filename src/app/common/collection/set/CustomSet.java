package app.common.collection.set;

import java.util.*;

public class CustomSet<E> implements Set<E> {
    private static final int    DEFAULT_BASKET_COUNT = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    private ArrayList<E>[]      containers;
    private double              loadFactor;
    private int                 size = 0;
    private int                 threshold;

    public CustomSet() {
        loadFactor = DEFAULT_LOAD_FACTOR;
        containers = createContainer(DEFAULT_BASKET_COUNT);
        threshold = (int) (DEFAULT_BASKET_COUNT*loadFactor);
    }
    public CustomSet(int startCapacity) {
        this(startCapacity, DEFAULT_LOAD_FACTOR);
    }
    public CustomSet(double loadFactor) {
        this.loadFactor = loadFactor;
        containers = createContainer(DEFAULT_BASKET_COUNT);
        threshold = (int) (DEFAULT_BASKET_COUNT*loadFactor);
    }
    public CustomSet(int startCapacity, double loadFactor) {
        this.loadFactor = loadFactor;
        int targetCapacity =(int) Math.ceil(startCapacity/loadFactor);//количество элементов с учётом loadFactor
        int targetBasketCount = (int) Math.ceil(targetCapacity/DEFAULT_BASKET_COUNT)*DEFAULT_BASKET_COUNT;
        containers = createContainer(targetBasketCount);
        threshold = startCapacity;
    }

    private ArrayList<E>[] createContainer(int capacity) {
        ArrayList<E>[] res = new ArrayList[capacity];
        for (int i = 0; i < capacity; i++) {
            res[i] = new ArrayList<>();
        }
        return res;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        int targetBasketIndex = defineBasketIndex(o.hashCode());

        ArrayList<E> basket = containers[targetBasketIndex];
        for (int i = 0; i < basket.size(); i++) {
            if (basket.get(i).equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        int firstIndex = containers.length-1;
        ArrayList<E> firstBasket = firstBasket = containers[firstIndex];
        for (int i = 0; i < containers.length; i++) {
            if (containers[i].size() > 0) {
                firstBasket = containers[i];
                firstIndex = i;
                break;
            }
        }


        ArrayList<E> finalFirstBasket = firstBasket;
        int finalFirstIndex = firstIndex;
        return new Iterator<E>() {
            int basketIndex = finalFirstIndex;
            ArrayList<E> basket = finalFirstBasket;
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < basket.size();
            }

            @Override
            public E next() {
                E res = null;
                try {
                    res = basket.get(index);
                }
                catch (Exception e) {
                    throw new NoSuchElementException();
                }

                index++;

                while (index >= basket.size() && basketIndex < containers.length-1) {
                    basketIndex++;
                    index = 0;
                    basket = containers[basketIndex];
                }
                return res;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public boolean add(E e) {
        if (addWithoutResize(e)) {
            size++;
            resize();
            return true;
        }
        return false;
    }

    private boolean addWithoutResize(E e) {
        int hash = e.hashCode();
        int targetBasketIndex = defineBasketIndex(hash);

        ArrayList<E> basket = containers[targetBasketIndex];
        for (int i = 0; i < basket.size(); i++) {
            if (basket.get(i).equals(e)) {
                return false;
            }
        }
        basket.add(e);
        return true;
    }

    private void resize() {
        if (size <= threshold) {
            return;
        }
        int newCapacity = containers.length*2;
        threshold = (int) (newCapacity*loadFactor);

        ArrayList<E>[] lastContainers = containers;
        containers = createContainer(newCapacity);
        for (int i = 0; i < lastContainers.length; i++) {
            ArrayList<E> basket = lastContainers[i];
            for (int j = 0; j < basket.size(); j++) {
                addWithoutResize(basket.get(j));
            }
        }
    }

    private int defineBasketIndex(int hash) {
        return hash & containers.length-1;
    }

    @Override
    public boolean remove(Object o) {
        int hash = o.hashCode();
        int targetBasketIndex = defineBasketIndex(hash);

        ArrayList<E> basket = containers[targetBasketIndex];
        int findIndex = -1;

        for (int i = 0; i < basket.size(); i++) {
            if (basket.get(i).equals(o)) {
                findIndex = i;
                break;
            }
        }
        if (findIndex == -1) { return false; }
        size--;
        E last = basket.remove(basket.size()-1);
        if (findIndex == basket.size()) { return true; }

        basket.set(findIndex, last);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < containers.length; i++) {
            containers[i].clear();
        }
        size = 0;
    }
}
