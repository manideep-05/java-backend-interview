# Low-Level Design (LLD) — Out of Scope (and How to Prepare It Separately)

> This repository is about **High-Level Design (HLD)**. LLD is a *different* interview round. This short page explains the boundary, what LLD covers, and how to prepare it — so you know what this repo does **not** teach and don't mistake the two.

## Why this page exists

This repo deliberately scopes to HLD (components, data stores, scaling, failure, trade-offs — see [what-is-system-design](./00-foundations/what-is-system-design.md) for the HLD vs LLD boundary). Many SDE-2 loops have a **separate LLD / object-oriented-design / machine-coding round**. This repo will not prepare you for that round — prepare it separately. (A few interviews *blend* HLD and LLD; the [15-practical-backend-examples](./15-practical-backend-examples/README.md) section helps there, but it's not a full LLD course.)

## HLD vs LLD (the boundary)

| | HLD (this repo) | LLD (separate) |
|---|---|---|
| Question | *What components exist and how do they interact?* | *How is one component built internally?* |
| Output | Architecture + data stores + APIs + capacity + failure modes | Class diagrams, interfaces, method signatures, patterns |
| Example | "API gateway → stateless app tier → Postgres + replicas → Redis → Kafka → workers" | "A `RetryPolicy` interface with `ExponentialBackoff`/`FixedBackoff` impls, injected into `NotificationSender`" |
| Round name | "System design" / "architecture" | "LLD" / "OOD" / "machine coding" |

See the full comparison in [what-is-system-design](./00-foundations/what-is-system-design.md) section B.

## What an LLD round tests (what to prepare)

- **Object-oriented design:** translating requirements into classes, interfaces, and relationships.
- **SOLID principles:** single responsibility, open/closed, Liskov, interface segregation, dependency inversion.
- **Design patterns:** Strategy, Factory, Observer, Builder, Adapter, Decorator, State, Singleton (used judiciously), etc. — and *when* each applies.
- **Extensibility & clean code:** designing so new requirements slot in without rewrites; clear abstractions.
- **Machine coding (some companies):** implement a working mini-system (e.g., a parking lot, a rate limiter, an in-memory cache, a splitwise, an elevator) in 60–90 min with tests.
- **Concurrency (sometimes):** thread-safety, locks, concurrent collections at the *code* level.
- **Common LLD problems:** parking lot, elevator system, library/BookMyShow seat booking (LLD angle), splitwise, in-memory key-value store, logger, tic-tac-toe/chess, rate limiter (class design), vending machine, cab booking (class design).

## How to prepare LLD (separate track)

1. **Learn the fundamentals:** SOLID + the common design patterns (what problem each solves, when to use/avoid).
2. **Practice the classic machine-coding problems** (parking lot, elevator, splitwise, in-memory cache, rate limiter) — design the classes/interfaces, then code one or two end-to-end with tests under a timer.
3. **Reuse the patterns you've built:** your resume (retry policies, idempotent handlers, outbox, a reusable notification client) is full of real LLD — practice articulating those as class designs. The [15-practical-backend-examples](./15-practical-backend-examples/README.md) snippets are a bridge (they show the code behind the HLD patterns).
4. **Suggested resources:** standard references on OOD/design patterns (e.g., *Head First Design Patterns*, the Gang of Four patterns), and LLD-focused practice sets / repositories for the classic problems. (These are general, well-known resources; pick ones current when you prepare.)

## The one-line takeaway

This repo makes you strong at the **HLD/architecture** round. For the **LLD/OOD/machine-coding** round, prepare SOLID + design patterns + the classic machine-coding problems separately — and lean on the real patterns you've already built in production.

---
*Return to [README](./README.md) · HLD vs LLD detail in [what-is-system-design](./00-foundations/what-is-system-design.md).*
