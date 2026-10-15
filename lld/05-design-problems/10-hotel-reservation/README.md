# Hotel / Restaurant Reservation — LLD

The concurrency crux is identical to seat booking: never **double-book** the same room/table for an overlapping period. Dates add an interval-overlap twist.

---

## 1. Requirements

**Functional**
- Rooms (by type) / tables (by capacity). Search availability for a date range.
- Reserve a room for `[checkIn, checkOut)`; cancel.
- Pricing per room type / night.

**Non-functional**
- Concurrent reservations must not double-book an overlapping range.
- Scale: single property; mention DB + distributed locks for chains.

---

## 2. Critical flow

search availability for a range → user picks a room → atomically reserve IF no existing reservation overlaps that range → confirm. The "check no overlap, then insert" is the critical section.

---

## 3. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Overlap | New range overlaps an existing booking | Reject; intervals `[a,b)` overlap iff `a < d && c < b`. |
| Boundary | checkout == next checkin | Half-open intervals: back-to-back is allowed. |
| Concurrency | Two users reserve the same room/overlap | Lock per room during the overlap-check-and-insert. |
| Invalid | checkIn >= checkOut | Reject. |
| Cancel | Cancel frees the range | Remove the reservation; range reusable. |

---

## 4. Entities, relationships, responsibilities

- **Room** (id, type, pricePerNight).
- **DateRange**: `[checkIn, checkOut)` with an `overlaps()` method. *Responsibility:* interval math.
- **Reservation**: room + range + guest + status.
- **ReservationService** (Facade): search + reserve + cancel; owns the per-room reservation lists.
- **PricingStrategy**: nightly pricing. → Strategy (optional).

---

## 5. Class/interface design

```
ReservationService
   └── Map<roomId, List<Reservation>>   (bookings per room)
Room { id, type, pricePerNight }
DateRange { checkIn, checkOut; overlaps(other) }
```

### normal vs production
- `normal/` — reserve checks all existing reservations of that room for overlap, then adds; single-threaded.
- `production/` — the overlap-check-and-insert is guarded by a **per-room lock** (striped locking) so two threads can't both pass the "no overlap" check for the same room; different rooms never contend.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you detect overlapping date ranges?**
Half-open intervals `[a, b)` and `[c, d)` overlap iff `a < d && c < b`. Using half-open means a checkout on the same day as the next checkin does NOT overlap (back-to-back bookings are fine) — state this explicitly.

**Q: How do you prevent double-booking under concurrency?** *(the core question)*
"Check no overlap, then insert" is check-then-act. Lock per room for that compound step. Per-room (striped) locking keeps different rooms fully concurrent while serializing conflicting operations on the same room.

**Q: Make it efficient for many reservations per room.**
A sorted structure keyed by start date (TreeMap) lets you find potential overlaps in O(log n) instead of scanning all. In a DB, an exclusion constraint or a `WHERE NOT EXISTS (overlapping)` conditional insert does it atomically.

**Follow-ups**
- *"Hotel chain / many servers?"* — Reservations in a DB; use a transaction with an exclusion constraint or `SELECT ... FOR UPDATE` on the room row; or a distributed lock keyed by room.
- *"Overbooking strategy (airlines/hotels do it)?"* — Allow N% over capacity deliberately; a policy knob.
- *"Search by room type, not a specific room?"* — Find ANY room of the type that's free for the range; grab the first atomically.

**Traps**
- Closed intervals making back-to-back bookings falsely conflict.
- Check-then-insert without a lock (double-booking race).
- A single global lock serializing all rooms (use per-room striping).
- Scanning every reservation for overlap at scale (use a sorted index).
