# Elevator System — LLD

An open-ended scheduling problem. Interviewers watch how you bound scope and pick a scheduling policy, then push on concurrency and multi-car dispatch.

---

## 1. Requirements

**Functional**
- Multiple floors; one or more elevator cars.
- Two request kinds: **external** (hall call: floor + up/down) and **internal** (car button: target floor).
- Move the car to serve requests; open/close doors; show direction.

**Non-functional**
- Requests arrive concurrently from many floors/riders → the request queue must be thread-safe.
- Scheduling policy should be swappable (FCFS vs SCAN/LOOK "elevator algorithm").

**Scope control:** start with one car; mention multi-car dispatch as an extension.

---

## 2. Why these patterns

- **Strategy** for the scheduling algorithm (which request next / which direction). FCFS is naive; SCAN/LOOK sweeps in one direction then reverses, which is how real elevators behave.
- **State** for the car's motion (Idle / MovingUp / MovingDown / DoorsOpen) — behavior depends on current motion.

---

## 3. Critical flow

Rider presses hall button → request enqueued → scheduler picks next target based on current floor + direction → car moves one floor at a time, serving stops along the way → doors open at a served floor → continue until queue empty → Idle.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Direction | Request behind current direction | SCAN defers it until the sweep reverses. |
| Duplicate | Same floor requested twice | Dedupe in the request set. |
| Concurrency | Many simultaneous hall calls | Thread-safe queue; pulls are atomic. |
| Boundary | Request for current floor | Open doors immediately, don't move. |
| Idle start | No pending requests | Car waits in Idle. |
| Multi-car | Which car serves a hall call? | Dispatcher picks nearest/least-busy (extension). |

---

## 5. Entities, relationships, responsibilities

- **Elevator (car)**: current floor, direction, state; serves requests. *Responsibility:* motion + door control.
- **Request**: target floor + (for hall calls) desired direction.
- **SchedulingStrategy** (interface): given current floor/direction and pending requests, pick the next target. → **Strategy**.
- **ElevatorController / Dispatcher**: owns cars, routes external requests to a car. *Responsibility:* dispatch, not motion.

---

## 6. Class/interface design

```
ElevatorController ──> Elevator(car) ──> SchedulingStrategy (FCFS / SCAN)
     routes hall calls      has: currentFloor, Direction, State, pending requests
```

### normal vs production
- `normal/` — one car, a `TreeSet` of pending floors, SCAN-style "next in current direction" selection, single-threaded `step()` simulation.
- `production/` — pending requests in a thread-safe structure; requests can be submitted from many threads while a dedicated car thread processes them; the car loop and submission are decoupled (producer/consumer).

---

## Interview Questions, Follow-ups & Traps

**Q: What scheduling algorithm would you use and why?**
FCFS is simple but inefficient (lots of direction changes). SCAN/LOOK ("elevator algorithm") sweeps up serving all up-requests, then sweeps down — minimizes travel and is how real elevators feel. Make it a Strategy so you can swap.

**Q: External vs internal requests — do they differ?**
Internal (car buttons) are just target floors. External (hall calls) carry a desired direction, so the scheduler should only serve an up-call while heading up. Model direction on the request.

**Q (senior): Requests arrive concurrently — how do you keep the car loop correct?**
Separate the producer (riders submitting requests from many threads) from the consumer (the single car-processing loop). Use a thread-safe pending structure; the car thread drains it. One car = one consumer avoids most races.

**Follow-ups**
- *"Multiple elevators — how to dispatch?"* — A dispatcher scores each car for a hall call (distance, direction match, load) and assigns the best. That's another Strategy.
- *"Starvation of a far request under SCAN?"* — Bounded by the sweep; mention aging if strict fairness is required.
- *"Priority (firefighter/VIP) mode?"* — A state/override that preempts normal scheduling.

**Traps**
- Hard-coding FCFS and not mentioning SCAN/LOOK.
- Ignoring request direction for hall calls.
- A single shared mutable pending set mutated by many threads without synchronization.
- Over-scoping to N cars immediately instead of nailing one car first.
