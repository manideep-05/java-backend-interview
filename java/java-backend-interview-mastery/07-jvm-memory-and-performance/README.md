# 07 — JVM Memory & Performance

> **Priority: P0.** Interviewers want evidence-based reasoning: how to investigate a slow or memory-hungry service, not a list of GC flags. "Show me how you'd find the leak" beats "which collector is fastest."

**Prerequisites:** [01 Core Java](../01-core-java/), [06 Concurrency](../06-java-concurrency/).
**Related:** [22 Production debugging](../22-debugging-and-production-engineering/).

---

## A. Concept & intuition
The JVM loads bytecode, manages memory automatically (GC), and compiles hot code to native (JIT). You don't `free()` memory, but you *can* leak it by holding references. Performance work is about **measuring** where time/memory goes, not guessing.

## B. Internals

### Runtime memory areas
- **Heap** — all objects; divided (in generational collectors) into **young** (eden + survivor) and **old**. Shared across threads.
- **Stack** — per thread; frames with locals and operand stack. `StackOverflowError` when too deep (e.g., infinite recursion).
- **Metaspace** — class metadata (native memory, replaced PermGen in Java 8). Leaks here come from loading too many classes/classloaders.
- **Thread stacks, code cache (JIT output), direct/native memory**.

### Class loading
Parent-first delegation: a classloader asks its parent before loading itself (bootstrap → platform → application). Custom classloaders (app servers, plugins) can cause leaks if they retain references.

### JIT
Code starts interpreted; the JIT compiles **hot** methods to native and inlines/optimizes based on runtime profiles (C1/C2 tiers). Implication for benchmarking: you must **warm up** before measuring, or you measure the interpreter.

