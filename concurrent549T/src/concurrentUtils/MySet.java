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

    // check whether k is a member
    public boolean LookUp(int k) {
        int h = this.hash(k);
        int max = this.GetProbeBound(h);
        for (int i = 0; i <= max; i++) {
            if (this.getBucket(h, i).get().key == k && this.getBucket(h, i).get().state == State.MEMBER) {
                return true;
            }
        }
        return false;
    }

    // insert
    public boolean Insert(int k) {
        int h = this.hash(k);
        // reserve an index for this insert
        int i = 0;
        // CAS compare address rather than value
        Bucket bucketEmpty = new Bucket(-1, State.EMPTY);
        Bucket bucketBusy = new Bucket(-1, State.BUSY);
        while (!(this.getBucket(h, i).get().key == -1 && this.getBucket(h, i).get().state == State.EMPTY)) {
            i++;
            if (i >= this.size) {
                System.out.println("Table full.");
                return false;
            }
        }
        this.getBucket(h, i).set(bucketEmpty);
        while (!this.getBucket(h, i).compareAndSet(bucketEmpty, bucketBusy)) {
            continue;
        }
        // try to insert k (unique)
        Bucket bucketInserting = new Bucket(k, State.INSERTING);
        Bucket bucketMember = new Bucket(k, State.MEMBER);
        do {
            this.getBucket(h, i).set(bucketInserting);
            this.ConditionallyRaiseBound(h, i);
            int max = this.GetProbeBound(h);
            for (int j = 0; j <= max; j++) {
                if (j != i) {
                    // stall concurrent inserts from other threads
                    if (this.getBucket(h, j).get().equals(bucketInserting)) {
                        this.getBucket(h, j).compareAndSet(bucketInserting, bucketBusy);
                    }
                    // if k is already a member, then abort
                    if (this.getBucket(h, j).get().key == bucketMember.key
                            && this.getBucket(h, j).get().state == bucketMember.state) {
                        this.getBucket(h, i).set(bucketBusy);
                        this.ConditionallyLowerBound(h, i);
                        this.getBucket(h, i).set(bucketEmpty);
                        return false;
                    }
                }
            }
        } while (!this.getBucket(h, i).compareAndSet(bucketInserting, bucketMember));
        return true;
    }

    // delete
    public boolean Erase(int k) {
        int h = this.hash(k);
        int max = this.GetProbeBound(h);
        Bucket bucketMember = new Bucket(k, State.MEMBER);
        for (int i = 0; i <= max; i++) {
            if (this.getBucket(h, i).get().key == k && this.getBucket(h, i).get().state == State.MEMBER) {
                this.getBucket(h, i).set(bucketMember);
                if (this.getBucket(h, i).compareAndSet(bucketMember, new Bucket(-1, State.BUSY))) {
                    this.ConditionallyLowerBound(h, i);
                    this.getBucket(h, i).set(new Bucket(-1, State.EMPTY));
                    return true;
                }
            }
        }
        return false;
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
            this.bounds.get(h).compareAndSet(this.bounds.get(h).get(),
                    new Tuple(this.bounds.get(h).get().bound, false));
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

    public AtomicReference<Bucket> getBucket(int h, int index) {
        // return this.buckets.get(h + index * (index + 1) / 2 % this.size).get();
        return this.buckets.get(h + index * (index + 1) / 2 % this.size);
    }

    public boolean DoesBucketContainCollision(int h, int index) {
        Bucket bucket = this.getBucket(h, index).get();
        return (bucket.key != -1 && this.hash(bucket.key) == h);
    }

}