package conc;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Three ways to achieve mutual exclusion, side by side.
 */
public class Locks {

    // --- 1. synchronized: the monitor lock on `this` (simplest) ---
    static class SyncCounter {
        private int count = 0;
        synchronized void increment() { count++; } // atomic compound action
        synchronized int get() { return count; }
    }

    // --- 2. ReentrantLock: explicit lock/unlock, always in try/finally ---
    static class LockCounter {
        private int count = 0;
        private final ReentrantLock lock = new ReentrantLock();
        void increment() {
            lock.lock();
            try { count++; }
            finally { lock.unlock(); } // unlock in finally so an exception can't leak the lock
        }
        int get() {
            lock.lock();
            try { return count; } finally { lock.unlock(); }
        }
    }

    // --- 3. ReadWriteLock: many readers OR one writer (read-heavy win) ---
    static class RwBox {
        private int value = 0;
        private final ReadWriteLock rw = new ReentrantReadWriteLock();
        void write(int v) {
            rw.writeLock().lock();
            try { value = v; } finally { rw.writeLock().unlock(); }
        }
        int read() {
            rw.readLock().lock(); // multiple threads can hold the read lock at once
            try { return value; } finally { rw.readLock().unlock(); }
        }
    }

    static void demo() throws InterruptedException {
        SyncCounter sc = new SyncCounter();
        LockCounter lc = new LockCounter();
        Runnable incSync = () -> { for (int i = 0; i < 100_000; i++) sc.increment(); };
        Runnable incLock = () -> { for (int i = 0; i < 100_000; i++) lc.increment(); };

        Thread a = new Thread(incSync), b = new Thread(incSync);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("synchronized counter (want 200000): " + sc.get());

        Thread c = new Thread(incLock), d = new Thread(incLock);
        c.start(); d.start(); c.join(); d.join();
        System.out.println("ReentrantLock counter (want 200000): " + lc.get());

        RwBox box = new RwBox();
        box.write(42);
        System.out.println("ReadWriteLock value: " + box.read());
    }
}
