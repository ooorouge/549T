package test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

import concurrentUtils.*;

public class FunctionTest {
    private MySet mySet;
    private int size = 100;

    @Before
    public void setup() {
        this.mySet = new MySet(this.size);
    }

    @Test
    public void testInsert() {
        assertTrue(this.mySet.Insert(2));
        assertFalse(this.mySet.Insert(2));
    }

    @Test
    public void testErase() {
        assertFalse(this.mySet.Erase(2));
        this.mySet.Insert(2);
        assertTrue(this.mySet.Erase(2));
    }

    @Test
    public void testLookup() {
        assertFalse(this.mySet.LookUp(2));
        this.mySet.Insert(2);
        assertTrue(this.mySet.LookUp(2));
    }
}