### Garbage collection
Generational hypothesis: most objects die young. Minor GC collects the young gen (fast); major/full GC touches old gen (costlier). **G1** (default since Java 9) targets predictable pause times by collecting regions; ZGC/Shenandoah are low-pause collectors for large heaps. GC reclaims **unreachable** objects — a "leak" in Java is objects you no longer need but are still **reachable** (so GC can't collect them).

### Reference types
Strong (normal), **soft** (cleared under memory pressure — caches), **weak** (cleared at next GC — `WeakHashMap` keys), **phantom** (post-mortem cleanup). Know them for cache/cleanup questions.

## C. Code — a classic leak
```java
// BROKEN: unbounded static cache -> entries reachable forever -> OutOfMemoryError over time
class Cache {
    static final Map<String,byte[]> C = new HashMap<>(); // static => lives for app lifetime
    static void put(String k, byte[] v){ C.put(k, v); }  // never evicts
}
// FIX: bound it (size/TTL) with Caffeine, or use a weak/soft-referenced cache, or evict explicitly.
```
Also classic: a listener/callback registered but never deregistered; a `ThreadLocal` set on a pooled thread and never removed (leaks per request across the pool).

## D. Trade-offs — diagnostics toolkit
| Tool | Shows | Use for |
|---|---|---|
| GC logs (`-Xlog:gc*`) | pause times, frequency, heap after GC | is GC the problem? growing old gen = leak |
| Heap dump (`jmap`, auto on OOM via `-XX:+HeapDumpOnOutOfMemoryError`) + MAT | dominator tree, biggest retained | find what holds memory |
| Thread dump (`jstack`) | thread states, locks, deadlocks | hangs, high latency, deadlock |
| JFR (Java Flight Recorder) | low-overhead CPU/alloc/lock profiling | find hot methods, allocation sources |
| async-profiler | flame graphs | CPU vs alloc attribution |

## E. Interview Q&A

### JVM-01 (P1) — Heap vs stack; what causes OOM vs StackOverflow?
**Strong answer:** Objects live on the shared heap; each thread has a stack of frames for locals/calls. `OutOfMemoryError` means the heap (or Metaspace/direct memory) can't satisfy an allocation — usually a leak (reachable-but-unneeded objects) or an undersized heap. `StackOverflowError` means a thread's stack overflowed, typically unbounded recursion. They're different memory regions and different root causes.

<a id="jvm-02"></a>
### JVM-02 (P0) — How would you investigate a production memory leak?
**Strong answer:** First confirm it's a leak with GC logs — if the heap after full GC keeps climbing over time, memory is being retained. Then capture a heap dump (I enable `HeapDumpOnOutOfMemoryError`, or take one with `jmap`) and open it in MAT/VisualVM, looking at the **dominator tree** to find the objects retaining the most memory and the GC roots holding them. Common culprits: unbounded caches/collections, `ThreadLocal`s not removed on pooled threads, listeners never deregistered, classloader leaks. I fix the root cause (bound the cache, remove the ThreadLocal) and verify the heap stabilizes under the same load. I don't start by changing GC flags — I find what's reachable that shouldn't be.
**Weak answer:** "Increase the heap." That delays, not fixes, a real leak.

### JVM-03 (P1) — What's a memory leak in a garbage-collected language?
**Strong answer:** Objects you no longer need but that are still reachable from a GC root, so the collector can't reclaim them. The usual sources are long-lived containers (static maps, caches), unremoved `ThreadLocal`s, and un-deregistered callbacks. The fix is to drop the references (bounded caches, cleanup).

### JVM-04 (P1) — Explain generational GC and G1 at a high level.
**Strong answer:** Most objects die young, so the heap is split into young and old generations; minor GCs cheaply collect the young gen and promote survivors. G1 (default) divides the heap into regions and collects those with the most garbage first to meet a pause-time target, avoiding long stop-the-world full GCs in the common case. For very large heaps with strict latency, ZGC/Shenandoah offer very low pauses. I pick based on measured pause requirements, not folklore.

<a id="jvm-05"></a>
### JVM-05 (P0) — How do you tell a CPU-bound bottleneck from an IO-bound one?
**Strong answer:** Measure. If CPU utilization is near saturation and a CPU profiler (JFR/async-profiler flame graph) shows time in computation, it's CPU-bound — fix the algorithm or parallelize across cores. If CPU is low but latency is high, threads are mostly **waiting** (DB, remote calls, locks): a thread dump shows many threads `BLOCKED`/`WAITING` on IO or monitors, and the fix is reducing round trips (N+1, batching), connection-pool sizing, or timeouts — adding threads won't help CPU-bound work and may not help IO-bound work if the real limit is downstream ([06 CON-04](../06-java-concurrency/)).

### JVM-06 (P1) — What are the pitfalls of microbenchmarking in Java?
**Strong answer:** Without warm-up you measure the interpreter, not JIT-compiled code; dead-code elimination can delete the thing you're timing; constant folding and on-stack replacement distort results; GC and background compilation add noise. I use JMH, which handles warm-up, prevents dead-code elimination, and reports statistics — and I never quote a single run as a benchmark.

### JVM-07 (P2) — Soft vs weak vs phantom references?
**Strong answer:** Soft references are cleared only under memory pressure (useful for memory-sensitive caches); weak references are cleared at the next GC once no strong refs remain (`WeakHashMap` keys, canonicalizing maps); phantom references are enqueued after an object is finalized for deterministic post-mortem cleanup (a safer replacement for finalizers). In practice I prefer a proper cache library over hand-rolling soft/weak caches.

## F. Production relevance
This chapter is the toolkit for the worst incidents: a creeping `OutOfMemoryError` (unbounded cache / ThreadLocal leak), high GC pauses (allocation storms, undersized heap), high CPU (hot method, regex, serialization), and hangs (deadlock, lock contention). The professional posture is evidence-first: GC logs → heap/thread dumps → profiler, change one thing, verify under load. Blindly tuning flags or growing the heap hides leaks until they return worse. See the [production debugging playbooks](../22-debugging-and-production-engineering/).

## G. Revision

### 10 key takeaways
1. Heap=objects(shared); stack=frames(per thread); Metaspace=class metadata.
2. Leak = reachable-but-unneeded objects (GC can't help).
3. OOM=heap/metaspace exhausted; StackOverflow=deep recursion.
4. Generational GC: most objects die young; G1 is region-based default.
5. Confirm leaks with GC logs; find them with heap dumps + dominator tree.
6. Common leaks: static caches, ThreadLocals, listeners, classloaders.
7. CPU-bound vs IO-bound: measure CPU + profiler + thread dump.
8. Warm up before benchmarking; use JMH.
9. Soft(memory pressure)/weak(next GC)/phantom(cleanup).
10. Fix root cause; don't just grow the heap or tweak flags.

### 10 rapid-fire
1. Default GC since Java 9? (G1) 2. Class metadata area? (Metaspace) 3. Tool for heap dump analysis? (MAT) 4. Deadlock from? (thread dump/jstack) 5. Low-overhead profiler? (JFR) 6. WeakHashMap keys cleared when? (next GC) 7. Benchmark harness? (JMH) 8. OOM auto-dump flag? (`-XX:+HeapDumpOnOutOfMemoryError`) 9. StackOverflow cause? (recursion) 10. Leak definition? (reachable, unneeded)

### 5 tricky
1. Why does raising `-Xmx` not fix a leak? 2. Why can a ThreadLocal leak across requests? (pooled threads) 3. Why does a benchmark show absurd speed? (dead-code elimination / no warm-up) 4. Low CPU but high latency — bound by? (IO/locks) 5. Why is a static map a leak risk? (lives for app lifetime)

### 5 exercises
1. Create an unbounded static-cache leak; capture a heap dump; find it in the dominator tree; fix with Caffeine.
2. Trigger StackOverflow with recursion and read the trace.
3. Capture GC logs under load; identify minor vs full GC and old-gen growth.
4. Use JFR to find a hot method in a CPU-bound loop.
5. Take a thread dump during an artificial deadlock and read the cycle.

### One-page revision sheet
> Memory: **heap**(objects, young/old) + **stack**(frames/thread) + **Metaspace**(classes). Leak=**reachable-but-unneeded** (static maps, ThreadLocals, listeners). OOM(heap/metaspace) vs StackOverflow(recursion). GC: generational, G1 default (region, pause target); ZGC/Shenandoah low-pause. **Investigate leak:** GC logs confirm (old gen grows after full GC) → heap dump + MAT dominator tree → fix roots → verify. **CPU vs IO bound:** measure CPU + flame graph + thread dump; adding threads won't help CPU-bound. Benchmark with JMH + warm-up. Refs: soft(pressure)/weak(next GC)/phantom(cleanup).

### Prerequisites to revisit
- Object references/reachability (ch.01); thread states & pools (ch.06). Applied in ch.22.
