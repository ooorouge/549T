package concurrentUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class MySet {
    // Using my set to avoid confusing with standard set
    public int size;
    public ArrayList<AtomicReference<Tuple>> bounds;
    public ArrayList<AtomicReference<Bucket>> buckets;

    // The init
    public MySet(int size) {
        this.size = size;
        this.bounds = new ArrayList<AtomicReference<Tuple>>(size);
        this.buckets = new ArrayList<AtomicReference<Bucket>>(size);
        for (int i = 0; i < size; ++i) {
            this.bounds.add(new AtomicReference<Tuple>(new Tuple(0, false)));
            this.buckets.add(new AtomicReference<Bucket>(new Bucket(-1, State.EMPTY)));
        }
    }

    public int hash(int k) {
        return k % this.size;
    }

    public void InitProbeBound(int h) {
        // No necessay to add index checking, cause set() already did.
        this.bounds.set(h, new AtomicReference<Tuple>(new Tuple(0, false)));
    }

    public int GetProbeBound(int h) {
        // The same
        return this.bounds.get(h).get().bound;
    }

    public void ConditionallyRaiseBound(int h, int index) {
        int newBound = Math.max(this.bounds.get(h).get().bound, index);
        while (!this.bounds.get(h).compareAndSet(this.bounds.get(h).get(), new Tuple(newBound, false))) {
            continue;
        }
    }

    public void ConditionallyLowerBound(int h, int index) {
        boolean boundEqualsToIndex = false;
        Tuple indexFalse = new Tuple(index, false);
        Tuple indexTrue = new Tuple(index, true);

        if (this.bounds.get(h).get().scanning) {
            this.bounds.get(h).compareAndSet(this.bounds.get(h).get(), new Tuple(this.bounds.get(h).get().bound, false));
        }

        // Avoid compareAndSet compare memory location rathre than value
        if (this.bounds.get(h).get().bound == index) {
            boundEqualsToIndex = true;
            if (this.bounds.get(h).get().scanning == false) {
                this.bounds.get(h).set(indexFalse);
            } else {
                this.bounds.get(h).set(indexTrue);
            }
        }

        if (index > 0) {
            // A problem may occur here, how does CAS compare two objects.
            while (boundEqualsToIndex && this.bounds.get(h).compareAndSet(indexFalse, indexTrue)) {
                int _i = index - 1;
                while (_i > 0 && !this.DoesBucketContainCollision(h, _i)) {
                    _i--;
                }
                this.bounds.get(h).compareAndSet(indexTrue, new Tuple(_i, false));
            }
        }
    }

    public Bucket getBucket(int h, int index) {
        return this.buckets.get(h + index*(index+1)/2 % this.size).get();
    }

    public boolean DoesBucketContainCollision(int h, int index) {
        Bucket bucket = this.getBucket(h,index);
        return (bucket.key != -1 && this.hash(bucket.key) == h);
    }

}