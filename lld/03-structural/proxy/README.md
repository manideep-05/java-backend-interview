# Proxy Pattern

> Provide a surrogate or placeholder for another object to **control access** to it.

## What
A stand-in object that implements the same interface as the real object and controls access to it — adding lazy loading, caching, access control, or logging around the real work.

## Common proxy flavors
- **Virtual proxy**: delay creating an expensive object until it's actually needed (lazy init).
- **Protection proxy**: enforce access rights before delegating.
- **Caching proxy**: remember results to avoid repeating expensive calls.
- **Remote proxy**: local stand-in for an object living in another process/machine.

## Why it exists
You want to add access control around an object without changing the object or the client. Because the proxy shares the real object's interface, the client can't tell the difference.

## Where it's used in real apps
- Hibernate lazy-loading proxies, Spring AOP proxies.
- `java.lang.reflect.Proxy` (dynamic proxies).
- RMI stubs, API rate-limiting/caching layers.

## The exact problem (see `without/`)
A `RealImage` loads a large file from disk in its constructor. Creating the object is costly even if `display()` is never called.

## How it's applied (see `with/`)
An `ImageProxy` (virtual proxy) implements the same `Image` interface and defers constructing the heavy `RealImage` until `display()` is first called — then caches it.

---

## Interview Questions & Traps

**Q: Name the proxy types and a use for each.**
Virtual (lazy-create expensive objects), Protection (access control), Caching (memoize results), Remote (stand-in for an object in another process), Smart reference (ref counting, logging).

**Q: Proxy vs Decorator?** *(trap)*
Both share the subject's interface. Proxy **controls access** (and often creates/manages the real subject itself). Decorator **adds behavior** and is given the component to wrap. A proxy usually controls the lifecycle; a decorator doesn't.

**Q: How does Spring/Hibernate use proxies?**
Spring AOP wraps beans in proxies to add transactions/logging/security. Hibernate returns lazy-loading proxies for associations, hydrating them on first access.

**Follow-ups**
- *"Dynamic vs static proxy in Java?"* — Static: a hand-written class. Dynamic: `java.lang.reflect.Proxy` (interfaces) or CGLIB/ByteBuddy (classes) generated at runtime.
- *"Thread safety of a lazy virtual proxy?"* — The lazy-init check needs synchronization (or holder idiom) to avoid double creation under concurrency.
- *"Proxy vs Facade?"* — Proxy has the same interface as one subject; Facade is a new simplified interface over many classes.

**Traps to avoid**
- Unsynchronized lazy initialization in a concurrent proxy.
- Proxy silently changing behavior/results (it should be transparent to the client).
- Confusing it with Decorator (access control vs added behavior).
