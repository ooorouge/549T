package concurrentUtils;

public class Bucket {
    public int key;
    public State state;
    
    public Bucket(int key, State state) {
        this.key = key;
        this.state = state;
    }
}
