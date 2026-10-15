# Interface Segregation Principle (ISP)

> No client should be forced to depend on methods it does not use.

## What
Prefer many small, role-specific interfaces over one large "fat" interface. A class should only have to implement what it actually needs.

## Why it exists
A fat interface forces implementers to provide dummy/empty/throwing methods for behavior they don't have. Those stubs are lies that callers can trip over, and a change to one unrelated method on the fat interface recompiles everyone.

## The classic trap
A `Worker` interface with `work()` **and** `eat()`. A `RobotWorker` can `work()` but can't `eat()` — so it's forced to throw or no-op on `eat()`.

## Where it's used in real apps
- Java's own split: `Runnable`, `Callable`, `Closeable`, `AutoCloseable` are tiny focused interfaces instead of one giant one.
- Repository interfaces split into `ReadRepository` / `WriteRepository` (CQRS-ish).
- Printer/scanner/fax: a simple printer shouldn't implement scan and fax.

## The exact problem (see `without/`)
A fat `Machine` interface forces `OldPrinter` to implement `scan()` and `fax()` by throwing `UnsupportedOperationException`.

## How it's applied (see `with/`)
Split into `Printer`, `Scanner`, `Fax`. Each device implements only the capabilities it truly has. A `SimplePrinter` implements only `Printer`.

---

## Interview Questions & Traps

**Q: State ISP.**
Clients shouldn't be forced to depend on methods they don't use. Prefer several small, role-specific interfaces over one fat interface.

**Q: What's the symptom of an ISP violation?**
Classes implementing an interface with empty/no-op bodies or methods that throw `UnsupportedOperationException` — they're being forced to "implement" things they don't support.

**Q: How does ISP differ from SRP?**
SRP is about a *class* having one reason to change. ISP is about *interfaces* being focused so clients only depend on what they use. They're related (both about cohesion) but applied at different levels.

**Follow-ups**
- *"Give a JDK example."* — `Runnable`, `Callable`, `Closeable`, `AutoCloseable` are small focused interfaces rather than one giant lifecycle interface.
- *"Can you have too many interfaces?"* — Yes; over-segregation creates noise. Split by genuine client roles, not every method.
- *"How does ISP interact with LSP?"* — Fat interfaces push implementers toward throwing stubs, which then violate LSP. ISP removes that pressure.

**Traps to avoid**
- Splitting interfaces by implementation detail instead of by client need (role interfaces).
- Thinking ISP is only about interfaces — the underlying idea (depend only on what you use) applies to any dependency.
- A single "god" service interface with 20 methods where each client uses only two.
