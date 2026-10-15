# Concurrency Fundamentals

The "now make it thread-safe" follow-up is where SDE2 is won or lost. These are the exact tools the `production/` design solutions use.

---

## 1. The race condition (the disease)
A **check-then-act** or **read-modify-write** on shared mutable state, interleaved by threads, loses updates. `count++` is actually read → add → write (3 steps); two threads can both read the same value and one update vanishes. See `RaceCondition.java`.

## 2. `synchronized` — the simplest cure
A monitor lock: only one thread at a time runs a `synchronized` method/block on the same object. Makes a compound action atomic and establishes a happens-before (visibility) edge. Simple, but coarse.

## 3. `ReentrantLock` — explicit lock
Same mutual exclusion as `synchronized`, but with `lock()/unlock()` (always in try/finally), plus `tryLock(timeout)`, interruptibility, and fairness options. Use when you need those features or `Condition` variables.

## 4. `ReadWriteLock` — many readers OR one writer
`ReentrantReadWriteLock` lets many threads read concurrently but gives writers exclusive access. Big win for read-heavy structures (like the Splitwise balance sheet).

## 5. `volatile` — visibility, NOT atomicity
`volatile` guarantees reads/writes of that variable are visible across threads (no stale caches) and not reordered. It does **not** make `count++` atomic. Use for flags (`running`) and the double-checked-locking singleton field.

## 6. Atomics / CAS — lock-free
`AtomicInteger`/`AtomicReference` use **compare-and-set**: "set to new only if the current value is still what I read." The hardware does it atomically. A CAS loop retries on contention. This is how the production solutions claim a spot/seat/driver without a lock.

## 7. `ExecutorService` — don't hand-manage threads
A managed thread pool. Submit `Runnable`/`Callable`; get `Future`s. Bounded pools + bounded queues give backpressure. Always `shutdown()`.

## 8. `BlockingQueue` — producer/consumer
A thread-safe queue where `put` blocks when full and `take` blocks when empty. The backbone of producer/consumer (the logging async appender, the task scheduler). No manual wait/notify.

## 9. Deadlock
Two threads each hold a lock the other needs, and both wait forever. Classic cause: acquiring multiple locks in **different orders**. Fix: a global lock-ordering, or `tryLock` with timeout + backoff. See `Deadlock.java`.

---

## Decision guide
- One variable, simple counter/flag → **atomic** (or volatile for a flag).
- A compound action on a few fields → **synchronized** / **ReentrantLock**.
- Read-heavy shared structure → **ReadWriteLock**.
- Work handed between threads → **BlockingQueue** + **ExecutorService**.
- Lock-free single-slot claim → **CAS** on an atomic.

## Files
`RaceCondition.java`, `Locks.java` (synchronized / ReentrantLock / ReadWriteLock),
`VolatileVsAtomic.java`, `ExecutorDemo.java`, `ProducerConsumer.java`, `Deadlock.java`, `Main.java`.
