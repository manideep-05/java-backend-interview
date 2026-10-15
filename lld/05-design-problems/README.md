# LLD Design Problems — Interview Playbook

19 classic low-level-design problems, each solved the way you'd do it in an interview: talk through requirements, flows, and edge cases first, discover the entities, then design classes and interfaces. Every problem ships **two implementations**:

- **`normal/`** — the clean interview answer. Correct, well-factored, commented with the SOLID principles and GoF patterns in play. This is what you'd write on a whiteboard / shared editor.
- **`production/`** — the same design hardened for the real world: thread safety, concurrency control, idempotency, and failure handling. Every `synchronized` / lock / atomic is commented with *why* it's there.

> Read the problem README first, then `normal/`, then `production/`. The jump from normal → production is itself a common senior-level interview follow-up ("now make it thread-safe / handle concurrent users").

---

## A reusable framework for ANY LLD problem

Interviewers grade your *process* as much as your final diagram. Use this sequence out loud:

### 1. Clarify requirements (functional + non-functional)
- **Functional**: what must it *do*? List the use cases ("book a seat", "park a car").
- **Non-functional**: scale, concurrency, latency, persistence. Ask: "single machine or distributed?", "how many concurrent users?" Narrow scope so you don't boil the ocean.
- State your assumptions explicitly. Interviewers want to see you bound the problem.

### 2. Identify the critical flows
- Pick the 1–3 core happy paths and walk them step by step (e.g. *park → get ticket → pay → exit*).
- These flows reveal the operations your classes must support.

### 3. Find edge cases (and say how you detect them)
Systematic sources of edges:
- **Empty / full / boundary**: lot full, cache at capacity, zero balance.
- **Concurrency**: two users grab the last seat/spot at once.
- **Invalid input / state**: pay before selecting, cancel after completion.
- **Failure / partial**: payment succeeds but booking fails — who rolls back?
- **Time**: reservation expiry, rate-limit windows, idempotency windows.
Name the edge, then say how you'd handle it (reject, retry, lock, compensate).

### 4. Discover entities, relationships, responsibilities
- **Entities**: the nouns in the requirements (ParkingLot, Spot, Ticket, Vehicle).
- **Relationships**: has-a (composition), is-a (inheritance), uses (association). A lot *has* levels; a level *has* spots.
- **Responsibilities**: assign one clear job per class (SRP). "Who owns pricing?" → a `PricingStrategy`, not the `Ticket`.
- Heuristic: if a class is doing validation + persistence + business rules, split it.

### 5. Design classes and interfaces
- **Interfaces first** for anything that varies (payment methods, allocation strategies) — this is DIP + OCP.
- Prefer **composition over inheritance**; use inheritance only for true is-a.
- Keep a thin orchestration layer (a service/manager) that wires entities together — the Facade for your subsystem.
- Call out patterns by name as you introduce them, with the trade-off.

### 6. (Senior) Harden it
- Where is shared mutable state? Guard it (lock, atomic, concurrent collection).
- Is any operation "check-then-act"? That's a race — make it atomic.
- Idempotency for payments/bookings. Timeouts and retries for I/O.
- This is exactly the `normal/` → `production/` jump in each folder.

---

## Patterns you'll reuse across these problems

| Pattern | Shows up in |
|---------|-------------|
| **Strategy** | spot allocation, pricing, payment, matching, eviction policy, scheduling |
| **State** | vending machine, ATM, order/booking/ride lifecycle |
| **Factory** | creating vehicles, pieces, notifications, payment handlers |
| **Singleton** | the central manager/registry (used sparingly; prefer DI) |
| **Observer** | notifications, elevator/display updates |
| **Composite** | file system, document folders |
| **Chain of Responsibility** | logging levels, checkout validation, approval |
| **Facade** | the top-level service that orchestrates the subsystem |

---

## Index

| # | Problem | Core patterns | Headline concurrency concern |
|---|---------|---------------|------------------------------|
| 1 | [Parking Lot](01-parking-lot) | Strategy, Factory, Singleton | Two cars, one last spot |
| 2 | [Movie Ticket Booking](02-movie-ticket-booking) | State, Strategy | Two users, one seat; hold expiry |
| 3 | [Vending Machine](03-vending-machine) | State | Concurrent coin insert / dispense |
| 4 | [Splitwise](04-splitwise) | Strategy | Concurrent expense updates to balances |
| 5 | [Elevator System](05-elevator-system) | Strategy, State | Concurrent floor requests |
| 6 | [E-commerce Checkout & Payment](06-ecommerce-checkout) | State, Strategy | Idempotent payment; stock decrement |
| 7 | [Notification Framework](07-notification-framework) | Strategy, Observer, Factory | Async fan-out, retries |
| 8 | [Document Management System](08-document-management) | Composite, Memento | Concurrent edits, versioning |
| 9 | [LRU Cache](09-lru-cache) | — (HashMap + DLL) | Concurrent get/put eviction |
| 10 | [Hotel / Restaurant Reservation](10-hotel-reservation) | Strategy | Double-booking the same room |
| 11 | [Logging Framework](11-logging-framework) | Chain of Responsibility, Strategy, Singleton | Concurrent writers, async queue |
| 12 | [Rate Limiter](12-rate-limiter) | Strategy | Atomic token/counter updates |
| 13 | [Ride Sharing](13-ride-sharing) | Strategy, State | Matching a driver to one rider |
| 14 | [File System](14-file-system) | Composite | Concurrent path operations |
| 15 | [Food Delivery](15-food-delivery) | State, Strategy | Order assignment to one agent |
| 16 | [Inventory Management](16-inventory-management) | Observer, Strategy | Atomic stock decrement / oversell |
| 17 | [Chess Game](17-chess-game) | Strategy, Factory | (mostly single-threaded) move validity |
| 18 | [ATM Machine](18-atm-machine) | State | Concurrent withdrawal on one account |
| 19 | [Task Scheduler](19-task-scheduler) | Strategy, Priority Queue | Concurrent submit; worker pool |
| 20 | [Pub/Sub Message Queue](20-pubsub-message-queue) | Observer, Producer/Consumer | Async fan-out; retries + DLQ; per-subscriber isolation |
| 21 | [KV Store with TTL](21-kv-store-ttl) | — (map + expiry) | get/expiry race; lazy + sweeper |
| 22 | [Snake and Ladders](22-snake-and-ladders) | — (jump map), DI/Strategy | Online turn integrity |
| 23 | [Tic-Tac-Toe](23-tic-tac-toe) | — (incremental win check) | Online turn integrity |
| 24 | [Discount / Coupon Engine](24-discount-engine) | Strategy, Chain of Responsibility | Atomic single-use coupon |

> Problems 20–24 were added to cover high-frequency topics (messaging, caching, game modeling, rules engines) that map closely to real backend work.

Each folder's README ends with **Interview Questions, Follow-ups & Traps** specific to that problem.
