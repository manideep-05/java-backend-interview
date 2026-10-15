# 30 — Node.js Backend Fundamentals (for polyglot backends)

> **Priority: P1 (conditional — only if you'll be interviewed on Node).** You built a Node/Express/RabbitMQ notification platform, so if an interviewer sees Node on your resume they'll probe the event loop, async model, and backpressure. This chapter covers the Node fundamentals an SDE-2 is expected to explain and contrasts them with the JVM so you can speak to *both* stacks coherently.

**Prerequisites:** [06 Java Concurrency](../06-java-concurrency/) (for the contrast), [19 Messaging](../19-messaging-and-asynchronous-processing/) (your RabbitMQ outbox platform).

---

## A. Concept & intuition
Node.js runs JavaScript on V8 with a **single-threaded event loop** plus a small background thread pool (libuv) for certain operations. Instead of a thread per request (the servlet model), one thread handles many connections by never blocking: it starts IO and registers a callback, then moves on. This makes Node excellent for **IO-bound, high-concurrency** workloads (APIs, proxies, notification fan-out) and poor for **CPU-bound** work (which blocks the single thread). This is the mirror image of the JVM's thread-per-request model (and conceptually close to Java **virtual threads** — [06 §12](../06-java-concurrency/)).

**What breaks if you don't understand it:** a single CPU-heavy or synchronous call freezes the entire process for *all* requests — the classic Node production incident.

## B. Internals

### The event loop (NODE-01)
The loop runs in phases each tick: **timers** (`setTimeout`/`setInterval`), **pending callbacks**, **poll** (IO events — the main phase), **check** (`setImmediate`), **close callbacks**. Between every phase (and after each callback) it drains the **microtask queue**: resolved **Promises** (`.then`/`await`) and `process.nextTick` run before the loop continues — so microtasks have priority over `setTimeout`. Understanding this ordering is the #1 Node interview question.

```js
console.log('A');
setTimeout(() => console.log('timeout'), 0);   // macrotask: timers phase
Promise.resolve().then(() => console.log('promise')); // microtask: runs first
process.nextTick(() => console.log('nextTick'));       // microtask, even before promises
console.log('B');
// Output: A, B, nextTick, promise, timeout
```

### libuv thread pool
Node is "single-threaded" for *your* JS, but libuv uses a background pool (default 4 threads, `UV_THREADPOOL_SIZE`) for things that can't be async at the OS level: filesystem IO, DNS (`dns.lookup`), and crypto/zlib. Network IO uses the OS's async mechanisms (epoll/kqueue), not the pool. So a flood of `fs`/crypto work can exhaust the pool and stall even though "Node is async."

### Blocking the loop (NODE-02)
Any synchronous CPU work (a big JSON parse, a tight loop, synchronous `fs.readFileSync`, a slow regex) runs *on the event loop thread* and blocks every other request until it finishes. There is no preemption. Fixes: offload CPU work to **worker threads** (`worker_threads`) or a separate service/queue, stream large payloads, and never use `*Sync` APIs on the request path.

### Scaling across cores: cluster & worker_threads (NODE-03)
One Node process uses one core. To use all cores you run the **cluster** module (or a process manager like **PM2**, which you used) to fork one process per core behind a shared socket — scaling IO-bound throughput. **worker_threads** are for CPU-bound work *within* a process (they share memory via `SharedArrayBuffer`); cluster is for scaling whole request-handling processes. PM2 also gives you restarts and zero-downtime reloads.

### Async models: callbacks → Promises → async/await (NODE-04)
Callbacks (error-first `(err, data) => {}`) led to "callback hell" and easy error-swallowing. Promises made composition cleaner; `async/await` is syntactic sugar over Promises that reads like synchronous code but is non-blocking. Key correctness points: `await` in a loop serializes (use `Promise.all` for concurrency); an unhandled rejected Promise crashes the process in modern Node unless handled; always `try/catch` around `await` or attach `.catch`.

```js
// BROKEN: serial awaits when they could be concurrent
for (const id of ids) { await process(id); }        // one at a time
// BETTER (bounded concurrency): run in parallel but cap the fan-out
await Promise.all(ids.map(id => process(id)));        // all at once (careful with large N)
// BEST for large N: a bounded pool (e.g., p-limit) to avoid overwhelming downstreams
```

### Error handling (NODE-05)
No checked exceptions. Patterns: `try/catch` around `await`; `.catch` on Promises; Express error-handling middleware `(err, req, res, next)`; and process-level `unhandledRejection`/`uncaughtException` handlers for last-resort logging + graceful shutdown (don't keep running after an `uncaughtException` — the process state is unknown). A thrown error inside a callback that isn't caught can crash the process.

### Streams & backpressure (NODE-06)
Streams process data in chunks instead of buffering everything in memory (reading a big file, piping an HTTP response). **Backpressure**: when a writable stream can't keep up, `.write()` returns `false` and you should pause the readable until `'drain'` — `pipe()`/`pipeline()` handle this for you. Ignoring backpressure (e.g., reading faster than you can write/send) blows up memory. This is the Node analogue of the bounded-queue backpressure in [06 §8](../06-java-concurrency/) / [19](../19-messaging-and-asynchronous-processing/).

### Node vs the JVM (the comparison they'll ask)
| Aspect | Node.js | JVM (Spring) |
|---|---|---|
| Concurrency model | single event-loop thread + libuv pool | thread-per-request pool (or virtual threads) |
| IO | non-blocking by default | blocking by default (async opt-in) |
| CPU-bound work | blocks the loop → worker_threads/offload | uses pool threads/cores naturally |
| Scaling cores | cluster / PM2 (multi-process) | one process, many threads |
| Shared mutable state | mostly none (single thread) → fewer races | real races → locks/atomics ([06](../06-java-concurrency/)) |
| Memory model | one heap per process | one heap, many threads |
| Error model | callbacks/Promises, no checked exceptions | checked + unchecked exceptions |

The headline: Node trades the JVM's multithreading complexity for a single-threaded model that's simpler for IO concurrency but dangerous for CPU work — and you scale Node by adding *processes*, not threads.

## C. Code — your platform, framed correctly
```js
// Outbox-style publish on Express + RabbitMQ (the shape of your notification platform):
// 1) business change + outbox row committed in ONE DB transaction (no dual-write gap)
// 2) a relay reads outbox rows and publishes to RabbitMQ
// 3) up to 5 retries with backoff, then route to a dead-letter queue
// 4) consumers are idempotent (dedup key) because delivery is at-least-once
async function publishPending(channel, db) {
  const rows = await db.fetchUnsent({ limit: 100 });     // non-blocking IO
  for (const row of rows) {
    try {
      await publishWithRetry(channel, row, { attempts: 5 }); // backoff + jitter
      await db.markSent(row.id);
    } catch (err) {
      await channel.sendToQueue('notifications.dlq', row.payload); // poison -> DLQ
      logger.error({ err, id: row.id }, 'routed to DLQ after retries');
    }
  }
}
```
This is exactly the [ch.19](../19-messaging-and-asynchronous-processing/) outbox/at-least-once/DLQ story — the concepts are language-independent, which is a strong thing to say: "the pattern is the same whether I implement it in Node or Spring; only the primitives differ."

## D. Trade-offs
| Choice | Node strength | Node weakness | Prefer when |
|---|---|---|---|
| Event-loop model | huge IO concurrency, low overhead | CPU work blocks everything | IO-bound APIs, fan-out, proxies |
| cluster/PM2 | use all cores, resilience | multi-process state coordination | scale a stateless HTTP service |
| worker_threads | CPU work off the loop | complexity, serialization cost | occasional CPU-heavy tasks |
| async/await | readable async | easy to serialize or swallow errors | almost always over raw callbacks |

## E. Interview Q&A

### NODE-01 (P1) — Explain the event loop and the order of `setTimeout` vs Promise vs `process.nextTick`.
**Strong answer:** Node runs a single-threaded loop with phases (timers, poll for IO, check/`setImmediate`, close), and between phases it drains the microtask queue — resolved Promises and `process.nextTick` — before continuing. So microtasks run before the next macrotask: given a `setTimeout(…,0)`, a resolved Promise, and a `process.nextTick`, the order is `nextTick` → Promise → timeout. The loop stays responsive only because callbacks don't block; anything synchronous and heavy stalls it.

### NODE-02 (P1) — What happens if you do CPU-heavy work in a request handler?
**Strong answer:** It runs on the single event-loop thread with no preemption, so it blocks *every* other request and connection until it finishes — a classic Node outage. I keep CPU work off the loop: offload to `worker_threads`, push it to a separate service/queue, stream instead of buffering, and never call `*Sync` APIs on the hot path. Node is for IO concurrency, not computation.

### NODE-03 (P1) — How do you use all CPU cores in Node?
**Strong answer:** One Node process uses one core, so I run one process per core with the cluster module or a manager like PM2 behind a shared socket, scaling IO-bound throughput and adding restart/zero-downtime reloads. For CPU-bound work *inside* a process I use `worker_threads`. Cluster scales request handling across processes; worker_threads parallelize computation within one.

### NODE-04 (P1) — Callbacks vs Promises vs async/await; a common async bug?
**Strong answer:** Callbacks (error-first) led to nesting and swallowed errors; Promises compose; async/await reads synchronously but stays non-blocking. A common bug is `await` inside a loop when the iterations are independent — that serializes them; I use `Promise.all` for concurrency, or a bounded pool for large N so I don't overwhelm a downstream. Another is an unhandled rejection, which crashes modern Node — I always `try/catch` around `await` or attach `.catch`.

### NODE-05 (P1) — How do you handle errors in Node/Express?
**Strong answer:** `try/catch` around `await`, `.catch` on Promises, and Express error middleware `(err, req, res, next)` to translate errors to consistent responses. At the process level I log `unhandledRejection`/`uncaughtException` and shut down gracefully rather than continuing in an unknown state. There are no checked exceptions, so discipline about catching and not swallowing matters even more than in Java.

### NODE-06 (P1) — What is backpressure and how do streams handle it?
**Strong answer:** Backpressure is when a consumer can't keep up with a producer; in Node, `writable.write()` returns `false` when its buffer is full and you should pause the readable until the `'drain'` event. `pipe()`/`pipeline()` manage this automatically. Ignoring it means unbounded buffering and memory blowup — the same principle as a bounded queue in the JVM. For large files/responses I stream rather than buffer the whole payload.

### NODE-07 (P1) — Node vs JVM: when would you choose which?
**Strong answer:** Node for IO-bound, high-concurrency, mostly-glue services (APIs, real-time, fan-out) where its single-threaded model avoids locking complexity and starts fast; the JVM/Spring for CPU-intensive work, strong typing, mature transaction/ORM tooling, and teams that want threads/parallelism. I've built the same reliability patterns — outbox, retries, DLQ, idempotency — in both, which shows the patterns are language-independent; the choice is about the workload and ecosystem, not the pattern.

## F. Production relevance
Your notification platform is the canonical "good fit for Node" workload: IO-bound fan-out with reliability patterns (outbox, 5 retries, DLQ, idempotency). The risks you must be ready to discuss: a blocked event loop (don't do CPU/sync work on the request path), unhandled rejections crashing the process, libuv pool exhaustion from heavy fs/crypto, and backpressure when publishing faster than the broker/consumers drain. Being able to say "I used PM2 to run one process per core and restart on failure, kept CPU work off the loop, and made consumers idempotent" demonstrates real operational understanding.

## G. Revision

### 10 key takeaways
1. Single-threaded event loop + libuv pool; great for IO, bad for CPU.
2. Microtasks (Promises, `nextTick`) run before the next macrotask (`setTimeout`).
3. `nextTick` before Promise microtasks; both before timers.
4. CPU/sync work blocks the whole process — offload to worker_threads.
5. Scale cores with cluster/PM2 (processes), not threads.
6. worker_threads = CPU within a process; cluster = scale request handling.
7. `await` in a loop serializes; use `Promise.all` / bounded pool.
8. Unhandled rejection crashes modern Node — always catch.
9. Streams + backpressure (`write()`→false, wait for `'drain'`; use `pipeline`).
10. Reliability patterns (outbox/retry/DLQ/idempotency) are language-independent.

### 10 rapid-fire
1. Thread model? (single loop + libuv pool) 2. nextTick vs Promise vs setTimeout? (nextTick→promise→timeout) 3. CPU work on loop? (blocks all) 4. Use all cores? (cluster/PM2) 5. CPU inside a process? (worker_threads) 6. libuv pool default? (4) 7. await in a loop? (serializes) 8. unhandled rejection? (crashes) 9. Backpressure signal? (write()→false) 10. Node best for? (IO-bound concurrency)

### 5 tricky
1. Why did one request freeze the whole server? (blocked loop) 2. Output order of nextTick/promise/timeout? 3. Why did parallel awaits overwhelm the DB? (unbounded Promise.all) 4. Why did the process crash with no catch? (unhandled rejection) 5. Why did memory blow up piping a big file? (ignored backpressure)

### 5 exercises
1. Predict and verify the output order of `nextTick`/Promise/`setTimeout`.
2. Block the loop with a sync loop; move it to a worker_thread and show the server stays responsive.
3. Convert serial `await`-in-loop to bounded-concurrency with a limit.
4. Add Express error middleware + `unhandledRejection` handler with graceful shutdown.
5. Pipe a large file with `pipeline` and explain where backpressure is handled.

### One-page revision sheet
> Node = **single-threaded event loop** (+ libuv pool, default 4) → great IO concurrency, **CPU/sync work blocks everything**. Phase order drains **microtasks (nextTick→Promises) before next macrotask (setTimeout/setImmediate)**. Scale cores = **cluster/PM2 (processes)**; CPU in-process = **worker_threads**. `await` in loop serializes → `Promise.all`/bounded pool. **Unhandled rejection crashes** modern Node → always catch; Express err middleware `(err,req,res,next)`. **Streams + backpressure**: `write()`→false, wait `'drain'`, use `pipeline`. vs JVM: Node adds processes not threads, fewer races, blocks on CPU. Reliability patterns (outbox/retry/DLQ/idempotency) are language-independent.

### Prerequisites to revisit
- Java concurrency contrast + virtual threads (ch.06); messaging/outbox/DLQ (ch.19); caching/Redis (ch.20).
