package concurrentUtils;

public class Tuple {
    public int bound;
    public boolean scanning;

    public Tuple(int bound, boolean scanning) {
        this.bound = bound;
        this.scanning = scanning;
    }
}
