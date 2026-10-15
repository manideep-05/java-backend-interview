# Requirement Clarification Drills

> Prerequisites: [00-foundations/requirements-and-constraints](../00-foundations/requirements-and-constraints.md)
> Related: [estimation-drills](./estimation-drills.md)

Clarification is the first 5 minutes and the highest-leverage skill. These drills train you to ask the few questions that *change the architecture* and to spot the dominant non-functional requirement fast.

---

## The routine (recap)

For any prompt: list 3–5 FRs → ask the 6 NFR questions (scale, latency, availability, consistency, durability, read:write) → state assumptions → declare scope/non-goals → confirm. See [requirements-and-constraints](../00-foundations/requirements-and-constraints.md).

**The skill:** identify the *dominant* NFR — the one that drives the whole design — and the handful of clarifications that change the architecture.

---

## Drills — for each prompt, (a) list the clarifications that change the design, (b) name the dominant NFR. Solutions below.

1. "Design a ticketing system."
2. "Design a URL shortener."
3. "Design a chat app."
4. "Design a payment service."
5. "Design a news feed."
6. "Design a rate limiter."
7. "Design a notification service."
8. "Design a file storage service."
9. "Design a ride-hailing app."
10. "Design a metrics platform."

---

## Solutions (dominant NFR in bold)

1. **Ticketing:** Clarify: can two people book the same seat (concurrency)? flash-sale scale? → **Dominant: strong consistency / no double-booking under concurrency.** (See [ticket-booking-system](../12-system-design-problems/ticket-booking-system.md).)
2. **URL shortener:** Clarify: read:write ratio? custom aliases? scale? → **Dominant: read-heavy (cache-first redirects).** (See [url-shortener](../12-system-design-problems/url-shortener.md).)
3. **Chat:** Clarify: 1:1 vs groups? real-time latency? ordering? offline delivery? scale/connections? → **Dominant: real-time delivery + per-conversation ordering at huge write/connection scale.** (See [chat-messaging-system](../12-system-design-problems/chat-messaging-system.md).)
4. **Payment:** Clarify: double-charge tolerance (none)? consistency? → **Dominant: correctness — never double-charge/lose a record (strong consistency + idempotency).** (See [payment-system](../12-system-design-problems/payment-system.md).)
5. **News feed:** Clarify: celebrities (huge follower counts)? read:write? ranking? consistency? → **Dominant: read-heavy + celebrity fan-out (hybrid push/pull).** (See [news-feed](../12-system-design-problems/news-feed.md).)
6. **Rate limiter:** Clarify: distributed across how many instances? strict vs soft? fail-open/closed? → **Dominant: distributed shared state + atomicity (+ fail-open/closed).** (See [rate-limiter](../12-system-design-problems/rate-limiter.md).)
7. **Notification service:** Clarify: channels? delivery guarantee (don't lose)? spikiness? priority? → **Dominant: reliable async delivery (at-least-once + idempotency) under spikes.** (See [notification-service](../12-system-design-problems/notification-service.md).)
8. **File storage:** Clarify: file sizes (GB)? durability? read:write? → **Dominant: durable large-blob storage (object storage) + large/resumable uploads.** (See [file-storage-service](../12-system-design-problems/file-storage-service.md).)
9. **Ride-hailing:** Clarify: real-time matching latency? double-assignment? location update volume? → **Dominant: real-time geospatial matching + no double-assignment.** (See [ride-hailing-system](../12-system-design-problems/ride-hailing-system.md).)
10. **Metrics platform:** Clarify: ingest volume? retention? cardinality? loss tolerance? → **Dominant: high-write time-series ingest + retention/cardinality.** (See [metrics-monitoring-platform](../12-system-design-problems/metrics-monitoring-platform.md).)

---

## The meta-lesson

Each system has **one dominant NFR** that shapes everything. Train yourself to find it in the first 5 minutes — it tells you where to spend your design time. Asking "can reads be stale?" and "what happens under concurrency?" surfaces it for most systems.

## Exercise

For three *unseen* prompts (e.g., "design a leaderboard," "design a feature-flag service," "design an email-digest system"), write the clarifications that change the design and name the dominant NFR, in under 4 minutes each. (Hint: leaderboard → sorted-set reads + update frequency; feature flags → low-latency reads + instant propagation/consistency; digest → scheduling + fan-out + idempotent sends. These appear in the [INTERVIEW-CHECKLIST](../INTERVIEW-CHECKLIST.md) final assessment.)

**Next:** [estimation-drills](./estimation-drills.md)
