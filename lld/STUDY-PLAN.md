# Study Plan — Spaced Repetition for SDE2 LLD

A 4-week plan built around **spaced repetition**: you don't learn a topic once, you revisit it at growing intervals (day 1 → +1d → +3d → +7d) so it sticks for interview day. Tuned to your backend background — the topics that match your resume (messaging, caching, idempotency, concurrency) are front-loaded and revisited most.

> How to use it: do the day's **New** items, then the day's **Review** items (quick — just the one-line "why" and the trap). Tick the boxes. If a review feels shaky, re-read that topic's README and reset its interval.

---

## The spaced-repetition rule
When you learn a topic, schedule 4 touches:
- **T0** — learn it (read README → attempt code → fill NOTES.md).
- **T+1 day** — recall the one-liner + the main trap, no notes.
- **T+3 days** — re-derive the design / re-code the core from scratch.
- **T+7 days** — explain it out loud in 60 seconds as if interviewing.

A topic is "retained" once it survives the T+7 touch.

---

## Daily rhythm (≈60–90 min)
1. **Warm-up (5 min):** one fundamentals card (`00-fundamentals`).
2. **Reviews first (15–25 min):** clear the day's due reviews.
3. **New topic(s) (40–60 min):** read → code → NOTES.
4. **Cool-down (5 min):** update your retention tracker below.

Reviews before new material — that's the whole point of spaced repetition.

---

## Week 1 — Fundamentals + SOLID + core patterns + your-strength problems

| Day | New | Review (due) |
|-----|-----|--------------|
| 1 | Fundamentals: OOP primer; SRP, OCP | — |
| 2 | Fundamentals: Concurrency primer; LSP, ISP, DIP | OOP, SRP, OCP |
| 3 | Strategy, Observer, Factory Method | Concurrency, LSP, ISP, DIP |
| 4 | **Pub/Sub (20)**, **KV-store TTL (21)** *(your strengths)* | OOP; Strategy, Observer, Factory |
| 5 | State, Singleton; Rate Limiter (12) | Concurrency; Pub/Sub, KV-store |
| 6 | LRU Cache (09), Decorator | SRP/OCP/LSP/ISP/DIP (SOLID sweep); State, Singleton |
| 7 | **Rest / catch-up.** Re-code one shaky topic from scratch. | Strategy, Observer, Factory, Rate Limiter |

End of week check: can you explain all 5 SOLID + Strategy/Observer/Factory/State and code LRU + token-bucket from scratch?

---

## Week 2 — Remaining patterns + concurrency-heavy problems

| Day | New | Review (due) |
|-----|-----|--------------|
| 8 | Builder, Prototype, Abstract Factory | Pub/Sub, KV-store (T+... ), LRU, Decorator |
| 9 | Adapter, Facade, Composite | State, Singleton; Rate Limiter |
| 10 | Proxy, Bridge, Flyweight | Builder, Prototype, Abstract Factory |
| 11 | Command, Template Method, Chain of Responsibility | Adapter, Facade, Composite; LRU |
| 12 | Parking Lot (01), Vending Machine (03) | Proxy, Bridge, Flyweight |
| 13 | Movie Booking (02), ATM (18) *(concurrency focus)* | Command, Template Method, CoR |
| 14 | **Rest / catch-up.** Mock: pick a random problem, 45-min timed. | Parking Lot, Vending Machine; Decorator |

End of week check: all 22 GoF patterns explainable; the "last-spot/seat/account" race fix (CAS / lock) is automatic.

---

## Week 3 — Breadth of design problems

| Day | New | Review (due) |
|-----|-----|--------------|
| 15 | Iterator, Mediator, Memento, Visitor | Parking Lot, Vending, Movie, ATM |
| 16 | E-commerce Checkout (06), Inventory (16) | Builder/Prototype/Abstract Factory |
| 17 | Notification Framework (07), Logging (11) *(your strengths)* | Checkout, Inventory; Adapter/Facade/Composite |
| 18 | Hotel Reservation (10), Ride Sharing (13) | Notification, Logging; Proxy/Bridge/Flyweight |
| 19 | Food Delivery (15), Elevator (05) | Hotel, Ride Sharing; Command/Template/CoR |
| 20 | Discount Engine (24), Task Scheduler (19) | Food Delivery, Elevator; Iterator/Mediator/Memento/Visitor |
| 21 | **Rest / catch-up.** Mock: timed Pub/Sub or Notification (lean on resume). | Checkout, Inventory, Notification, Logging |

End of week check: you can drive any problem through the 7-step flow and reach a working `normal/` design in ~30 min.

---

## Week 4 — Remaining problems, mocks, and distributed bridge

| Day | New | Review (due) |
|-----|-----|--------------|
| 22 | Document Management (08), File System (14) | Discount, Task Scheduler; Hotel, Ride, Food, Elevator |
| 23 | Chess (17), Snake & Ladders (22), Tic-Tac-Toe (23) | Document, File System |
| 24 | **LLD → Distributed** deep read; map 3 problems to distributed form | Chess, Snake&Ladders, Tic-Tac-Toe |
| 25 | **Mock 1 (timed 45 min):** a game or rules problem. Self-score. | SOLID sweep; Strategy/State/Observer |
| 26 | **Mock 2 (timed 45 min):** a concurrency problem (booking/inventory). | Checkout, Inventory, Movie, ATM |
| 27 | **Mock 3 (timed 45 min):** a messaging/caching problem (your strength). | Pub/Sub, KV-store, Notification, Logging |
| 28 | **Final sweep:** re-read `LLD-CHEATSHEET` + `UML-PRIMER`; weak-topic triage. | anything still shaky |

