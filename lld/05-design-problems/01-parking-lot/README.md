# Parking Lot — LLD

The "hello world" of LLD interviews. Simple enough to finish, rich enough to probe design skill.

---

## 1. Requirements

**Functional**
- A lot has multiple **levels/floors**; each level has multiple **spots**.
- Spots have **types** (motorcycle, compact, large) and only fit matching vehicle sizes.
- A vehicle **parks** → gets a **ticket** (entry time, spot). It **unparks** → **pays** based on duration.
- Query **availability** per spot type.

**Non-functional (clarify these out loud)**
- Concurrency: many entry/exit gates operate at once → spot assignment must be race-free.
- Scale: single lot, single process (say so to bound scope). Mention sharding/DB if asked.
- Pricing may change (hourly, flat, type-based) → make it pluggable.

**Explicit assumptions:** one vehicle per spot; a motorcycle fits any spot, a car needs compact/large, a bus needs large (and in some variants, multiple contiguous spots — call this out).

---

## 2. Critical flows

1. **Park**: pick gate → find a free spot that fits the vehicle → mark occupied → issue ticket. *(The find-and-mark must be atomic.)*
2. **Unpark**: present ticket → compute fee (now − entry) via pricing strategy → free the spot → return fee.
3. **Availability**: count free spots by type.

---

## 3. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Full | No matching spot free | Reject park, return "lot full for this type". |
| Concurrency | Two cars race for the **last** spot | Atomic claim (lock/CAS). Only one wins; the other retries or is rejected. |
| Invalid state | Unpark with an unknown/used ticket | Reject; ticket must be active. |
| Double exit | Same ticket presented twice | Idempotency: mark ticket PAID/CLOSED once. |
| Boundary | Vehicle larger than any spot | Reject at entry. |
| Time | Duration rounds up to the next hour | Define rounding policy explicitly. |

**How to find edges:** walk each flow and ask "what if it's empty / full / concurrent / repeated / invalid / times out?"

---

## 4. Entities, relationships, responsibilities

- **ParkingLot** (orchestrator / Facade): holds levels, exposes `park()` / `unpark()`. *Responsibility:* coordinate, not compute pricing.
- **Level**: holds spots; finds a free spot. *has-a* many Spot.
- **ParkingSpot**: knows its type and occupancy; assign/free a vehicle.
- **Vehicle** (abstract) → Motorcycle / Car / Bus. *is-a* hierarchy keyed by size.
- **Ticket**: entry time, spot ref, status. A value/record.
- **ParkingStrategy** (interface): *which* free spot to pick (first-fit, nearest). → **Strategy**.
- **PricingStrategy** (interface): how to compute the fee. → **Strategy**.

Relationships: `ParkingLot 1—* Level 1—* ParkingSpot`; `Ticket → ParkingSpot`; `ParkingSpot → Vehicle`.

---

## 5. Class/interface design

```
ParkingLot ----> Level ----> ParkingSpot ----> Vehicle
    |  uses ParkingStrategy (pick a spot)
    |  uses PricingStrategy  (compute fee)
    +--> issues Ticket
```

- **Strategy** for allocation and pricing → open/closed: new policies don't touch the lot (OCP, DIP).
- **Factory** for creating `Vehicle` from a type → centralizes creation.
- The lot is a reasonable **Singleton** in a single-lot app (but we inject strategies, keeping it testable).

### normal vs production
- `normal/` — single-threaded clarity. `find free spot → mark occupied` is two steps (a race in the real world, fine for the whiteboard first pass).
- `production/` — the claim is made **atomic** with a per-level lock so two threads can't take the same last spot; availability uses atomic counters; tickets are closed idempotently.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you prevent two cars from taking the same last spot?** *(the #1 follow-up)*
The find-free-then-mark-occupied sequence is a classic **check-then-act** race. Make the claim atomic: lock the level (or the spot) during claim, or use an atomic/CAS flag on the spot. In `production/` we lock per-level so concurrency stays high across levels.

**Q: Where does pricing belong?**
In a `PricingStrategy`, not in `Ticket` or `ParkingLot`. Pricing changes independently (hourly vs flat vs weekend) — that's its own reason to change (SRP) and a swap point (Strategy/OCP).

**Q: A bus needs 3 contiguous spots — how does your design change?**
Allocation moves from "find one spot" to "find N contiguous spots on a level"; the `ParkingStrategy` abstraction absorbs this without changing the lot.

**Follow-ups**
- *"Multiple entry gates?"* — Each gate calls the same thread-safe `park()`. Contention handled by the per-level lock / concurrent structures.
- *"Make it distributed / multi-lot."* — Spots become rows in a DB; the atomic claim becomes a conditional UPDATE (`WHERE status='FREE'`) or a distributed lock. Say this; don't code it.
- *"How do you compute availability fast?"* — Maintain per-type atomic counters updated on claim/free, instead of scanning spots.

**Traps**
- Putting pricing/allocation logic inside `ParkingLot` (God object, violates SRP/OCP).
- Ignoring the last-spot race (interviewers *will* ask).
- Modeling vehicle size with an `if/else` on strings instead of a type hierarchy or enum.
- Forgetting idempotency on exit (double-charge on a re-presented ticket).
