# Ride Sharing — LLD

Think Uber/Lyft core. The signal is a clean **matching Strategy**, a **ride lifecycle State**, and the concurrency rule that one driver serves one rider at a time.

---

## 1. Requirements

**Functional**
- Riders request a ride (pickup + drop). Match to a nearby available driver.
- Ride lifecycle: REQUESTED → ASSIGNED → ONGOING → COMPLETED (or CANCELLED).
- Fare estimate / calculation.

**Non-functional**
- Many riders + drivers → matching must not assign one driver to two rides.
- Pluggable matching (nearest / highest-rated / surge-aware) and pricing.

---

## 2. Why these patterns

- **Strategy** for matching (nearest driver, etc.) and pricing (base + surge).
- **State** for the ride lifecycle.
- A **Facade** (`RideService`) orchestrates request → match → progress.

---

## 3. Critical flow

rider requests → matcher picks an available driver → atomically mark that driver BUSY and create a ride ASSIGNED → driver starts (ONGOING) → completes (COMPLETED), driver becomes AVAILABLE again.

The "pick available driver and mark busy" is the critical section.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Concurrency | Two riders matched to the same driver | Atomic claim of the driver (CAS AVAILABLE→BUSY). |
| None | No available drivers | Return "no drivers"; optionally queue/retry. |
| Cancel | Rider cancels after assignment | Free the driver (AVAILABLE), ride CANCELLED. |
| Invalid state | Complete a ride that never started | State rejects. |
| Fairness | Driver starvation / fairness | Matching policy concern (round-robin / rating). |

---

## 5. Entities, relationships, responsibilities

- **Driver** (id, location, status AVAILABLE/BUSY/OFFLINE).
- **Rider** (id, location).
- **Ride** (rider, driver, pickup, drop, state).
- **MatchingStrategy** (interface): pick a driver from the available pool. → Strategy.
- **PricingStrategy**: fare. → Strategy.
- **RideService** (Facade): request → match → lifecycle transitions.

---

## 6. Class/interface design

```
RideService
   ├── MatchingStrategy (NearestDriver)   STRATEGY
   ├── PricingStrategy                    STRATEGY
   └── Ride --> RideState                 STATE
Driver { status }  (atomic claim)
```

### normal vs production
- `normal/` — matcher scans available drivers, picks nearest, marks busy; single-threaded.
- `production/` — driver status is an atomic enum; claim is CAS (AVAILABLE→BUSY) so two riders can't grab the same driver; the available pool is a concurrent set.

---

## Interview Questions, Follow-ups & Traps

**Q: Two riders request at once and the matcher picks the same nearest driver — what happens?** *(core)*
The claim must be atomic: CAS the driver's status AVAILABLE→BUSY. Only one rider's CAS wins; the other re-matches to a different driver. Never mark-busy after a separate "is available" read without atomicity.

**Q: How would you find the nearest driver efficiently at scale?**
A geospatial index: a quadtree/geohash grid so you query only nearby cells instead of scanning all drivers. The `MatchingStrategy` abstracts this; the naive version scans, the scaled version uses the index.

**Q: Ride lifecycle — why State?**
Legal operations depend on the ride's phase (can't COMPLETE a ride that isn't ONGOING). State classes/enum-with-guards encode the valid transitions.

**Follow-ups**
- *"Surge pricing?"* — A PricingStrategy that multiplies by a demand/supply factor per area/time.
- *"Driver rejects the ride?"* — Re-enter matching, exclude that driver, free them back to AVAILABLE.
- *"Distributed matching across regions?"* — Shard by geography; a dispatcher per region; the claim becomes a DB/Redis atomic update.

**Traps**
- Non-atomic driver claim (one driver, two rides).
- Scanning all drivers for "nearest" at scale (need a geo index).
- Not freeing the driver on cancel/complete (driver stuck BUSY).
- Illegal ride state transitions.
