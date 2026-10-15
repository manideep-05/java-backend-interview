# SOLID Principles & Design Patterns — SDE2 Interview Prep

A hands-on learning repo (Java) covering **SOLID principles** and the classic **Gang of Four (GoF) design patterns**. Each topic answers the same five questions so you build real intuition, not just memorized definitions:

1. **What** is the principle/pattern?
2. **Why** does it exist? (the exact pain it removes)
3. **Where** is it used in real applications?
4. **How** is it applied? (code structure)
5. **Without vs With** — the same problem solved badly, then correctly.

---

## How to use this repo

For each topic:

1. Read the topic's `README.md` first — understand the problem before the code.
2. Open the `without/` package — see the smell/pain in working code.
3. Open the `with/` package — see the refactor, with detailed comments explaining *why* each change matters.
4. Try to explain the "why" out loud in one sentence. That's your interview answer.
5. Write what you learned in that topic's **`NOTES.md`** — a personal fill-in template that lives in every topic folder (your takeaways, the concept to focus on, where you got stuck, a 30-second interview explanation). These are the notes you re-read before an interview. They're yours to edit freely.

> **Interview tip:** Interviewers rarely ask "define SRP." They ask "here's a messy class, how would you improve it?" This repo is organized around that exact framing.

> **Prepping for interviews?** Start with the [**Interview Guide (INTERVIEW.md)**](INTERVIEW.md) for cross-cutting questions, the pattern-vs-pattern comparison traps, and an LLD-round walkthrough script. Every topic folder also ends with its own **Interview Questions & Traps** section.

---

## Folder structure

```
lld/
├── README.md                      <- you are here
├── STUDY-PLAN.md                  <- 4-week spaced-repetition plan + tracker
├── INTERVIEW.md                   <- cross-cutting interview guide
├── LLD-CHEATSHEET.md              <- one-page interview drill
├── UML-PRIMER.md                  <- draw & narrate a class diagram
├── LLD-TO-DISTRIBUTED.md          <- scaling LLD to distributed systems
├── 00-fundamentals/               <- OOP + concurrency primers (the warm-up round)
│   ├── oop/
│   └── concurrency/
├── 01-solid/
│   ├── README.md                  <- SOLID overview
│   ├── 1-srp/                     <- Single Responsibility
│   ├── 2-ocp/                     <- Open/Closed
│   ├── 3-lsp/                     <- Liskov Substitution
│   ├── 4-isp/                     <- Interface Segregation
│   └── 5-dip/                     <- Dependency Inversion
│
├── 02-creational/
│   ├── README.md
│   ├── singleton/
│   ├── factory-method/
│   ├── abstract-factory/
│   ├── builder/
│   └── prototype/
│
├── 03-structural/
│   ├── README.md
│   ├── adapter/
│   ├── decorator/
│   ├── facade/
│   ├── composite/
│   ├── proxy/
│   ├── bridge/
│   └── flyweight/
│
├── 04-behavioral/
│   ├── README.md
│   ├── strategy/
│   ├── observer/
│   ├── command/
│   ├── state/
│   ├── template-method/
│   ├── chain-of-responsibility/
│   ├── iterator/
│   ├── mediator/
│   ├── memento/
│   └── visitor/
│
└── 05-design-problems/            <- 24 full LLD interview problems
    ├── README.md                  <- LLD-solving framework + index
    ├── 01-parking-lot/
    ├── 02-movie-ticket-booking/
    └── ... (24 problems)
```

Each **principle/pattern** folder follows this layout:

```
<topic>/
├── README.md          <- what / why / where / how + interview Q&A
├── NOTES.md           <- YOUR personal notes (fill-in template)
├── without/           <- the problem (code smell in action)
└── with/              <- the solution (pattern/principle applied)
```

Each **design problem** folder follows this layout:

```
<problem>/
├── README.md          <- requirements, flows, edges, entities, design, interview Q&A
├── NOTES.md           <- YOUR personal notes (fill-in template)
├── normal/            <- clean interview solution (commented w/ patterns)
└── production/        <- thread-safe, concurrency-hardened version
```

---

## Index

