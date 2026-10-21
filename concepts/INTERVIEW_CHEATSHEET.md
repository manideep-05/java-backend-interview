# SDE-2 Concurrency & Locking Interview Master Cheatsheet

This cheatsheet contains **35 high-yield, deeply technical interview questions with production-grade answers, edge cases, and interviewer follow-ups**.

---

## Table of Contents
1. [Module 1: JVM Concurrency & In-Memory Locks (Q1 - Q10)](#module-1-jvm-concurrency--in-memory-locks)
2. [Module 2: Database Locking & Transaction Isolation (Q11 - Q20)](#module-2-database-locking--transaction-isolation)
3. [Module 3: Distributed Locks & Consensus (Q21 - Q28)](#module-3-distributed-locks--consensus)
4. [Module 4: System Design Scenarios & High-Scale Edge Cases (Q29 - Q35)](#module-4-system-design-scenarios--high-scale-edge-cases)

---

## Module 1: JVM Concurrency & In-Memory Locks

### Q1: How does `synchronized` work under the hood in the HotSpot JVM?
**Answer:**
Every Java object header contains a **Mark Word** (64 bits on 64-bit systems). The JVM implements a lock inflation hierarchy:
1. **Biased Locking (Historically enabled, disabled/deprecated in JDK 15+):** Optimizes for single-threaded usage by storing the Thread ID in the Mark Word without CAS.
2. **Lightweight Locking (Thin Lock):** When a second thread attempts acquisition, biased lock is revoked. The thread allocates a BasicObjectLock in its execution stack frame and uses an atomic CAS to swap the Mark Word with a pointer to its stack frame. If contention occurs, it spins briefly (adaptive spinning).
3. **Heavyweight Locking (Fat Lock):** If contention persists beyond the spin threshold, the lock inflates to an OS-level Mutex (ObjectMonitor). The Mark Word points to the `ObjectMonitor` structure in C++ heap. Contending threads are moved to the `_cxq` or `_EntryList` wait-sets and **parked** by the OS kernel, incurring context switch latency (~1-5 µs).

---

### Q2: Why should you avoid synchronizing on `this` or a public string literal in Java?
**Answer:**
1. **Synchronizing on `this`:** Exposes the lock reference to external callers. Any third-party code or client holding a reference to your object can write `synchronized(yourObject)` and hold it indefinitely, starving your internal methods and causing Denial of Service (DOS) or untraceable deadlocks.
2. **Synchronizing on String Literals (e.g. `synchronized("LOCK")`):** String literals are interned in the JVM String Pool (`String.intern()`). Two completely unrelated modules or libraries synchronizing on `"LOCK"` will share the exact same monitor instance, resulting in cross-system thread contention and catastrophic deadlocks.
**Production Rule:** Always lock on a `private final Object lock = new Object();`.

---

### Q3: What is AbstractQueuedSynchronizer (AQS) and how does `ReentrantLock` use it?
**Answer:**
AQS is the foundational framework in `java.util.concurrent.locks`. It provides:
1. A single `volatile int state` variable modified exclusively via atomic CAS (`compareAndSetState`).
2. A doubly-linked FIFO queue of waiting threads based on a variant of the **CLH lock queue**.
3. Thread parking/unparking primitives via `LockSupport.park()` and `LockSupport.unpark()`.

In `ReentrantLock`:
- `state == 0` means unlocked.
- `state > 0` represents the lock hold count (reentrancy counter).
- When `lock()` is called, the current thread attempts `compareAndSetState(0, 1)`. If already owned by `Thread.currentThread()`, it increments `state++`. If owned by another thread, the current thread is wrapped in a `Node`, enqueued into the CLH wait queue, and parked.

---

### Q4: What is the difference between Fair and Unfair `ReentrantLock`? Why is Unfair the default?
**Answer:**
- **Fair Lock (`new ReentrantLock(true)`):** Checks `hasQueuedPredecessors()` before attempting CAS. If threads are already waiting in the AQS queue, the incoming thread enqueues itself at the tail. Guarantees strict FIFO order.
- **Unfair Lock (`new ReentrantLock(false)`):** The incoming thread attempts an immediate CAS to acquire the lock ("barging"), completely ignoring the queue. Only if CAS fails does it enqueue.
- **Why Unfair is Default:** Unparking a parked OS thread incurs a scheduler delay (often 10–50 microseconds). In an unfair lock, an active thread currently executing on CPU can grab the lock immediately, execute its critical section, and release it *before* the unparked thread even wakes up. This delivers **2x to 10x higher throughput**.

---

### Q5: Why must `lock.lock()` be called OUTSIDE the `try` block, while `lock.unlock()` is in `finally`?
**Answer:**
```java
// CORRECT
lock.lock();
try {
    doWork();
} finally {
    lock.unlock();
}
```
If `lock.lock()` is placed inside the `try` block:
```java
// WRONG!
try {
    lock.lock(); // If this throws an OutOfMemoryError or InterruptedException
    doWork();
} finally {
    lock.unlock(); // Attempting to unlock a lock never acquired!
}
```
If an exception (e.g. `Error` / `OutOfMemoryError`) is thrown during `lock()`, the `finally` block executes and calls `unlock()`. Because the thread does NOT hold the lock, it throws an `IllegalMonitorStateException`, which **masks and swallows the original root-cause exception**, making production debugging nearly impossible.

---

### Q6: How does `ReentrantReadWriteLock` handle lock downgrading and lock upgrading?
**Answer:**
- **Lock Downgrading is SUPPORTED:**
  Acquire Write Lock $\rightarrow$ Acquire Read Lock $\rightarrow$ Release Write Lock $\rightarrow$ Do work under Read Lock $\rightarrow$ Release Read Lock.
  *Why?* It guarantees that no other thread can sneak in and modify the protected resource between when you finished writing and when you start reading the updated state.
- **Lock Upgrading is NOT SUPPORTED (Forbidden):**
  A thread holding a Read Lock cannot directly acquire a Write Lock without releasing the Read Lock first.
  *Why?* If Thread 1 and Thread 2 both hold Read Locks and both attempt to upgrade to a Write Lock, both block waiting for the other's Read Lock to release, creating an instantaneous, irrecoverable **deadlock**.

---

### Q7: What is `StampedLock` and what problem in `ReentrantReadWriteLock` does it solve?
**Answer:**
`ReentrantReadWriteLock` suffers from two critical flaws:
1. **Write Starvation:** If there is a continuous stream of reader threads, writers can starve indefinitely.
2. **CAS Contention on Reads:** Even readers must atomically modify the shared 32-bit AQS state via CAS, invalidating CPU cache lines across cores.

`StampedLock` (Java 8) introduces **Optimistic Reading (`tryOptimisticRead()`):**
- It returns a version stamp **without acquiring any lock and without modifying state** (zero CPU bus invalidation).
- The reader reads fields into local variables, then calls `validate(stamp)`.
- If `validate()` is true, no write occurred $\rightarrow$ read was consistent!
- If false, a writer modified data $\rightarrow$ reader safely falls back to a pessimistic read lock (`readLock()`).

---

### Q8: What are the two fatal gotchas of `StampedLock` that every SDE-2 must know?
**Answer:**
1. **It is NOT REENTRANT:** Calling `writeLock()` twice on the same thread will cause the thread to **deadlock on itself**!
2. **Interruptibility Bug:** Interrupting a thread blocked in `StampedLock.readLock()` or `writeLock()` can cause the thread to enter an infinite CPU spin loop consuming 100% CPU. If interruption is required, you must explicitly use `readLockInterruptibly()` or `writeLockInterruptibly()`.
3. **No Condition support:** It does not implement the `Lock` interface and does not support `.newCondition()`.

---

### Q9: Why does `AtomicLong` degrade under extreme multi-core contention, and how does `LongAdder` solve it?
**Answer:**
- **The Bottleneck in `AtomicLong`:** Uses a single `volatile long value` updated via atomic CAS (`compareAndSet`). Under high contention across 32 or 64 CPU cores, all cores attempt to CAS the same 8-byte cache line simultaneously. Due to the **MESI cache coherency protocol**, each successful CAS invalidates the L1/L2 cache lines of all other cores, creating memory bus saturation and a "CAS spin storm" with near-zero throughput.
- **The Fix in `LongAdder`:** Uses **Lock Striping / Striped64**:
  - Maintains a base value plus an internal array of `Cell` objects.
  - Contending threads hash to different cells based on thread ID probes.
  - Each `Cell` is annotated with `@jdk.internal.vm.annotation.Contended` to pad the object to 64 bytes (the CPU cache line size), completely eliminating **False Sharing**.
  - Final value is aggregated via `sum()`. Throughput scales linearly with CPU core count.

---

### Q10: What is False Sharing in multi-threaded CPU architectures?
**Answer:**
Modern CPUs read and write memory in chunks of **64 bytes (Cache Lines)**.
If Thread 1 updates variable `A` and Thread 2 updates variable `B`, and both `A` and `B` happen to reside within the *same* 64-byte memory chunk:
- Whenever Thread 1 writes to `A`, the CPU invalidates the entire 64-byte cache line in Thread 2's L1/L2 cache.
- Even though `A` and `B` are completely independent variables in software, hardware cache coherency treats them as dirty, forcing Thread 2 to re-fetch from L3 cache or RAM.
- **Solution:** Add cache-line padding (e.g. 7 unused `long` variables around the field, or `@Contended`).

---

## Module 2: Database Locking & Transaction Isolation

### Q11: Explain the difference between Pessimistic Locking and Optimistic Locking at the Database level.
**Answer:**
| Dimension | Optimistic Locking | Pessimistic Locking |
| :--- | :--- | :--- |
| **Philosophy** | "Conflicts are rare; detect them at commit time." | "Conflicts are likely; lock early to block others." |
| **Mechanism** | `@Version` column; `UPDATE ... WHERE id=? AND version=?` | `SELECT ... FOR UPDATE` row-level exclusive lock |
| **Lock Held Duration** | Zero locks held during processing; CAS at commit. | Locked from `SELECT` until `COMMIT` or `ROLLBACK`. |
| **Throughput** | High for read-heavy / low-write collision. | Low-medium; serializes concurrent transactions on row. |
| **High Contention Behavior** | Severe retry storm; high abort rate; wastes DB CPU. | Queues requests; latency increases; no wasted retries. |

---

### Q12: What is the difference between `PESSIMISTIC_WRITE` and `PESSIMISTIC_READ` in JPA?
**Answer:**
- **`PESSIMISTIC_WRITE`:** Issues `SELECT ... FOR UPDATE`.
  - Places an **Exclusive (X) lock** on the row.
  - Prevents any other transaction from reading with `FOR UPDATE` / `FOR SHARE` or modifying the row.
- **`PESSIMISTIC_READ`:** Issues `SELECT ... FOR SHARE` (Postgres) or `LOCK IN SHARE MODE` (MySQL).
  - Places a **Shared (S) lock** on the row.
  - Allows other concurrent transactions to read the row (and acquire shared locks), but **blocks any transaction from modifying the row** until all shared locks are released.

---

### Q13: What happens if you run `SELECT ... FOR UPDATE` on a column without an index?
**Answer:**
**Catastrophic Table Lock.**
In relational databases (like MySQL InnoDB):
- InnoDB locks records by locking the **index entries**.
- If your `WHERE` condition uses an indexed column, InnoDB acquires row locks (record locks and next-key locks) specifically on that index.
- If the column is **not indexed**, InnoDB cannot locate the specific index record; it must perform a full table scan and **locks every single record in the entire table (table-level lock)**.
- Any other concurrent query trying to insert, update, or read with `FOR UPDATE` on *any* row in that table will block!

---

### Q14: How does `SELECT ... FOR UPDATE SKIP LOCKED` work, and why is it used for job queues?
**Answer:**
- **Normal `FOR UPDATE`:** Blocks until the active lock holder commits.
- **`NOWAIT`:** Throws an immediate error (`could not obtain lock on row`) if locked.
- **`SKIP LOCKED`:** When scanning candidate rows matching the `WHERE` clause, if the database encounters a row locked by another active transaction, it simply **skips past it** and returns the next unlocked row.
- **Why it's ideal for Job Queues:**
  If 10 background worker instances execute:
  `SELECT * FROM tasks WHERE status = 'PENDING' LIMIT 1 FOR UPDATE SKIP LOCKED;`
  Worker 1 locks Task 1. Worker 2 skips Task 1 and immediately locks Task 2. Worker 3 immediately locks Task 3.
  All 10 workers process tasks in parallel without blocking, without contention, and without needing an external message broker like RabbitMQ or Kafka.

---

### Q15: What are the 4 Coffman conditions for Deadlocks, and how do you eliminate them in code?
**Answer:**
A deadlock can occur *if and only if* all 4 conditions hold simultaneously:
1. **Mutual Exclusion:** At least one resource is held in a non-shareable mode.
2. **Hold and Wait:** A process holds a resource while waiting to acquire another.
3. **No Preemption:** Resources cannot be forcibly taken away from the holder.
4. **Circular Wait:** Process $P_1$ waits for $P_2$, which waits for $P_3$, ..., which waits for $P_1$.

**The Production Remedy:** Break **Circular Wait** by enforcing a **Global Deterministic Lock Order**.
For example, when transferring between two bank accounts ($A$ and $B$), always acquire locks in sorted order of Account ID:
```java
Long first = Math.min(idA, idB);
Long second = Math.max(idA, idB);
lock(first);
lock(second);
```
Since every thread in the cluster requests locks in the exact same numerical sequence, a circular wait is mathematically impossible.

---

### Q16: What is the "Lost Update" anomaly and how do locks prevent it?
**Answer:**
- **The Anomaly:**
  1. Transaction 1 reads stock = 10.
  2. Transaction 2 reads stock = 10.
  3. Transaction 1 deducts 2 and writes stock = 8.
  4. Transaction 2 deducts 3 and writes stock = 7 (overwriting Transaction 1's write!).
  - Result: 5 items were sold, but stock is recorded as 7 instead of 5. Transaction 1's update was lost.
- **Prevention:**
  1. **Optimistic Locking:** Transaction 2's commit fails because the version changed from 1 to 2.
  2. **Pessimistic Locking:** Transaction 2 cannot read stock until Transaction 1 commits.
  3. **Atomic SQL:** `UPDATE inventory SET stock = stock - 2 WHERE id = 1 AND stock >= 2;` (DB engine serializes row update).

---

### Q17: What is Phantom Read and how does Next-Key Locking in MySQL InnoDB prevent it?
**Answer:**
- **Phantom Read:** Transaction 1 queries `SELECT * FROM orders WHERE user_id = 5` and gets 2 rows. Transaction 2 inserts a 3rd order for `user_id = 5` and commits. Transaction 1 re-runs the same query and sees 3 rows (a "phantom" row appeared).
- **InnoDB Solution (Next-Key Lock):**
  At `REPEATABLE READ` isolation, InnoDB uses **Next-Key Locks** (combination of Record Lock + Gap Lock).
  When Transaction 1 runs `SELECT ... FOR UPDATE WHERE user_id = 5`, InnoDB locks:
  1. The existing index records where `user_id = 5`.
  2. The **gaps** before, between, and after those records in the index tree.
  If Transaction 2 tries to `INSERT` a new row into that gap, it is blocked until Transaction 1 commits.

---

### Q18: Why is making an external HTTP/REST call inside a `@Transactional` block holding a Pessimistic Lock considered a critical production anti-pattern?
**Answer:**
1. A pessimistic lock holds an exclusive database row lock and keeps a database connection checked out from HikariCP.
2. An external HTTP call depends on third-party network latency, gateway timeouts, or outages (e.g. 5–30 seconds timeout).
3. If the payment gateway stalls for 10 seconds:
   - The database row remains locked for 10 seconds, blocking all other customers.
   - HikariCP connection remains tied up for 10 seconds.
   - Incoming web requests exhaust the connection pool in under 1 second.
   - The entire microservice stops responding to all health checks and crashes.
**Golden Rule:** Fetch data $\rightarrow$ Release lock $\rightarrow$ Perform HTTP call $\rightarrow$ Open new transaction to record result.

---

### Q19: How should you design retry mechanisms for `@Version` OptimisticLockException?
**Answer:**
Use **Exponential Backoff with Full Jitter**:
```java
@Retryable(
    retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
    maxAttempts = 4,
    backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 500, random = true)
)
```
- `delay = 50ms, multiplier = 2.0`: Increases wait time exponentially (50ms, 100ms, 200ms).
- `random = true` (Full Jitter): Spreads out retries evenly across a random window. Without jitter, all contending threads that failed together will retry at the exact same millisecond, creating a synchronized "Retry Wave" that collides repeatedly.

---

### Q20: What is the difference between Database Transaction Isolation Levels and Locking?
**Answer:**
- **Isolation Levels (SQL Standard):** Define what phenomena (Dirty Read, Non-Repeatable Read, Phantom Read) are permissible.
  1. `READ UNCOMMITTED`
  2. `READ COMMITTED` (Postgres default; reads see latest committed snapshot)
  3. `REPEATABLE READ` (MySQL default; reads see snapshot at start of transaction)
  4. `SERIALIZABLE` (Full strict isolation)
- **Locking:** The *underlying implementation mechanism* used by database engines (alongside MVCC - Multi-Version Concurrency Control) to enforce those isolation guarantees. MVCC provides non-blocking reads (readers don't block writers, writers don't block readers), whereas explicit locks (`FOR UPDATE`) force pessimistic serialization.

---

## Module 3: Distributed Locks & Consensus

### Q21: Why is raw `SETNX` followed by `EXPIRE` unsafe for distributed locking in Redis?
**Answer:**
Because they are **two separate commands executed non-atomically**.
If the application crashes, gets killed, or network severs immediately after `SETNX key value` and *before* `EXPIRE key 30`, the key has **no TTL**. It will live in Redis permanently, locking the resource forever and requiring manual operational intervention to recover.
**Fix:** Always use the single atomic command:
`SET key value NX PX 30000` (supported natively in Redis).

---

### Q22: Why can't you simply call `DEL lock_key` when releasing a Redis distributed lock?
**Answer:**
The **Premature Expiration / Lock Hijacking Bug**:
1. Client 1 acquires lock with 10s TTL.
2. Client 1 experiences a 12-second Stop-The-World (STW) JVM GC pause or network delay.
3. At 10s, Redis expires Client 1's lock.
4. Client 2 acquires the lock with its own 10s TTL.
5. Client 1 wakes up from GC, completes its work, and blindly calls `DEL lock_key`.
6. **Client 1 just deleted Client 2's lock!**
7. Now Client 3 can also acquire the lock while Client 2 is still executing, destroying mutual exclusion.
**Fix:** Store a unique identifier (UUID + Thread ID) as the lock value, and release exclusively via an **atomic Lua script** that verifies ownership:
```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

---

### Q23: How does Redisson's Watchdog mechanism work, and why does it prevent premature expiration?
**Answer:**
If you acquire a lock without specifying an explicit `leaseTime` (e.g. `lock.tryLock(waitTime, TimeUnit.SECONDS)`):
1. Redisson sets an initial 30-second TTL on the Redis lock key (`lockWatchdogTimeout`).
2. Redisson launches an internal background timer task (Netty `Timeout`).
3. Every 10 seconds (`lockWatchdogTimeout / 3`), the timer runs a Lua script that renews the key's TTL back to 30 seconds as long as the holding thread is still alive.
4. **Crash Safety:** If the JVM process crashes, the watchdog timer dies. Within 30 seconds, Redis automatically expires the key, guaranteeing no permanent deadlocks!

---

### Q24: What is the Redlock algorithm and what was Martin Kleppmann's famous critique of it?
**Answer:**
- **Redlock (Salvatore Sanfilippo / Redis creator):**
  Uses $N$ independent Redis masters (typically 5). A client tries to acquire the lock in all 5 instances sequentially with a small timeout. If it acquires the lock in the majority ($\ge 3$ out of 5) within a time frame smaller than the lock validity time, the lock is acquired.
- **Martin Kleppmann's Critique:**
  1. **Clock Drift:** Redlock relies on physical system clocks across nodes. NTP clock jumps can cause leases to expire prematurely.
  2. **Process Pauses:** An unexpected Stop-The-World GC pause can occur *after* acquiring the majority of locks but *before* writing to storage. By the time the thread wakes up, its lease has expired, another node has acquired the lock, and both write concurrently.
  3. **Conclusion:** For performance / non-critical deduplication, standard single-instance / sentinel Redis with watchdog is fine. For mission-critical financial safety, a distributed lock *alone* cannot guarantee safety without **Fencing Tokens**.

---

### Q25: What is a Fencing Token and how does it guarantee data safety against paused clients?
**Answer:**
A Fencing Token is a **strictly monotonically increasing integer** issued by the lock service whenever a lock is granted (e.g. Token 101, 102, 103...).
1. Client 1 acquires lock $\rightarrow$ receives Fencing Token `101`.
2. Client 1 goes into an extended 20-second GC pause.
3. Lock expires; Client 2 acquires lock $\rightarrow$ receives Fencing Token `102`.
4. Client 2 writes to Database:
   `UPDATE data SET value = 'B', last_fencing_token = 102 WHERE id = 1 AND 102 > last_fencing_token;`
   (Success, DB records `last_fencing_token = 102`).
5. Client 1 wakes up and attempts its write:
   `UPDATE data SET value = 'A', last_fencing_token = 101 WHERE id = 1 AND 101 > last_fencing_token;`
   (Fails! Because $101 \ngtr 102$). Stale write is rejected. Data corruption prevented!

---

### Q26: How does ZooKeeper implement distributed locks using Ephemeral Sequential Nodes?
**Answer:**
1. A client creates an **`EPHEMERAL_SEQUENTIAL`** znode under the lock path:
   `/locks/order_pay/lock-0000000001`
2. The client fetches all children under `/locks/order_pay`.
3. If its created znode has the **lowest sequence number**, the client owns the lock!
4. **Herd Effect Avoidance:** If its znode is not the lowest, it finds the node with the sequence number immediately preceding its own (e.g. node 3 watches node 2) and sets a **Watcher** on that single node.
5. When node 2 releases (or crashes), ZooKeeper notifies *only* node 3.
6. If the client crashes, its session heartbeats stop $\rightarrow$ ZooKeeper deletes the ephemeral node automatically.

---

### Q27: Compare Redis Distributed Lock vs ZooKeeper Distributed Lock. When would you choose which?
**Answer:**
| Dimension | Redis (Redisson) | ZooKeeper (Curator) |
| :--- | :--- | :--- |
| **Consensus Model** | AP / Asynchronous Replication (Eventual consistency) | CP / ZAB Consensus Protocol (Strict consistency) |
| **Failover Risk** | Master failover can lose lock before replica syncs (Split-Brain) | Quorum-based; no lock loss during leader election |
| **Throughput** | High (50,000+ lock ops/sec in RAM) | Moderate (1,000 - 5,000 ops/sec due to disk fsync) |
| **Operational Overhead** | Low (almost every stack already has Redis) | High (requires ZooKeeper ensemble management) |
| **Decision Rule** | Use Redis for high-throughput, short-lived tasks, rate limiters, flash sales. | Use ZooKeeper for critical cluster coordination, leader election, metadata locks. |

---

### Q28: How do PostgreSQL Advisory Locks work and when should you use them over Redis?
**Answer:**
Postgres Advisory Locks (`SELECT pg_advisory_xact_lock(key)`) are application-level distributed locks managed directly in PostgreSQL shared memory (`LockMethod`).
- **Advantages:**
  1. Zero extra infrastructure: If your system already uses PostgreSQL, you don't need to deploy and operate Redis or ZooKeeper.
  2. Transaction-scoped (`pg_advisory_xact_lock`): Automatically released when the SQL transaction commits or rolls back (no lock leaks).
  3. No table bloat: Does not touch or lock any database tables or rows.
- **Ideal Use Case:** Multi-instance periodic batch jobs, distributed leader crons in small to medium scale architectures.

---

## Module 4: System Design Scenarios & High-Scale Edge Cases

### Q29: System Design: Flash Sale with 100,000 requests/sec for 1,000 units. How to lock?
**Answer (The 3-Tier Production Architecture):**
1. **Never lock the DB directly:** 100k req/sec will saturate connection pools and crash Postgres/MySQL in milliseconds.
2. **Tier 1 (In-Memory Atomic Gate via Redis Lua):**
   - Pre-warm inventory in Redis: `SET item:1001:stock 1000`.
   - Incoming requests call atomic Lua script:
     ```lua
     local stock = tonumber(redis.call('get', KEYS[1]))
     if stock >= tonumber(ARGV[1]) then
         redis.call('decrby', KEYS[1], ARGV[1])
         return 1
     else
         return 0
     end
     ```
   - Redis processes this in RAM at 100k ops/sec. Once stock hits 0, the remaining 99,000 requests are rejected at edge in < 2ms!
3. **Tier 2 (Asynchronous Queue):**
   - The 1,000 winning requests are pushed to a Kafka topic (`order-creation-topic`).
4. **Tier 3 (Batch DB Persistence):**
   - DB consumers pull from Kafka at a controlled rate (e.g. 50 orders/sec) and execute batch inserts into the database.

---

### Q30: How do you design Inventory Bucketing (Sharding) to increase Database Pessimistic Lock throughput by 10x?
**Answer:**
- **The Problem:** A single row lock (`SELECT ... FOR UPDATE WHERE item_id = 1`) serializes transactions to ~200-500 TPS due to row-level mutex and disk write-ahead log (WAL) sync.
- **The Bucketing Strategy:**
  Instead of 1 row with 1,000 stock, create 10 bucket rows of 100 stock each:
  - `item_1001_bucket_0: stock = 100`
  - `item_1001_bucket_1: stock = 100`
  - ...
  - `item_1001_bucket_9: stock = 100`
- **Execution:**
  Incoming user threads hash to a random bucket: `bucket_id = rand(0, 9)`.
  The thread locks *only that specific bucket row*.
  If bucket is empty, it tries the next bucket.
  **Result:** Contention is reduced by 10x! Throughput increases from 300 TPS to 3,000 TPS on the exact same database.

---

### Q31: How do you prevent Double-Spending in a digital wallet service?
**Answer:**
A multi-layered defense:
1. **Idempotency Key:** Client passes a unique `idempotency_key` (UUID) with the payment request. Check/insert into an `idempotency_records` table with unique constraint.
2. **Pessimistic Lock with Timeout:**
   `SELECT balance FROM wallet WHERE user_id = :id FOR UPDATE;`
3. **Atomic Balance Deduct Constraint:**
   `UPDATE wallet SET balance = balance - :amount WHERE user_id = :id AND balance >= :amount;`
   If affected rows == 0, abort immediately (insufficient balance).
4. **Deterministic Ordering:** If transferring between two wallets, always lock lower wallet ID first to avoid circular deadlock.

---

### Q32: How do you implement a distributed rate limiter without race conditions?
**Answer:**
- **Sliding Window Log via Redis Sorted Set (ZSET):**
  - Key: `rate_limit:user_id:endpoint`
  - Score & Value: Current timestamp in milliseconds (`currentTimeMs`).
  - Execute atomic Lua / Redis pipeline:
    1. `ZREMRANGEBYSCORE key 0 (currentTimeMs - windowMs)` (Remove entries older than window).
    2. `ZCARD key` (Count current requests in window).
    3. If `count < limit`, execute `ZADD key currentTimeMs currentTimeMs` and `EXPIRE key windowSeconds`, allow request.
    4. Else, reject with HTTP 429 Too Many Requests.
  - Using Redis Lua guarantees atomic execution without client-side locking.

---

### Q33: What is Priority Inversion and how do operating systems / lock implementations solve it?
**Answer:**
- **The Problem:**
  - Low-priority thread $L$ acquires Lock $X$.
  - High-priority thread $H$ wants Lock $X$ and is blocked waiting for $L$.
  - Medium-priority thread $M$ (which does NOT need Lock $X$) preempts thread $L$ because $M$ has higher priority than $L$.
  - Result: High-priority thread $H$ is indirectly blocked by medium-priority thread $M$ for an indefinite duration! (Famously almost crashed the Mars Pathfinder spacecraft in 1997).
- **The Solution: Priority Inheritance:**
  When thread $H$ blocks waiting for Lock $X$ held by thread $L$, the OS temporarily **boosts the priority of thread $L$ to match thread $H$** until $L$ releases Lock $X$. This prevents thread $M$ from preempting $L$.

---

### Q34: How do you prevent split-brain issues in Redis distributed locks during Master-Replica failover?
**Answer:**
Redis Sentinel / Cluster asynchronous replication:
1. Client 1 acquires lock on Redis Master.
2. The Master crashes *before* the key is replicated to the Redis Replica.
3. Sentinel promotes the Replica to become the new Master.
4. Client 2 requests the lock on the new Master $\rightarrow$ Lock granted!
5. Now both Client 1 and Client 2 believe they hold the lock!
**Mitigations:**
- Use Redlock across 3 or 5 independent master nodes (not master-replica).
- Enable `WAIT 1 1000` command in Redis to force synchronous replication before confirming lock acquisition.
- Implement **Fencing Tokens** on the database storage layer so split-brain writes are rejected.

---

### Q35: Summary checklist for SDE-2 interview: How to answer any locking question in 4 steps?
**Answer:**
Whenever an interviewer presents a concurrency/locking problem, follow this structured framework:
1. **Clarify Scale & Architecture:**
   - "Is this running on a single JVM instance or a distributed microservice cluster?"
   - "What is the expected read-to-write ratio and peak operations per second?"
2. **Identify the Core Invariant & Failure Mode:**
   - "What is the critical state? (e.g. inventory cannot go negative, double-spend prevention)."
   - "What happens under high contention? (Deadlocks, starvation, GC pauses, thundering herds)."
3. **Propose the Baseline Solution & Discuss Trade-offs:**
   - In-memory: `ReentrantLock` with timeout vs `StampedLock` vs `LongAdder`.
   - Database: `@Version` optimistic vs `FOR UPDATE` pessimistic vs `SKIP LOCKED`.
   - Distributed: Redisson with Watchdog + Fencing Token.
4. **Address Edge Cases Proactively:**
   - Lock timeouts to avoid connection starvation.
   - Resource ordering to avoid circular deadlocks.
   - Retry with exponential backoff and full jitter.
