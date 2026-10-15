# Movie Ticket Booking — LLD

The concurrency story is the whole point: two users must never book the same seat. Expect the interviewer to drive straight at seat locking and hold expiry.

---

## 1. Requirements

**Functional**
- Browse shows (movie + screen + time). Each show has seats.
- Select seats → **hold** them → pay → **confirm** booking.
- A hold expires if payment isn't completed in N minutes (seats released).

**Non-functional**
- Many users hitting the same popular show → seat selection must be race-free.
- Holds must auto-expire so abandoned carts don't block seats forever.

---

## 2. Critical flows

1. **Book**: pick seats → atomically move each from AVAILABLE → HELD (by this user, with expiry) → pay → HELD → BOOKED.
2. **Expire**: a background sweeper (or lazy check) moves expired HELD seats back to AVAILABLE.
3. **Cancel**: release held/booked seats.

The AVAILABLE→HELD transition is the critical section.

---

## 3. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Concurrency | Two users select the same seat | Atomic state flip; only one wins. |
| Time | User holds then disappears | Hold expiry releases the seat. |
| Partial | Payment fails after hold | Release the held seats. |
| Double | Pay twice for the same hold | Idempotent confirm. |
| Boundary | Some requested seats free, some taken | All-or-nothing: either grab all or roll back the ones you grabbed. |

---

## 4. Entities, relationships, responsibilities

- **Movie**, **Screen**, **Show** (movie + screen + time + seats).
- **Seat** / **ShowSeat**: a seat *within a show* with a status (AVAILABLE/HELD/BOOKED) + holder + hold-expiry.
- **Booking**: user + seats + status (HELD/CONFIRMED/CANCELLED).
- **BookingService** (Facade): orchestrates hold → pay → confirm.
- **PaymentStrategy**: pluggable payment. → Strategy.

*Key:* seat status lives on **ShowSeat**, not Seat, because the same physical seat has different status per show.

---

## 5. Class/interface design

```
Show ──> List<ShowSeat> (status, heldBy, holdExpiry)
BookingService
   ├── hold(showId, seatIds, user)  -> all-or-nothing atomic grab
   ├── confirm(bookingId, payment)  -> HELD -> BOOKED, idempotent
   └── (sweeper) releaseExpiredHolds()
```

### normal vs production
- `normal/` — seat status is a plain enum field; `hold` checks then sets (fine single-threaded); shows the all-or-nothing rollback.
- `production/` — each seat's status transition is **atomic (CAS on an AtomicReference)**; holds store an expiry timestamp; a sweeper (or lazy check on access) reclaims expired holds; confirm is idempotent.

---

## Interview Questions, Follow-ups & Traps

**Q: Two users click the same seat at the same instant — who wins?** *(the core question)*
Make AVAILABLE→HELD atomic: CAS the seat's status (or lock the seat/show row). Exactly one CAS succeeds; the other user sees the seat as taken. Never rely on a read-then-write gap.

**Q: How do holds expire?**
Store `holdExpiry` when holding. Either a background sweeper periodically releases expired holds, or you lazily check expiry whenever a seat is read/attempted — expired HELD is treated as AVAILABLE and can be re-CAS'd.

**Q: User selected 3 seats but only 2 are free — now what?**
All-or-nothing: try to grab all; if any grab fails, release the ones you already grabbed and fail the whole request. Partial bookings are a bad UX and a data mess.

**Follow-ups**
- *"Distributed (many servers)?"* — Seat rows in a DB; the atomic grab becomes a conditional UPDATE (`SET status='HELD' WHERE id=? AND status='AVAILABLE'`) or a distributed lock / Redis `SETNX` with TTL for the hold.
- *"Idempotent payment confirm?"* — Key the confirm by bookingId/payment token so a retry doesn't double-book or double-charge.
- *"Prevent one user hoarding seats?"* — Limit held seats per user; expire aggressively.

**Traps**
- Check-then-set on seat status (the classic double-booking race).
- Holding seats forever when payment is abandoned (no expiry).
- Putting status on the physical Seat instead of per-show ShowSeat.
- Allowing partial multi-seat bookings.
