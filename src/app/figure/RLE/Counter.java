package app.figure.RLE;

public class Counter {
    private static final    int MIN_COUNT = 1;

    private                 int count;

    public Counter() {
        count = 0;
    }

    public void reset() {
        count = 0;
    }

    public int getCountAndReset() {
        int res = getCount();
        reset();
        return res;
    }

    public int getCount() {
        return Math.max(count, MIN_COUNT);
    }

    public void appendNum(char num) {
        appendNum(Character.getNumericValue(num));
    }
    public void appendNum(int num) {
        count = count*10+num;
    }
}
