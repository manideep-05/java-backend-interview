# Singleton Pattern

> Ensure a class has **only one instance** and provide a global point of access to it.

## What
A class that controls its own instantiation so that exactly one object ever exists, shared across the whole app.

## Why it exists
Some resources are inherently single: an app-wide configuration, a logger, a thread pool, a connection pool. Creating multiple copies wastes resources or causes inconsistent state (two config objects disagreeing).

## Where it's used in real apps
- `java.lang.Runtime.getRuntime()`.
- Spring beans are singletons by default.
- Loggers (Log4j), config managers, caches, connection pools.

## The exact problem (see `without/`)
Each part of the app does `new ConfigManager()` and reads a config file again. You get multiple objects, repeated expensive file loads, and no single source of truth — change one, the others don't see it.

## How it's applied (see `with/`)
Private constructor + a static accessor returning the one instance. We show the **thread-safe, lazy** version using the "initialization-on-demand holder" idiom, which is lazy, thread-safe, and needs no synchronization.

## Trade-offs / interview notes
- Singletons are effectively global state — can hide dependencies and make testing harder (prefer DI where possible).
- Watch out for thread safety during lazy init, and for serialization/reflection breaking the single-instance guarantee.
- The **enum** singleton is the simplest fully-safe form in Java (handles serialization and reflection).

---

## Interview Questions & Traps

**Q: How do you make a Singleton thread-safe?**
Options, best to acceptable: (1) **enum** singleton — simplest, handles serialization and reflection; (2) **initialization-on-demand holder** (used in `with/`) — lazy, thread-safe, no locking; (3) **double-checked locking** with a `volatile` field; (4) eager `static final` instance — simple but not lazy.

**Q: Why is double-checked locking tricky?**
Without `volatile`, another thread can see a partially-constructed instance due to instruction reordering. The field must be `volatile` for DCL to be correct.

**Q: How can a Singleton's single-instance guarantee be broken?**
- **Reflection** can call the private constructor.
- **Serialization/deserialization** can create a second instance (fix with `readResolve()`).
- **Cloning**.
- **Multiple classloaders** each load their own copy.
The enum singleton is immune to reflection and serialization attacks.

**Follow-ups**
- *"Why do people call Singleton an anti-pattern?"* — It's global mutable state: hides dependencies, couples code to a static accessor, and makes unit testing hard (can't easily substitute a mock).
- *"How would you make a Singleton testable?"* — Prefer DI: register a single instance in a container and inject it, rather than a hard `getInstance()` call.
- *"Spring beans are singletons — same thing?"* — Conceptually one instance per container, but managed and injectable, avoiding the global-static downsides.

**Traps to avoid**
- Lazy init without synchronization in a multithreaded app (race creates two instances).
- Forgetting serialization/reflection can duplicate it.
- Putting mutable shared state in a Singleton and ignoring concurrency on that state.
