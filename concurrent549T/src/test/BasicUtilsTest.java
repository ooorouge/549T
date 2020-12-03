package test;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import concurrentUtils.*;

public class BasicUtilsTest {
    private MySet mySet;
    private int size = 100;

    @Before
    public void setup() {
        this.mySet = new MySet(this.size);
    }

    @Test
    public void testInit() {
        mySet.InitProbeBound(5);
    }

    @Test
    public void testRaiseBound() {
        int oldBound = mySet.bounds.get(2).get().bound;
        mySet.ConditionallyRaiseBound(2, 5);
        int newBound = mySet.bounds.get(2).get().bound;
    
        assertTrue(newBound > oldBound);
    }

    @Test
    public void testLowerBound() {
        mySet.ConditionallyRaiseBound(2, 8);
        int oldBound = mySet.bounds.get(2).get().bound;
        mySet.ConditionallyLowerBound(2, 8);
        int newBound = mySet.bounds.get(2).get().bound;

        assertTrue(newBound < oldBound);
    }
}