### Start here
- [**Study Plan**](STUDY-PLAN.md) — a 4-week spaced-repetition schedule with a retention tracker.
- [**Fundamentals**](00-fundamentals) — OOP + concurrency primers (the interview warm-up round).
- [**LLD Cheat Sheet**](LLD-CHEATSHEET.md) — the one-page drill for interview day.
- [**UML Primer**](UML-PRIMER.md) — draw and narrate a class diagram fast.
- [**Interview Guide**](INTERVIEW.md) — cross-cutting Q&A and comparison traps.
- [**LLD → Distributed**](LLD-TO-DISTRIBUTED.md) — the "now scale it" follow-up.

### SOLID Principles ([details](01-solid/README.md))
| # | Principle | One-line why |
|---|-----------|--------------|
| S | [Single Responsibility](01-solid/1-srp) | A class should have one reason to change. |
| O | [Open/Closed](01-solid/2-ocp) | Open for extension, closed for modification. |
| L | [Liskov Substitution](01-solid/3-lsp) | Subtypes must be usable in place of their base type. |
| I | [Interface Segregation](01-solid/4-isp) | Don't force clients to depend on methods they don't use. |
| D | [Dependency Inversion](01-solid/5-dip) | Depend on abstractions, not concretions. |

### Creational Patterns ([details](02-creational/README.md))
*Concerned with how objects are created.*
| Pattern | One-line why |
|---------|--------------|
| [Singleton](02-creational/singleton) | Exactly one shared instance. |
| [Factory Method](02-creational/factory-method) | Let subclasses decide which concrete class to instantiate. |
| [Abstract Factory](02-creational/abstract-factory) | Create families of related objects. |
| [Builder](02-creational/builder) | Construct complex objects step by step. |
| [Prototype](02-creational/prototype) | Create new objects by cloning existing ones. |

### Structural Patterns ([details](03-structural/README.md))
*Concerned with how objects/classes are composed.*
| Pattern | One-line why |
|---------|--------------|
| [Adapter](03-structural/adapter) | Make incompatible interfaces work together. |
| [Decorator](03-structural/decorator) | Add behavior to objects dynamically. |
| [Facade](03-structural/facade) | A simple front for a complex subsystem. |
| [Composite](03-structural/composite) | Treat individual objects and groups uniformly. |
| [Proxy](03-structural/proxy) | A stand-in that controls access to another object. |
| [Bridge](03-structural/bridge) | Decouple an abstraction from its implementation. |
| [Flyweight](03-structural/flyweight) | Share objects to save memory. |

### Behavioral Patterns ([details](04-behavioral/README.md))
*Concerned with how objects communicate.*
| Pattern | One-line why |
|---------|--------------|
| [Strategy](04-behavioral/strategy) | Swap algorithms at runtime. |
| [Observer](04-behavioral/observer) | Notify many objects when one changes. |
| [Command](04-behavioral/command) | Turn a request into an object (undo, queue, log). |
| [State](04-behavioral/state) | Change behavior when internal state changes. |
| [Template Method](04-behavioral/template-method) | Fix the skeleton, let subclasses fill steps. |
| [Chain of Responsibility](04-behavioral/chain-of-responsibility) | Pass a request along a chain of handlers. |
| [Iterator](04-behavioral/iterator) | Traverse a collection without exposing its internals. |
| [Mediator](04-behavioral/mediator) | Centralize complex communication between objects. |
| [Memento](04-behavioral/memento) | Capture and restore an object's state. |
| [Visitor](04-behavioral/visitor) | Add operations to a class hierarchy without changing it. |

### Design Problems ([details](05-design-problems/README.md))
*24 full LLD interview problems, each with a `normal/` and a `production/` (thread-safe) solution.*
Parking Lot, Movie Ticket Booking, Vending Machine, Splitwise, Elevator, E-commerce Checkout, Notification Framework, Document Management, LRU Cache, Hotel Reservation, Logging Framework, Rate Limiter, Ride Sharing, File System, Food Delivery, Inventory Management, Chess, ATM, Task Scheduler, Pub/Sub Message Queue, KV Store with TTL, Snake & Ladders, Tic-Tac-Toe, Discount Engine. See the [playbook](05-design-problems/README.md) for the reusable solving framework.

---

## Running the code

Each example is plain Java with a `main` method where useful. No build tool required:

```bash
# from a topic's folder
javac with/*.java && java <MainClassName>
```

> All code targets Java 11+ and uses no external dependencies, so it stays focused on the concept.