End of week check: three clean 45-min mocks done; cheat-sheet flow is muscle memory.

---

## Mock interview protocol (use for Week 4 mocks)
1. Set a 45-minute timer. Pick a problem you haven't done recently.
2. Follow the [cheat sheet](LLD-CHEATSHEET.md) flow out loud (or write it): clarify → actors → entities → relationships → interfaces → patterns → harden.
3. Draw the class diagram ([UML primer](UML-PRIMER.md)) before coding.
4. Code the `normal/` happy path; then verbally harden for concurrency.
5. **Self-score (1–5):** requirements clarified? entities/responsibilities clean? right patterns named with trade-offs? concurrency fix correct? edge cases called out? narrated throughout?
6. Only after scoring, compare with the repo's `README` + `production/`. Note the gaps in that problem's `NOTES.md`.

---

## Retention tracker
Mark the date of each touch. A topic is "done" when T+7 is ticked and solid.

### Fundamentals & principles
| Topic | T0 | T+1 | T+3 | T+7 | Solid? |
|-------|----|-----|-----|-----|--------|
| OOP primer | | | | | [ ] |
| Concurrency primer | | | | | [ ] |
| SRP | | | | | [ ] |
| OCP | | | | | [ ] |
| LSP | | | | | [ ] |
| ISP | | | | | [ ] |
| DIP | | | | | [ ] |

### Patterns
| Topic | T0 | T+1 | T+3 | T+7 | Solid? |
|-------|----|-----|-----|-----|--------|
| Strategy | | | | | [ ] |
| Observer | | | | | [ ] |
| Factory Method | | | | | [ ] |
| Abstract Factory | | | | | [ ] |
| Builder | | | | | [ ] |
| Prototype | | | | | [ ] |
| Singleton | | | | | [ ] |
| Adapter | | | | | [ ] |
| Decorator | | | | | [ ] |
| Facade | | | | | [ ] |
| Composite | | | | | [ ] |
| Proxy | | | | | [ ] |
| Bridge | | | | | [ ] |
| Flyweight | | | | | [ ] |
| State | | | | | [ ] |
| Command | | | | | [ ] |
| Template Method | | | | | [ ] |
| Chain of Responsibility | | | | | [ ] |
| Iterator | | | | | [ ] |
| Mediator | | | | | [ ] |
| Memento | | | | | [ ] |
| Visitor | | | | | [ ] |

### Design problems
| Problem | T0 | T+1 | T+3 | T+7 | Solid? |
|---------|----|-----|-----|-----|--------|
| Parking Lot | | | | | [ ] |
| Movie Booking | | | | | [ ] |
| Vending Machine | | | | | [ ] |
| Splitwise | | | | | [ ] |
| Elevator | | | | | [ ] |
| E-commerce Checkout | | | | | [ ] |
| Notification Framework | | | | | [ ] |
| Document Management | | | | | [ ] |
| LRU Cache | | | | | [ ] |
| Hotel Reservation | | | | | [ ] |
| Logging Framework | | | | | [ ] |
| Rate Limiter | | | | | [ ] |
| Ride Sharing | | | | | [ ] |
| File System | | | | | [ ] |
| Food Delivery | | | | | [ ] |
| Inventory Management | | | | | [ ] |
| Chess | | | | | [ ] |
| ATM | | | | | [ ] |
| Task Scheduler | | | | | [ ] |
| Pub/Sub Message Queue | | | | | [ ] |
| KV Store with TTL | | | | | [ ] |
| Snake & Ladders | | | | | [ ] |
| Tic-Tac-Toe | | | | | [ ] |
| Discount Engine | | | | | [ ] |

---

## If you have less time
- **2 weeks:** do Week 1 as-is, then merge Weeks 2–4 — all patterns at a skim (README + one-liner), but fully code the 10 highest-frequency problems: Parking Lot, LRU, Rate Limiter, Movie Booking, Pub/Sub, KV-store, Notification, Checkout, Elevator, Tic-Tac-Toe. Keep all three mocks.
- **1 week (crunch):** Fundamentals + the pattern-vs-pattern traps from `INTERVIEW.md`, the `LLD-CHEATSHEET`, and 6 problems (Parking Lot, LRU, Rate Limiter, Pub/Sub, Checkout, Movie Booking) + 2 mocks. Lead every answer with the resume-matching framing.

## Signals you're interview-ready
- You run the 7-step flow without looking at the cheat sheet.
- For any problem you can state the entities, the key interface(s), and the one concurrency risk in under 2 minutes.
- You name patterns *with* their trade-off, and you catch yourself before over-engineering.
- You can take any `normal/` solution and describe its `production/` hardening (and the distributed version) unprompted.
