package app.common;

public class Cell implements Comparable<Cell>  {
    private int x;
    private int y;

    private boolean isChane = true;
    private int hash;

    public Cell() {}

    public Cell(Cell other) {
        x = other.x;
        y = other.y;
        isChane = other.isChane;
        hash = other.hash;
    }

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void set(int x, int y) {
        isChane = true;
        this.x = x;
        this.y = y;
    }
    public void set(Cell other) {
        isChane = other.isChane;
        hash = other.hash;
        x = other.x;
        y = other.y;
    }

    @Override
    public int hashCode() {
        if (isChane) {
            hash = CalcHash();
            isChane = false;
        }

        return hash;
    }

    private int CalcHash() {
        /*
            y = 35
            x = 15

            y*100+15
        */


        int k = 10;
        int sign = 1;
        if (x<0 ^ y<0) { sign = -1; }

        int nx = Math.abs(x);
        int ny = Math.abs(y);

        for (; k <= nx; k*=10) {}
        return sign*(ny*k+nx);

//        return x*31 + y;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) { return false; }
        if (this == o) { return true; }
        if (!(o instanceof Cell)) { return false; }
        Cell cell = (Cell) o;

        return cell.x == x && cell.y == y;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", x, y);
    }

    @Override
    public int compareTo(Cell o) {
        if (o.y != y) {
            return clamp(y - o.y);
        }
        else if (o.x != x) {
            return clamp(x - o.x);
        }
        else {
            return 0;
        }
    }

    private int clamp(int num) {
        if (num > 0) {
            return 1;
        }
        else if (num < 0) {
            return -1;
        }
        else {
            return 0;
        }
    }
}
