# Task Scheduler — LLD

Design something like a cron / `ScheduledExecutorService`. The signals are a priority/time-ordered queue and a worker pool draining it safely.

---

## 1. Requirements

**Functional**
- Submit a task to run now, after a **delay**, or at a **priority**.
- Execute tasks; higher priority / earlier scheduled time first.
- (Optional) recurring tasks at a fixed interval.

**Non-functional**
- Concurrent submission from many producers; a pool of workers executes.
- A task failing must not kill the worker or the scheduler.

---

## 2. Why these patterns

- **Priority queue** ordered by (runAt, priority) — the core data structure.
- **Strategy** (optional) for the ordering policy.
- **Command**: each task is a self-contained unit of work (a `Runnable`).
- Producer/consumer: submitters enqueue; workers dequeue and run.

---

## 3. Critical flow

submit(task, delay, priority) → compute `runAt = now + delay`, enqueue into a priority queue ordered by `runAt` then `priority` → a worker waits until the head task's `runAt`, dequeues it, and runs it (recurring tasks re-enqueue with the next run time).

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Timing | Task not due yet | Worker waits (timed) until the head is due, not busy-spin. |
| Concurrency | Many producers + many workers | Thread-safe priority queue (`PriorityBlockingQueue` / `DelayQueue`). |
| Failure | A task throws | Catch per task; log; keep the worker alive. |
| Ordering | Equal runAt | Break ties by priority, then FIFO (sequence number) for stability. |
| Shutdown | Pending tasks on stop | Drain or cancel; define the policy. |

---

## 5. Entities, relationships, responsibilities

- **Task**: a `Runnable` + `runAt` + `priority` + sequence. *Responsibility:* carry work + ordering keys.
- **Scheduler**: holds the queue + worker threads; `schedule()`; lifecycle.
- **OrderingStrategy** (optional): comparator for the queue. → Strategy.

---

## 6. Class/interface design

```
Scheduler
   ├── PriorityBlockingQueue<Task> (ordered by runAt, then priority)
   └── worker threads: take() head, wait until due, run()
Task (Runnable) { runAt, priority, seq }
```

### normal vs production
- `normal/` — a single-threaded loop with a `PriorityQueue`: pop the earliest-due task, (conceptually) wait, run it. Shows the ordering and the run loop clearly.
- `production/` — a `DelayQueue` (or `PriorityBlockingQueue` + timed wait) with a worker **thread pool**; `take()` blocks until the head is due; each run is wrapped in try/catch so one failing task never kills a worker; clean shutdown drains/stops.

---

## Interview Questions, Follow-ups & Traps

**Q: What data structure and why?**
A priority queue ordered by run time (then priority). It gives O(log n) insert and O(1) peek of the next-due task — exactly what a scheduler needs. For "wait until due," a `DelayQueue` blocks the consumer until the head's delay elapses.

**Q: How do workers wait efficiently for a future task?**
Timed blocking, not spinning. `DelayQueue.take()` parks the thread until the earliest task is due. If you roll your own, use a lock + `Condition.awaitUntil(runAt)` and re-check on wakeups.

**Q: A task throws — what happens?**
Wrap each execution in try/catch inside the worker so the exception is logged/handled and the worker loops to the next task. An uncaught exception would otherwise kill the worker thread and shrink your pool.

**Follow-ups**
- *"Recurring tasks?"* — On completion, re-enqueue with `runAt += interval`. Beware drift (schedule from the scheduled time, not the finish time).
- *"Cancel a scheduled task?"* — Return a handle; mark it cancelled; workers skip cancelled tasks (or remove from the queue).
- *"Distributed scheduler?"* — Persist tasks in a store; workers across machines claim due tasks atomically (DB/Redis lease) to avoid double execution.

**Traps**
- Busy-waiting instead of timed blocking.
- A task exception killing the worker.
- Unstable ordering for equal timestamps (add a sequence number).
- Unbounded queue growth with no backpressure.
