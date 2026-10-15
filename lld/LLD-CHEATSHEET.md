# LLD Interview Cheat Sheet — one page, drill it

Print this. It's the muscle memory for a 45-minute LLD round. The full reasoning is in [INTERVIEW.md](INTERVIEW.md) and [05-design-problems/README.md](05-design-problems/README.md); this is the condensed drill.

---

## The 7-step flow (say it out loud, in order)

1. **Clarify** — functional + non-functional. State assumptions and scope.
2. **Actors & use cases** — who uses it, what are the 2–3 core operations.
3. **Entities** — the nouns. One responsibility each (SRP).
4. **Relationships** — has-a (composition), is-a (inheritance), uses (association) + multiplicity.
5. **Interfaces first** — abstract whatever varies (payment, allocation, pricing). DIP + OCP.
6. **Patterns** — name them as you apply them, with the trade-off.
7. **Harden** — concurrency, idempotency, failure/edge handling (the SDE2 part).

---

## Clarifying questions to ask (pick the relevant ones)
- Scope: single machine or distributed? In-memory or persisted?
- Scale: how many users/requests concurrently?
- Which operations are must-have vs nice-to-have?
- Consistency needs? Can two users act on the same resource at once?
- What varies / will be extended later? (→ your abstractions)
- Any time element: expiry, scheduling, rate windows?

---

## 45-minute time-box
| Time | Phase |
|------|-------|
| 0–7 min | Clarify + list functional/non-functional + assumptions |
| 7–15 min | Entities, relationships, a rough class diagram |
| 15–20 min | Define key interfaces + name the patterns |
| 20–38 min | Code the core (happy path first), narrate as you go |
| 38–45 min | Concurrency/edge hardening + wrap-up ("here's what I'd add with more time") |

Don't code before the design is sketched. Don't silently code — narrate.

---

## Pattern picker (symptom → pattern)
| Symptom | Reach for |
|---------|-----------|
| `switch`/if-else on a type that keeps growing | **Strategy** / **Factory** (OCP) |
| Object behaves differently by lifecycle phase | **State** |
| Many objects must react to one change | **Observer** |
| One of several handlers should process a request | **Chain of Responsibility** |
| Add behavior to one object without subclass explosion | **Decorator** |
| Treat item and group of items the same (tree) | **Composite** |
| Complex subsystem, want a simple entry point | **Facade** |
| Need undo / queue / log of actions | **Command** |
| Build a complex object with many optional fields | **Builder** |
| Exactly one shared instance | **Singleton** (sparingly; prefer DI) |

---

## Concurrency picker (shared state → fix)
| Situation | Fix |
|-----------|-----|
| Single counter/flag | **atomic** (`AtomicInteger`) / `volatile` for a flag |
| "grab the last X" (spot/seat/driver) | **CAS** on an atomic (compareAndSet) |
| Compound action on a few fields | **synchronized** / **ReentrantLock** |
| Read-heavy shared structure | **ReadWriteLock** |
| Work handed between threads | **BlockingQueue** + **ExecutorService** |
| Must not run twice (payment, coupon) | **idempotency key** / atomic `add()` to a set |
| Independent resources contending | **per-key/striped locking**, not one global lock |

---

## Red flags that cost you points
- God class doing validation + persistence + business rules.
- Pattern-itis (abstraction with no second use case) — over-engineering.
- Check-then-act on shared state without atomicity (the classic race).
- Floating-point money (use integer cents).
- Reciting a pattern's structure without saying *why/when*.
- Coding before clarifying scope.

---

## Closing lines that land well
- "The core is done; with more time I'd add [X concurrency / persistence / this edge case]."
- "This abstraction means a new [type] is a new class, no change to the engine."
- "The thread-safety here comes from [primitive] because the shared mutable state is [what]."
