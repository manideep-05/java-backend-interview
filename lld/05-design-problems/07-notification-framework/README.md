# Notification Framework — LLD

A "design a reusable library" problem. Interviewers want clean extension points (new channels, new events) and, at senior level, async delivery with retries.

---

## 1. Requirements

**Functional**
- Send a notification over one or more **channels** (email, SMS, push).
- Users have **preferences** (which channels, opt-out).
- Multiple subscribers can react to one event (e.g. order placed → email + SMS).
- Templating of message content.

**Non-functional**
- Delivery is I/O (network) → should be **async** so the caller isn't blocked.
- **Retries** with backoff for transient failures.
- Pluggable channels (OCP).

---

## 2. Why these patterns

- **Strategy** for the channel: each channel is a `NotificationChannel` with one `send()`.
- **Observer / pub-sub** for fan-out: an event is published; many subscribers (channels/handlers) react.
- **Factory** to create the right channel/notification from a type.
- (Decorator) to layer retry/logging around a channel without changing it.

---

## 3. Critical flow

`notify(user, message)` → resolve the user's enabled channels (preferences) → for each, dispatch via its Strategy → (production) submit to a thread pool, with retry on failure.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Preference | User opted out of SMS | Skip that channel. |
| Failure | Channel send throws (network) | Retry with backoff; after N tries, dead-letter. |
| Partial | Email ok, SMS fails | Independent per-channel; one failure doesn't block others. |
| Ordering | Does order matter? | Usually no; fan-out is parallel. |
| Overload | Burst of notifications | Bounded queue + thread pool; shed/throttle if full. |

---

## 5. Entities, relationships, responsibilities

- **NotificationChannel** (interface): `send(user, message)`. → **Strategy**.
- **EmailChannel / SmsChannel / PushChannel**: concrete channels.
- **ChannelFactory**: type → channel. → **Factory**.
- **NotificationService** (subject/Facade): holds channels, respects preferences, dispatches.
- **UserPreferences**: enabled channels per user.
- **(Decorator) RetryingChannel**: wraps a channel to add retry.

---

## 6. Class/interface design

```
NotificationService
   ├── UserPreferences (which channels per user)
   ├── List<NotificationChannel>   (Observer-style fan-out)
   └── ChannelFactory              (Factory)
NotificationChannel (Strategy): Email / SMS / Push
RetryingChannel (Decorator) wraps any NotificationChannel
```

### normal vs production
- `normal/` — synchronous: iterate the user's channels and `send()` in-line. Factory creates channels; preferences filter.
- `production/` — delivery submitted to a bounded `ThreadPoolExecutor` (async, non-blocking caller); a `RetryingChannel` decorator retries transient failures with backoff; per-channel failures are isolated.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you add a new channel without touching existing code?**
Implement `NotificationChannel` and register it with the factory/service. The dispatch loop is written against the interface (Strategy + OCP), so nothing else changes.

**Q: Why async delivery?**
Sending is network I/O; doing it synchronously blocks the caller (e.g. the checkout request) on third-party latency. Submit to a thread pool so the business flow returns immediately; delivery happens in the background.

**Q: How do retries work and where do they live?**
Wrap a channel in a `RetryingChannel` decorator: it calls the inner channel, and on a transient failure retries with exponential backoff up to N times, then dead-letters. Keeping retry in a decorator means every channel gets it without duplicating logic.

**Follow-ups**
- *"At-least-once vs exactly-once delivery?"* — Networks force at-least-once; make the consumer idempotent (dedupe by notification id).
- *"Backpressure on a burst?"* — Bounded queue + a rejection policy (caller-runs, drop, or throttle). Unbounded queues cause OOM.
- *"Priority (OTP before marketing)?"* — Separate pools/queues per priority, or a priority queue.

**Traps**
- Synchronous fan-out that blocks the caller on slow I/O.
- One channel's failure aborting the others (not isolating failures).
- Unbounded executor queue (memory blowup under load).
- Baking retry into each channel instead of a decorator.
