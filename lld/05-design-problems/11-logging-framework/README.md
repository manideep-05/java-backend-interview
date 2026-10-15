# Logging Framework — LLD

A "design a library" problem showcasing **Chain of Responsibility** (level handlers) and **Strategy** (appenders/formatters), with an async-logging concurrency follow-up.

---

## 1. Requirements

**Functional**
- Log at levels: DEBUG < INFO < WARN < ERROR.
- A configured minimum level filters out lower messages.
- Multiple **appenders** (console, file) — a message can go to several.
- Pluggable **formatting**.

**Non-functional**
- Many threads log concurrently → appenders must be thread-safe.
- Logging must be cheap on the hot path → async appender option.

---

## 2. Why these patterns

- **Chain of Responsibility** for level handlers: a `DebugHandler → InfoHandler → ErrorHandler` chain; each handles messages at/above its level or passes along. (Classic textbook use.)
- **Strategy** for appenders (where output goes) and formatters (how it looks).
- **Singleton** for the logger registry/root logger (used carefully).

---

## 3. Critical flow

`log(level, msg)` → if `level >= configuredMin`, format → dispatch to each appender. In the CoR variant, the message walks the handler chain and the matching handler writes it.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Filtering | Message below min level | Dropped cheaply (early return). |
| Concurrency | Many threads write a file | Appender writes are synchronized / use a concurrent queue. |
| Perf | Logging on a hot path | Async appender: enqueue, a background thread writes. |
| Loss | App crashes with buffered logs | Flush on shutdown; bounded queue with a drop/block policy. |
| Ordering | Interleaved lines from threads | Serialize the actual write. |

---

## 5. Entities, relationships, responsibilities

- **LogLevel** (enum, ordered).
- **LogHandler** (CoR): `setNext`, `handle(level, msg)`.
- **Appender** (Strategy): `append(formattedMessage)` → Console / File / Async.
- **Formatter** (Strategy): `format(level, msg)`.
- **Logger**: entry point; owns min level + appenders (or the handler chain).

---

## 6. Class/interface design

```
Logger.log(level, msg)
   └── LogHandler chain (Debug -> Info -> Warn -> Error)   CoR
         each handler uses an Appender (Console/File)       STRATEGY
```

### normal vs production
- `normal/` — a CoR chain of level handlers writing to the console; synchronous.
- `production/` — an **AsyncAppender** decouples callers from I/O via a bounded `BlockingQueue` drained by a background worker thread; file writes are serialized; flush-on-shutdown avoids losing buffered logs.

---

## Interview Questions, Follow-ups & Traps

**Q: Why Chain of Responsibility for levels?**
Each level handler decides to handle or pass the message along, so you can insert/reorder/remove handlers (e.g. add an audit handler) without rewriting a big `switch(level)`. It's the canonical CoR example.

**Q: How do you make logging cheap under high throughput?**
Async appender: the caller just enqueues the message to a bounded queue and returns; a dedicated worker thread performs the slow I/O. The hot path pays only an enqueue, not a disk write.

**Q: Bounded vs unbounded async queue?** *(trap)*
Unbounded risks OOM if producers outpace the writer. Bounded needs a policy when full: block the producer (backpressure), or drop (lose logs). State the trade-off; logging usually tolerates dropping over crashing.

**Follow-ups**
- *"Guarantee no lost logs on shutdown?"* — Flush the queue on shutdown (drain + join the worker). Accept that a hard crash may still lose buffered entries.
- *"Per-class log levels?"* — A logger registry keyed by name with hierarchical level inheritance (like Log4j).
- *"Structured/JSON logs?"* — A different Formatter strategy.

**Traps**
- Synchronous file I/O on the hot path.
- Unbounded async queue (memory blowup).
- Unsynchronized concurrent writes (interleaved/corrupted lines).
- Not flushing on shutdown (silent log loss).
