# 06 — Java Concurrency & Multithreading

> **Priority: P0 (highest-weight SDE-2 topic).** Concurrency is where "I use `synchronized`" candidates get separated from engineers who can reason about atomicity, visibility, and ordering, size a thread pool, and explain why their fix is correct. Backend services are concurrent by default (one request = one thread), so almost every production bug you'll be asked about lives here.

**Prerequisites:** [01 Core Java](../01-core-java/) (object references, immutability), [03 Collections](../03-collections-framework/) (why HashMap isn't thread-safe).
**Related:** [07 JVM](../07-jvm-memory-and-performance/), [13 Spring Transactions](../13-spring-transactions/) (tx are thread-bound), [19 Messaging](../19-messaging-and-asynchronous-processing/).

Code for this chapter: [`code-examples/.../concurrency/`](../code-examples/).

---

## Table of contents
1. [Foundations: process/thread, concurrency/parallelism](#1-foundations)
2. [The three problems: atomicity, visibility, ordering](#2-the-three-problems)
3. [Java Memory Model & happens-before](#3-java-memory-model--happens-before)
4. [`synchronized` & intrinsic locks](#4-synchronized--intrinsic-locks)
5. [`volatile` and its limits](#5-volatile-and-its-limits)
6. [Atomics & compare-and-set](#6-atomics--compare-and-set)
7. [Explicit locks: ReentrantLock, ReadWriteLock, Condition](#7-explicit-locks)
8. [wait/notify and coordination primitives](#8-coordination-primitives)
9. [Deadlock, livelock, starvation](#9-deadlock-livelock-starvation)
10. [Thread pools & ExecutorService](#10-thread-pools--executorservice)
11. [CompletableFuture](#11-completablefuture)
12. [Virtual threads (Java 21)](#12-virtual-threads-java-21)
13. [Concurrency in Spring applications](#13-concurrency-in-spring)
14. [Trade-offs summary](#d-trade-offs-summary)
15. [Interview Q&A](#e-interview-qa)
16. [Production relevance](#f-production-relevance)
17. [Revision](#g-revision)

---

## 1. Foundations

### A. Concept & intuition
- **Process:** an isolated address space with its own heap. Inter-process communication is explicit (sockets, pipes).
- **Thread:** a unit of scheduling *inside* a process. Threads share the process heap and class metadata but each has its **own stack** (local variables, call frames) and program counter. Sharing the heap is exactly why threads are fast to coordinate *and* dangerous — two threads can touch the same object.
- **Concurrency vs parallelism:** concurrency is *dealing with* many things at once (structure: tasks interleave, possibly on one core). Parallelism is *doing* many things at once (execution: multiple cores literally run simultaneously). You can have concurrency without parallelism (one core, time-sliced) and the bugs are the same either way, because interleaving is enough to break non-atomic operations.

**Why it matters:** On a Spring Boot server, the servlet container (Tomcat) hands each HTTP request to a worker thread from a pool. Your singleton `@Service` beans are therefore touched by many threads at once. Any mutable field on a singleton is shared mutable state. That single fact is the root of most backend concurrency interview questions.

### B. Internal working
Each Java thread maps (on the HotSpot JVM on mainstream OSes) to one OS thread — a *platform thread*. The OS scheduler decides which runnable thread occupies a core. A context switch saves/restores registers and stack pointers; it costs microseconds and pollutes CPU caches. This is why "more threads" is not "more throughput" past a point (see [CON-04](#con-04)).

Virtual threads (Java 21) break the 1:1 mapping — many virtual threads are multiplexed onto a few platform "carrier" threads (see §12).

---

## 2. The three problems

Every concurrency bug is one (or more) of these:

| Problem | Question it answers | Fixed primarily by |
|---|---|---|
| **Atomicity** | Does a compound operation happen all-or-nothing relative to other threads? | locks, atomics |
| **Visibility** | When thread A writes, when (if ever) does thread B see it? | `volatile`, locks, `final` |
| **Ordering** | Can the compiler/CPU reorder my reads/writes in ways another thread observes? | `volatile`, locks (happens-before) |

**The classic misconception:** that these are the same thing. `volatile` gives visibility and ordering but **not** atomicity of compound operations. `synchronized`/locks give all three. Confusing them is the single most common way candidates fail concurrency questions.

### Why visibility is not obvious
Without synchronization, the JMM lets each thread keep values in registers / per-core caches and lets the compiler reorder operations. A write by thread A to a plain field may **never** become visible to thread B. The canonical example — a `while(!stopFlag)` loop that never terminates after another thread sets `stopFlag = true` — is a visibility bug, not a scheduling fluke.

```java
// BROKEN: visibility bug — the reader may loop forever.
class Worker implements Runnable {
    private boolean stop = false;          // plain field, no visibility guarantee
    public void requestStop() { stop = true; }
    public void run() { while (!stop) { /* spin */ } } // JIT may hoist !stop out of the loop
}
```
Fix: make `stop` `volatile` (visibility + prevents hoisting). See §5.

---

## 3. Java Memory Model & happens-before

### A. Concept & intuition
The **Java Memory Model (JMM)** is the *contract* that defines when a write by one thread is guaranteed visible to a read by another. It exists because real hardware and compilers reorder and cache aggressively; without a model, "shared memory" would be unusable portably. The JMM is a **language guarantee** (JLS §17), not an implementation detail — it holds across JVMs and CPUs.

### B. Happens-before — the one rule to internalize
If action X **happens-before** action Y, then X's effects are visible to Y. The edges you must know:

1. **Program order:** within a single thread, each action happens-before the next.
2. **Monitor lock:** unlocking a monitor happens-before every subsequent lock of the same monitor. (This is why `synchronized` publishes writes.)
3. **volatile:** a write to a `volatile` field happens-before every subsequent read of that field.
4. **Thread start:** `Thread.start()` happens-before any action in the started thread.
5. **Thread join:** all actions in a thread happen-before another thread returns from `join()` on it.
6. **Transitivity:** if A hb B and B hb C, then A hb C.

**Interview-ready phrasing:** "Happens-before isn't about time; it's about *visibility and ordering guarantees*. Two actions can be unordered by happens-before even if one runs before the other in wall-clock time, and then there's no guarantee the second sees the first."

### Safe publication
Publishing an object means making its reference visible to other threads. It's **safe** only if the publication rides a happens-before edge:
- via a `volatile` field / `AtomicReference`,
- via a `final` field set in the constructor (the JMM guarantees other threads see fully-initialized `final` fields — this is why immutable objects are safe to share),
- via a `static` initializer,
- by storing into a thread-safe collection that provides the edge (e.g., `ConcurrentHashMap`).

Publishing via a plain field is **unsafe**: another thread may see the reference but a half-constructed object.

---

## 4. `synchronized` & intrinsic locks

### A. Concept & intuition
Every Java object has an **intrinsic lock (monitor)**. `synchronized` acquires it on entry and releases on exit (including on exception). It provides **mutual exclusion** (atomicity for the guarded block) **and** the happens-before edge (visibility/ordering).

### B. Internal working — step by step
1. Thread tries to enter a `synchronized(obj)` block → attempts to acquire `obj`'s monitor.
2. If free, it enters; the JVM may use *biased/thin locking* fast paths (HotSpot implementation detail; biased locking was deprecated/removed in later JDKs — don't rely on it in answers).
3. If held by another thread, the entrant blocks (parked) until release.
4. On exit, the monitor is released and a happens-before edge is established to the next acquirer.
5. `wait()` inside releases the monitor and parks; `notify()`/`notifyAll()` wake waiters.

**Granularity matters.** `synchronized` on an instance method locks `this`; on a `static` method locks the `Class` object. Locking too coarsely serializes unrelated work (throughput collapse); locking too finely risks forgetting a path and reintroducing a race.

### C. Code demonstration — broken → fixed

```java
// BROKEN: check-then-act race. Two threads can both pass the check.
class DocumentPublisher {
    private final Set<Long> published = new HashSet<>();   // not thread-safe either
    void publishOnce(long docId) {
        if (!published.contains(docId)) {   // T1 and T2 both see "absent"
            send(docId);                    // duplicate publish
            published.add(docId);
        }
    }
}
```
Why it breaks: `contains`→`send`→`add` is a compound operation; nothing makes it atomic, and `HashSet` is not thread-safe (concurrent `add` can corrupt it).

```java
// FIXED (simplest correct): guard the compound op with one lock.
class DocumentPublisher {
    private final Set<Long> published = new HashSet<>();
    synchronized void publishOnce(long docId) {   // atomic check-then-act + safe HashSet access
        if (published.add(docId)) {               // add returns false if already present
            send(docId);
        }
    }
}
```
`Set.add` returning `false` collapses check-then-act into one atomic step *inside* the lock. Observable behavior: exactly one `send` per `docId` even under concurrent calls. (For a lock-free version, see `ConcurrentHashMap.newKeySet()` + `add` in §6/§13.)

---

## 5. `volatile` and its limits

### A. Concept & intuition
`volatile` guarantees **visibility** (every read sees the latest write) and **ordering** (prevents reordering across the access), but **not atomicity of compound operations**. Use it for flags and for the "single writer, many readers" / "publish a reference" patterns.

### C. Broken → fixed

```java
// BROKEN: volatile does NOT make i++ atomic. i++ is read-modify-write (3 steps).
class Counter {
    private volatile long count = 0;
    void inc() { count++; }        // lost updates under concurrency
    long get() { return count; }
}
```
Why: `count++` = read `count`, add 1, write back. Two threads can read the same value and both write `n+1`, losing one increment. `volatile` made each individual read/write visible but did nothing for the three-step compound.

```java
// FIXED option A: atomic CAS (lock-free, best for a single hot counter)
class Counter {
    private final java.util.concurrent.atomic.AtomicLong count = new java.util.concurrent.atomic.AtomicLong();
    void inc() { count.incrementAndGet(); }
    long get() { return count.get(); }
}
```
```java
// FIXED option B: a lock (use when you must update several fields atomically together)
class Counter {
    private long count = 0;
    synchronized void inc() { count++; }
    synchronized long get() { return count; }
}
```
**When `volatile` IS enough:** a `boolean running` flag, or publishing an immutable config object by reference (`volatile Config cfg; cfg = newCfg;`) — readers atomically see either the old or the new reference, never a torn one. (Note: `long`/`double` non-volatile writes may be non-atomic per the JLS; `volatile` also fixes that.)

**Never say in an interview:** "volatile makes it thread-safe." Say what it does and doesn't do.

---

## 6. Atomics & compare-and-set

### A/B. Concept & internals
`java.util.concurrent.atomic` (`AtomicInteger`, `AtomicLong`, `AtomicReference`, `LongAdder`, …) provide lock-free atomic updates built on **CAS (compare-and-set)**: a hardware instruction that atomically sets a value *only if* it still equals an expected value, else fails and you retry. This avoids blocking but can waste CPU under very high contention (the retry loop). The ABA problem (value changes A→B→A and CAS wrongly succeeds) is handled by `AtomicStampedReference` when it matters.

- **`LongAdder` vs `AtomicLong`:** under high write contention, `LongAdder` spreads increments across cells to reduce CAS contention; `sum()` adds them. Prefer `LongAdder` for hot counters where you read infrequently (e.g., metrics).

```java
// Lock-free idempotent "do once" using AtomicReference / ConcurrentHashMap
private final java.util.Set<Long> published = java.util.concurrent.ConcurrentHashMap.newKeySet();
void publishOnce(long id) {
    if (published.add(id)) {   // atomic; true only for the first caller
        send(id);
    }
}
```

---

## 7. Explicit locks

### A. Concept & intuition
`ReentrantLock` is `synchronized` with extra abilities: **tryLock** (with timeout — deadlock avoidance), **interruptible** acquisition, **fairness** option, and **multiple `Condition`s** per lock. `ReadWriteLock` allows many concurrent readers but exclusive writers — good for read-heavy, write-rare state. `StampedLock` adds optimistic reads (advanced).

Cost: you **must** `unlock()` in a `finally`, or you leak the lock forever.

```java
private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
void doWork() {
    if (!lock.tryLock(2, java.util.concurrent.TimeUnit.SECONDS)) {
        throw new IllegalStateException("could not acquire lock in time"); // avoids indefinite block
    }
    try { /* critical section */ } finally { lock.unlock(); }   // MUST be in finally
}
```

**When to prefer `synchronized`:** simpler, JVM-optimized, impossible to forget the release. Reach for `ReentrantLock` only when you need timeout/interruptibility/fairness/multiple conditions.

---

## 8. Coordination primitives

| Primitive | Use it for | One-line gotcha |
|---|---|---|
| `wait`/`notify`/`notifyAll` | low-level condition waiting | always `wait` in a `while` loop (guard against spurious wakeups); prefer `notifyAll` |
| `CountDownLatch` | wait for N one-time events (e.g., startup) | **not reusable** once it hits zero |
| `CyclicBarrier` | N threads rendezvous repeatedly | **reusable**; throws `BrokenBarrierException` if one party fails |
| `Semaphore` | limit concurrency to N permits (rate/throttle) | release in `finally`; permits can be "lost" if you forget |
| `Phaser` | dynamic parties, multi-phase (P2) | more flexible, more complex than barrier |
| `BlockingQueue` | producer/consumer handoff | bounded queue = built-in backpressure |

```java
// Bounded producer/consumer with backpressure — classic backend pattern
var queue = new java.util.concurrent.ArrayBlockingQueue<Task>(1000); // bounded -> backpressure
// producer: queue.put(task);  // blocks when full, protecting memory
// consumer: Task t = queue.take();  // blocks when empty
```

---

## 9. Deadlock, livelock, starvation

- **Deadlock:** two+ threads each hold a lock the other needs. Needs all four Coffman conditions (mutual exclusion, hold-and-wait, no preemption, circular wait).
- **Livelock:** threads keep responding to each other and make no progress (e.g., both back off repeatedly).
- **Starvation:** a thread never gets scheduled/lock (e.g., unfair locks, priority issues).

```java
// BROKEN: lock-ordering deadlock. transfer(a,b) and transfer(b,a) can deadlock.
void transfer(Account from, Account to, long amt) {
    synchronized (from) {
        synchronized (to) { from.debit(amt); to.credit(amt); }
    }
}
```
```java
// FIXED: impose a global lock order (e.g., by id) so cycles are impossible.
void transfer(Account from, Account to, long amt) {
    Account first  = from.id() < to.id() ? from : to;
    Account second = from.id() < to.id() ? to : from;
    synchronized (first) {
        synchronized (second) { from.debit(amt); to.credit(amt); }
    }
}
```
**Prevention toolkit:** global lock ordering; `tryLock` with timeout + back off; shrink critical sections; avoid calling foreign/user code while holding a lock. **Detection in prod:** a thread dump (`jstack <pid>`) prints "Found one Java-level deadlock" with the cycle (see ch.22).

---

## 10. Thread pools & ExecutorService

### A. Concept & intuition
Creating a thread per task is unbounded (OOM / scheduler thrash under load). A **thread pool** reuses a bounded set of threads and a queue — this is *load leveling* and the single most important concurrency control in a backend service.

### B. `ThreadPoolExecutor` internals — the parameters that matter
`corePoolSize`, `maximumPoolSize`, `keepAliveTime`, `workQueue`, `RejectedExecutionHandler`.

**The crucial, non-obvious rule:** a `ThreadPoolExecutor` grows past `corePoolSize` **only when the queue is full**. So with an **unbounded** `LinkedBlockingQueue` (what `Executors.newFixedThreadPool` uses), `maximumPoolSize` is never reached and the queue grows without bound → memory pressure / latency blowup. This is why `Executors.newFixedThreadPool`/`newCachedThreadPool` are discouraged for production; build the executor explicitly.

```java
// Production-shaped executor: bounded queue + caller-runs backpressure
var executor = new java.util.concurrent.ThreadPoolExecutor(
    8, 32, 60L, java.util.concurrent.TimeUnit.SECONDS,
    new java.util.concurrent.ArrayBlockingQueue<>(500),           // BOUNDED -> backpressure
    new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy() // slows producers instead of dropping/OOM
);
```

**Rejection policies:** `AbortPolicy` (default, throws), `CallerRunsPolicy` (runs on caller — natural backpressure), `DiscardPolicy`, `DiscardOldestPolicy`.

**Sizing (reason, don't memorize):**
- CPU-bound work: ~ number of cores (±1). More threads just add context switches.
- IO-bound work: higher, bounded by the downstream (DB connection pool size, remote service capacity). A pool bigger than your DB connection pool just queues on the DB.

### `newFixedThreadPool` vs `newCachedThreadPool` vs `newScheduledThreadPool`
- Fixed: N threads, unbounded queue (danger above).
- Cached: unbounded threads, hands off via `SynchronousQueue` — can spawn thousands of threads under a burst.
- Scheduled: for delayed/periodic tasks.

---

## 11. CompletableFuture

### A. Concept & intuition
`CompletableFuture<T>` composes asynchronous steps without blocking: `thenApply`, `thenCompose` (flatMap for futures), `thenCombine`, `allOf`/`anyOf`, plus explicit error handling. It replaces nested callbacks.

### B/C. Execution thread & exception handling — the two things people get wrong
1. **Where does the callback run?** If you use the non-`Async` variants, the callback may run on the thread that completed the future (often a pool thread or even the caller). The `*Async` variants run on the common `ForkJoinPool` **unless you pass your own executor** — and for backend work you almost always should pass your own bounded executor, so a slow task can't starve the shared common pool.
2. **Exceptions are swallowed if you don't handle them.** A failure completes the future exceptionally; `get()` throws `ExecutionException`. Use `.handle`/`.exceptionally`/`.whenComplete`.

```java
// BROKEN: blocks a request thread AND loses exceptions
CompletableFuture.supplyAsync(this::callSlowService)  // runs on shared common pool
    .thenAccept(this::store);                          // no error handling; failures vanish
// ... nobody observes completion/exception

// FIXED: own executor + explicit error handling + non-blocking composition
CompletableFuture
    .supplyAsync(this::callSlowService, ioExecutor)    // bounded, dedicated pool
    .thenApply(this::transform)
    .exceptionally(ex -> { log.error("fallback", ex); return fallback(); })
    .thenAccept(this::store);
```
**Do not** sprinkle `.get()`/`.join()` on a request thread — that reintroduces blocking and defeats the point.

---

## 12. Virtual threads (Java 21)

### A. Concept & intuition
Virtual threads (JEP 444, finalized in Java 21) are lightweight threads scheduled by the JVM onto a small pool of OS **carrier** threads. You can have *millions*. They make the simple **thread-per-request, blocking** style scale for **IO-bound** workloads without async/reactive complexity: when a virtual thread blocks on IO, the JVM unmounts it from its carrier so the carrier runs other virtual threads.

### B. What they do and don't fix
- **Help:** high-concurrency IO-bound work (DB calls, HTTP calls, waiting). You get async-like scalability with straight-line blocking code.
- **Don't help:** CPU-bound work — you still only have as many cores as you have; virtual threads add no compute.
- **Pinning:** if a virtual thread blocks **inside a `synchronized` block** on certain operations, it can **pin** the carrier (can't unmount), hurting scalability. Mitigation: prefer `ReentrantLock` over `synchronized` around blocking calls on hot paths. (This pinning limitation is being reduced in later JDKs; state it as version-dependent.)
- **Don't pool them:** virtual threads are cheap and disposable — create one per task (`Executors.newVirtualThreadPerTaskExecutor()`), don't pool. Pooling them is an anti-pattern.
- `ThreadLocal` still works but, with millions of threads, heavy thread-locals cost memory — a reason to prefer scoped values / explicit context passing.

```java
try (var exec = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
    for (var req : requests) exec.submit(() -> handleBlockingIo(req)); // one VT per task
}
```

---

## 13. Concurrency in Spring

- **Singleton beans are shared.** Never put request state in a mutable singleton field. Keep services stateless; pass state as method params or use request-scoped beans deliberately.
- **`@Async`** runs on a Spring `TaskExecutor`. Configure a **bounded** `ThreadPoolTaskExecutor`; the default can be surprising. Exceptions from `void @Async` methods go to an `AsyncUncaughtExceptionHandler`, not the caller.
- **Transactions are thread-bound** (ch.13): spawning a thread or calling `@Async` does **not** carry the caller's transaction/`EntityManager` to the new thread.
- **MDC / context propagation:** logging context (correlation IDs) lives in a `ThreadLocal`; when you hand work to another thread/pool, you must **propagate** it explicitly (copy the MDC map) or logs lose the trace. Same for `SecurityContext`.
- **Duplicate-work bugs** (notifications, document revisions) usually combine a check-then-act race with a missing DB uniqueness guard — fix in the DB with a unique constraint *and* make the operation idempotent (ch.17, ch.19).

---

## D. Trade-offs summary

| Mechanism | Pros | Cons | Perf under contention | Complexity | Use when | Poor choice when |
|---|---|---|---|---|---|---|
| `synchronized` | simple, auto-release, full happens-before | coarse, no timeout/try | blocks; fine for low contention | low | guarding a small compound op | high contention read-heavy; need timeout |
| `volatile` | cheapest visibility; no locking | no atomic compound ops | excellent | low | flags, publishing a reference | counters, multi-field invariants |
| Atomics/CAS | lock-free, scalable | only single-variable; retry spins | great until very high contention | low–med | hot single counters, idempotent flags | multi-field atomic updates |
| `LongAdder` | best for hot write counters | approximate during `sum`, more memory | excellent writes | low | metrics counters | you read far more than you write |
| `ReentrantLock` | tryLock/timeout/fair/conditions | must unlock in finally | like synchronized + features | med | need deadlock avoidance/interrupt | simple case where synchronized suffices |
| `ReadWriteLock` | many readers | writer starvation risk; overhead if writes frequent | great read-heavy | med | read-mostly shared state | write-heavy |
| Thread pool (bounded) | load leveling, backpressure | needs tuning; rejection handling | predictable | med | all server async work | one-off tasks |
| `CompletableFuture` | non-blocking composition | easy to swallow errors / block wrongly | depends on executor | med–high | pipelines of async steps | simple blocking call |
| Virtual threads | massive IO concurrency, simple code | pinning, no CPU gain, young | excellent for IO | low | IO-bound thread-per-request | CPU-bound; heavy synchronized hot paths |

> No invented benchmark numbers: "performance under contention" is qualitative. Measure your workload (ch.07 benchmarking pitfalls) before claiming one is faster.

---

## E. Interview Q&A

<a id="con-01"></a>
### CON-01 (P0) — Why is `volatile` insufficient for incrementing a counter?
**Testing:** do you distinguish atomicity from visibility?
**Strong answer:** `count++` is a read-modify-write — three operations. `volatile` guarantees each read sees the latest value and each write is visible, and it prevents reordering, but it does **not** make the three steps atomic as a unit. Two threads can read the same value, each add one, each write back the same result — one increment is lost. Fix with `AtomicLong.incrementAndGet()` (lock-free CAS) for a single counter, or a lock if the counter is updated together with other fields that must stay consistent.
**Weak answer / why it fails:** "volatile makes it thread-safe." It conflates visibility with atomicity; the interviewer will immediately ask you to trace two interleaved `i++` and you'll contradict yourself.
**Follow-ups:** How does `AtomicLong` avoid the lock? (CAS retry loop.) When would `LongAdder` be better? (hot writes, infrequent reads.) Could you use `synchronized`? (yes, but serializes and is heavier for a pure counter.)

<a id="con-02"></a>
### CON-02 (P0) — Explain happens-before. Give an example where two actions are *not* ordered.
**Strong answer:** Happens-before is the JMM's visibility/ordering guarantee: if X hb Y, X's effects are visible to Y. Edges include program order, monitor unlock→lock, volatile write→read, thread start, join, and transitivity. Example of *no* ordering: two threads writing/reading a **plain** field with no lock/volatile between them — even if the write happens earlier in wall-clock time, there's no hb edge, so the reader may never see it (the infinite `while(!stop)` loop). It's not about time; it's about the edge.
**Misconception:** "happens-before means happened earlier in time." No — it's a *guarantee relation*, and unordered actions have no visibility promise.

### CON-03 (P1) — What is safe publication? Why are immutable objects safe to share?
**Strong answer:** Safe publication = making an object reference visible to other threads along a happens-before edge so they see a fully constructed object. Immutable objects whose fields are `final` and set in the constructor get a JMM guarantee that other threads observe the initialized `final` fields even without extra synchronization, so they're safe to share freely. Publishing via a plain field risks exposing a partially-constructed object.

<a id="con-04"></a>
### CON-04 (P0) — Why can increasing a thread pool size make performance *worse*?
**Testing:** do you understand contention, context switching, and downstream limits?
**Strong answer:** Several reasons. (1) For CPU-bound work, once threads exceed cores, you add context-switch overhead and cache thrashing with no extra parallelism. (2) More threads contending on the same locks increases blocking and lock convoy. (3) For IO-bound work, if threads all hit a database with, say, a 20-connection pool, 200 threads just queue on the connection pool — you moved the bottleneck, didn't remove it, and added memory for all those stacks. The right size is bounded by the real constraint (cores for CPU work, downstream capacity for IO work), found by measurement.
**Follow-up:** What's the fix if the DB pool is the bottleneck? (Right-size the app pool to the DB pool; add backpressure; or address the slow query — ch.16.)

<a id="con-05"></a>
### CON-05 (P0) — Walk through thread-pool saturation and rejection policies.
**Strong answer:** A `ThreadPoolExecutor` adds threads up to `max` only when the queue is full; with an unbounded queue it never adds beyond core and the queue grows unbounded (latency/memory blowup). With a bounded queue, when both pool and queue are full, the `RejectedExecutionHandler` fires: `AbortPolicy` throws (default), `CallerRunsPolicy` executes on the submitting thread (natural backpressure — slows the producer), `DiscardPolicy`/`DiscardOldestPolicy` drop tasks. For a server, a bounded queue + `CallerRunsPolicy` (or an explicit 429/backpressure) is usually safest.
**Trap:** many candidates say `newFixedThreadPool` is "safe" — its unbounded queue is exactly the risk.

### CON-06 (P1) — `synchronized` vs `ReentrantLock`: when do you pick which?
**Strong answer:** Default to `synchronized` — simpler, auto-released on exception, JVM-optimized. Choose `ReentrantLock` when you need `tryLock`/timeout (deadlock avoidance), interruptible acquisition, fairness, or multiple `Condition`s. The cost of `ReentrantLock` is that you must `unlock()` in `finally`.

<a id="con-07"></a>
### CON-07 (P0) — Explain deadlock: cause, how you'd detect it in production, how you'd prevent it.
**Strong answer:** Deadlock needs all four Coffman conditions; the practical trigger is inconsistent lock ordering (`transfer(a,b)` vs `transfer(b,a)`). Prevent by imposing a global lock order (e.g., by account id), using `tryLock` with timeout and backing off, shrinking critical sections, and never calling foreign code while holding a lock. Detect in prod with a thread dump (`jstack`), which explicitly reports the deadlock cycle; also watch for threads stuck in `BLOCKED` state.
**Follow-up:** livelock vs deadlock? (livelock = threads keep moving but make no progress; deadlock = everyone blocked.)

### CON-08 (P1) — How does `CompletableFuture` decide which thread runs a callback, and how do you handle errors?
**Strong answer:** Non-`Async` stages may run on whatever thread completed the previous stage (could be the caller); `*Async` stages run on the common `ForkJoinPool` unless you pass an executor — and for server work you should pass a bounded, dedicated executor so one slow task can't starve the shared pool. Errors complete the future exceptionally and are silent unless you add `.exceptionally`/`.handle`/`.whenComplete`; `join()`/`get()` will throw `CompletionException`/`ExecutionException`. Never block a request thread with `get()`.

<a id="con-09"></a>
### CON-09 (P0) — Virtual threads: when do they help, when don't they, what is pinning?
**Strong answer:** They let the simple blocking thread-per-request model scale to huge concurrency for **IO-bound** work, because the JVM unmounts a blocked virtual thread from its carrier. They do **not** speed up CPU-bound work — cores are the limit. **Pinning** is when a virtual thread can't unmount (historically when blocking inside a `synchronized` block on certain operations), which ties up a carrier and hurts scalability; mitigate by using `ReentrantLock` around blocking calls on hot paths. Don't pool virtual threads — create one per task. (Pinning behavior is version-dependent and improving.)
**Trap:** "virtual threads make everything faster." No — IO concurrency, not CPU throughput.

### CON-10 (P1) — Give a realistic backend concurrency bug and your fix.
**Strong answer (document revisions):** Two requests load the same document (version 5), each edits and saves version 6 — a lost update. Fix with optimistic locking (`@Version`): the second save fails `OptimisticLockException` and we retry/merge or return 409. If writes are frequent and conflicts common, pessimistic `SELECT … FOR UPDATE` serializes them. The decision depends on contention. (See ch.12/17/27.)

### CON-11 (P1) — Why is a `HashMap` dangerous under concurrency and is `ConcurrentHashMap` a drop-in fix?
**Strong answer:** Concurrent `put` during resize can corrupt the structure (historically an infinite loop pre-Java 8; still lost updates/visibility issues). `ConcurrentHashMap` makes individual operations thread-safe via per-bin locking/CAS, but **compound** operations (get-then-put) are still races — use its atomic methods (`computeIfAbsent`, `merge`, `putIfAbsent`). So it's not a blanket replacement; it changes which operations are atomic. (Full treatment: COL-01/COL-03 in ch.03.)

### CON-12 (P1) — What breaks when you propagate work to another thread in Spring?
**Strong answer:** The transaction and `EntityManager` are bound to the original thread, so the new thread has no transaction (lazy loads fail, writes aren't in the same tx). MDC logging context and `SecurityContext` are `ThreadLocal` and don't follow automatically — you must copy them. This is why `@Async` + lazy-loaded entities is a classic bug.

### CON-13 (P2) — `CountDownLatch` vs `CyclicBarrier` vs `Semaphore`?
**Strong answer:** Latch: one-shot wait for N events, not reusable. Barrier: N threads rendezvous repeatedly, reusable, breaks if a party fails. Semaphore: N permits to bound concurrency (throttling), release in `finally`.

---

## F. Production relevance
Concurrency correctness shows up as: duplicate notifications/emails (check-then-act + missing uniqueness), lost document updates (no optimistic lock), request latency spikes (pool saturation / lock convoy), memory growth (unbounded queue), and mysterious "works locally, fails under load" bugs (visibility/ordering that only manifest on multi-core production). The defensive posture: keep singletons stateless, bound every pool and queue, make shared mutable state the exception (and guard it with the simplest correct primitive), enforce invariants in the database too, and make operations idempotent so retries are safe.

---

## G. Revision

### 10 key takeaways
1. Every concurrency bug is atomicity, visibility, or ordering.
2. `volatile` = visibility + ordering, **not** atomicity of compound ops.
3. `i++` is read-modify-write; use atomics or a lock.
4. Happens-before is a guarantee relation, not wall-clock order.
5. `synchronized` gives mutual exclusion **and** the happens-before edge.
6. Singleton Spring beans are shared — keep them stateless.
7. A `ThreadPoolExecutor` grows past core only when the queue is full; unbounded queues are the trap.
8. Bound every queue; use `CallerRunsPolicy`/backpressure.
9. Virtual threads scale IO-bound blocking code, not CPU-bound; beware pinning; don't pool them.
10. Transactions/MDC/SecurityContext are thread-bound — propagate explicitly.

### 10 rapid-fire
1. Does `volatile` make `count++` safe? (No.)
2. What establishes happens-before besides locks? (volatile, start, join, final fields.)
3. When does a thread pool spawn beyond core size? (queue full.)
4. Default rejection policy? (`AbortPolicy`.)
5. Reusable barrier? (`CyclicBarrier`.)
6. One-shot countdown? (`CountDownLatch`.)
7. Lock-free counter class? (`AtomicLong`/`LongAdder`.)
8. Thread-safe compound op on a map? (`computeIfAbsent`.)
9. Virtual thread for CPU-bound crunching? (No benefit.)
10. Where do `@Async` void exceptions go? (`AsyncUncaughtExceptionHandler`.)

### 5 tricky
1. Why can a `while(!flag)` loop never terminate even on a healthy machine? (visibility; JIT hoisting.)
2. Is `ConcurrentHashMap` enough to make `if(!map.containsKey(k)) map.put(k,v)` atomic? (No — use `putIfAbsent`/`computeIfAbsent`.)
3. Why might `CompletableFuture.supplyAsync(...)` silently never report a failure? (no error stage attached.)
4. Why can a 200-thread pool be slower than a 20-thread pool against one DB? (queues on the connection pool; context switches.)
5. How can a virtual thread *reduce* scalability? (pinning inside `synchronized` on blocking IO.)

### 5 exercises
1. Reproduce the lost-update counter with 10 threads × 100k increments; print the wrong total; fix with `AtomicLong` and verify.
2. Build a bounded producer/consumer with `ArrayBlockingQueue`; show backpressure by slowing the consumer.
3. Trigger a lock-ordering deadlock between two accounts; capture a `jstack` showing the cycle; fix with ordered locking.
4. Write an `@Async` method that fails silently, then route its exception to an `AsyncUncaughtExceptionHandler`.
5. Compare `newFixedThreadPool` (unbounded queue) vs an explicit bounded `ThreadPoolExecutor` by flooding tasks and observing memory/latency.

### One-page revision sheet
> Three problems: **atomicity / visibility / ordering**. `volatile`=visibility+ordering only. `i++`=RMW→atomic/lock. Happens-before edges: program order, monitor unlock→lock, volatile write→read, start, join, final-field, transitive. `synchronized`=mutex+edge; `ReentrantLock` adds tryLock/timeout/fair/conditions (unlock in finally). Atomics=CAS lock-free; `LongAdder` for hot writes. Pools: grow past core **only when queue full**; bound the queue; rejection = Abort/CallerRuns/Discard/DiscardOldest; size = cores (CPU) or downstream capacity (IO). `CompletableFuture`: pass your executor, handle errors, don't block request thread. Virtual threads: IO-bound scale, no CPU gain, beware pinning (prefer ReentrantLock), one-per-task. Spring: singletons shared→stateless; tx/MDC/SecurityContext thread-bound.

### Prerequisites to revisit if you struggle
- Object references & stack-vs-heap (ch.01) — to see what's shared.
- HashMap internals (ch.03) — to understand the resize corruption.
- Spring bean scopes (ch.09) and transaction thread-binding (ch.13).
