# Mock Interviews

> Prerequisites: [how-to-drive-an-interview](./how-to-drive-an-interview.md), [00-foundations/interview-communication-framework](../00-foundations/interview-communication-framework.md)
> Related: [INTERVIEW-CHECKLIST](../INTERVIEW-CHECKLIST.md) (the scoring rubric)

Ten complete mock interviews. **Practice mode:** read only the PROMPT + INTERVIEWER PROMPTS section first, run the full 45-minute design yourself (recorded), *then* read the solution/rubric. Don't peek — the value is in doing it cold.

How to use: for each mock, (1) run it under a 45-min timer using the [11-step flow](../00-foundations/interview-communication-framework.md), (2) have the interviewer prompts thrown at you (or self-prompt), (3) score with the [rubric](../INTERVIEW-CHECKLIST.md), (4) log weak dimensions in [PROGRESS](../PROGRESS.md).

---

# PART 1 — PROMPTS (do these first, no solutions)

### Mock 1 — URL shortener (foundational)
**Prompt:** "Design a URL shortener." **Interviewer prompts:** "10M new URLs/day, very read-heavy." Follow-ups: "Redis is down?" "10× the reads?" "How do you generate codes without collisions?" "301 or 302?"

### Mock 2 — Rate limiter (foundational)
**Prompt:** "Design a distributed rate limiter." **Prompts:** "Per API key, ~50 app instances, strict-ish limit." Follow-ups: "Why not per-instance counters?" "Avoid the check-and-decrement race?" "Redis down — allow or block?" "One tenant sends most traffic."

### Mock 3 — Notification service (intermediate)
**Prompt:** "Design a notification service (email/SMS/push)." **Prompts:** "250M/day, spiky, don't lose transactional ones." Follow-ups: "A worker crashes after sending — duplicate?" "A campaign floods the system." "A provider is down." "Per-user ordering?"

### Mock 4 — Chat system (intermediate→advanced)
**Prompt:** "Design a chat/messaging system." **Prompts:** "1:1 and groups, real-time, 50M DAU, don't lose messages, in-order per conversation." Follow-ups: "Offline users?" "A gateway node crashes?" "A 100K-member channel?" "Exactly-once?"

### Mock 5 — Payment system (advanced)
**Prompt:** "Design a payment system." **Prompts:** "Never double-charge, never lose a record." Follow-ups: "Walk me through why naive check-then-charge is unsafe." "Provider charged but your write failed?" "Strong or eventual consistency?" "Checkout spans order+inventory+payment."

### Mock 6 — Ticket booking (advanced)
**Prompt:** "Design a ticket booking system." **Prompts:** "No double-booking a seat; popular events cause massive concurrency." Follow-ups: "How do you prevent double-booking?" "Payment fails after a hold?" "A concert sells out in seconds — survive the stampede."

### Mock 7 — News feed (advanced)
**Prompt:** "Design a news feed." **Prompts:** "500M users, celebrities exist, read-heavy." Follow-ups: "Fan-out on write or read?" "A celebrity with 50M followers posts." "Feed store is down?" "When does a post appear in followers' feeds?"

### Mock 8 — File storage (intermediate)
**Prompt:** "Design a cloud file storage service." **Prompts:** "Files KB to multi-GB, durability paramount, global, read-heavy." Follow-ups: "A 5GB upload keeps failing." "Avoid storing the same file twice?" "Guarantee durability?" "Serve downloads globally."

### Mock 9 — Job scheduler (intermediate)
**Prompt:** "Design a job scheduler (cron + delayed jobs)." **Prompts:** "Millions of jobs, exactly-once execution, don't lose jobs." Follow-ups: "Prevent a job running twice?" "Everyone schedules at midnight." "Dispatcher is a SPOF?" "A worker crashes mid-job."

### Mock 10 — Document management system (intermediate)
**Prompt:** "Design a document management system." **Prompts:** "100K users, 50M docs, upload→process (OCR/scan)→notify, search, multi-tenant, files never lost, read-your-write after upload." Follow-ups: "Files in the DB?" "OCR is slow and blocking uploads." "Keep search in sync?" "A huge tenant with 10M docs."

---

# PART 2 — SOLUTIONS, EXPECTED REASONING & RUBRICS

> Read only *after* attempting. Each solution references the full design doc (don't duplicate — the full A–P treatments are in [12-system-design-problems](../12-system-design-problems/)).

### Mock 1 solution — URL shortener
**Full solution:** [url-shortener](../12-system-design-problems/url-shortener.md). **Expected:** clarify read:write → estimate (~120 writes/sec, ~60K read RPS peak) → cache-first redirect (immutable mapping → trivial invalidation) → base62-of-Snowflake code gen (no collision checks) → async analytics → Redis-down = DB fallback → 302 for click counting. **Rubric focus:** estimation drives the cache-first decision; avoids random-code-collision-check; doesn't shard (writes tiny).
**Red flags:** treats it write-heavy; sharding at 10M/day; sync click counting.

### Mock 2 solution — Rate limiter
**Full:** [rate-limiter](../12-system-design-problems/rate-limiter.md). **Expected:** per-instance counters fail (N× limit) → shared Redis state → token bucket → atomic Lua check-decrement → 429+Retry-After → fail-open with local cap on Redis down → hot key = shard/local pre-check. **Rubric focus:** recognizes the distributed-state requirement + atomicity; makes an explicit fail-open/closed decision.
**Red flags:** per-instance counters; non-atomic check; no Redis-down plan.

### Mock 3 solution — Notification service
**Full:** [notification-service](../12-system-design-problems/notification-service.md). **Expected:** async (202+queue) → stream keyed by userId (ordering) + per-channel work queues → at-least-once + idempotent sends (dedup on notification_id) → retries/backoff/jitter + DLQ → autoscale on queue depth + prioritize transactional + provider circuit breaker. **Rubric focus:** idempotency for the worker-crash duplicate; backpressure/priority for the campaign spike.
**Red flags:** sync sends; no idempotency; one queue; no DLQ.

### Mock 4 solution — Chat system
**Full:** [chat-messaging-system](../12-system-design-problems/chat-messaging-system.md). **Expected:** ~70K peak write RPS + 438 TB/yr → wide-column partitioned by conversation (ordering) → stateful WebSocket gateway + pub/sub routing via presence → offline = persist + fetch-on-reconnect pointer → at-least-once + clientMsgId dedupe → huge channel = pull-based. **Rubric focus:** recognizes the stateful connection layer + wide-column store + per-conversation ordering + offline delivery.
**Red flags:** stateless HTTP request/response model; relational DB for messages; no offline mechanism.

### Mock 5 solution — Payment system
**Full:** [payment-system](../12-system-design-problems/payment-system.md). **Expected:** correctness-first → mandatory idempotency key → atomic unique-key insert before charging (closes the race) → pass key to provider (dedupes external charge) → record result+ledger in one txn → replay returns original → webhook + reconciliation for residual drift → "effectively-once, not exactly-once." Checkout = saga. **Rubric focus:** correctly explains why check-then-charge is unsafe (3 ways); honest about effectively-once + reconciliation.
**Red flags:** check-then-charge; claims hard exactly-once; eventual consistency for the ledger; 2PC across the external provider.

### Mock 6 solution — Ticket booking
**Full:** [ticket-booking-system](../12-system-design-problems/ticket-booking-system.md). **Expected:** strong consistency on seat state → atomic conditional transition (AVAILABLE→HELD→BOOKED, one winner) → hold TTL (release abandoned) → virtual waiting room for the stampede → payment idempotent. **Rubric focus:** no double-booking via atomic transition; waiting room to protect the DB under flash-sale.
**Red flags:** read-check-then-write (double-book); eventual consistency on seats; no hold TTL; no waiting room.

### Mock 7 solution — News feed
**Full:** [news-feed](../12-system-design-problems/news-feed.md). **Expected:** feed reads dominate → fan-out-on-write (precomputed feeds, fast reads) → **hybrid: pull for celebrities** (avoid millions of feed writes) → async fan-out (fast posting) → cap feed length + cache + Snowflake-ordered → feed store down = fan-out-on-read fallback. **Rubric focus:** the hybrid fan-out decision + celebrity hot-key handling.
**Red flags:** pure fan-out-on-write without the celebrity case (melts down); pure pull at scale; missing the celebrity problem.

### Mock 8 solution — File storage
**Full:** [file-storage-service](../12-system-design-problems/file-storage-service.md). **Expected:** object storage (petabytes) + metadata DB (sharded by owner) → multipart pre-signed resumable uploads (GB files, re-upload missing parts) → content-hash dedup (ref-counted) → CDN + signed URLs for downloads → versioning + cross-region replication for durability → reconciliation for orphans. **Rubric focus:** multipart/resumable for large files; object-storage/metadata separation; durability (versioning + replication).
**Red flags:** blobs in the DB; streaming large files through the app; no resumability; no versioning.

### Mock 9 solution — Job scheduler
**Full:** [job-scheduler](../12-system-design-problems/job-scheduler.md). **Expected:** dispatcher scans `next_run_at`, atomically claims due jobs (SKIP LOCKED / leader-elected) → enqueue → idempotent workers (unique on job_id+scheduled_for) → at-least-once + idempotency = effectively-once → thundering herd = jitter + autoscale + partition scan → overdue jobs picked up on recovery. **Rubric focus:** prevents double-run via atomic claim + idempotency; handles the midnight thundering herd and dispatcher SPOF.
**Red flags:** multiple dispatchers double-enqueuing; non-idempotent execution; losing overdue jobs; claiming hard exactly-once.

### Mock 10 solution — Document management system
**Full:** [document-management-system](../12-system-design-problems/document-management-system.md). **Expected:** storage math → files in object storage + metadata in one SQL node (no sharding) → upload returns 201 + pre-signed URL + PROCESSING → async idempotent OCR workers → outbox/CDC fans out to search/cache/notify (no dual-write) → read-your-write from primary → Elasticsearch search → tenant_id + RLS → huge tenant = hot-shard handling. **Rubric focus:** files-vs-metadata split from estimation; async processing; outbox/CDC; read-your-write; multi-tenant isolation; resists over-engineering (modest RPS).
**Red flags:** files in the DB; sync OCR; dual-write to search; LIKE search; missing tenant isolation; over-engineering (sharding/Kafka/multi-region for modest scale).

---

# PART 3 — MOCKS THAT TEST REVISION (requirements change mid-interview)

The master prompt wants mocks where a reasonable initial decision must be revised when the interviewer changes the requirement. Run these as **two-phase** mocks:

### Revision Mock A — URL shortener → "now it's write-heavy and global"
Phase 1: design for read-heavy, single-region (cache-first). Phase 2 (interviewer changes it): "Actually writes are now enormous and users are global." **Expected revision:** move the mapping to a KV/wide-column store (writes exceed one primary); Snowflake IDs already shard-ready; add CDN + multi-region read replicas for global redirects. **Signal:** revises gracefully, re-estimates the pressured dimension, doesn't cling to the original.

### Revision Mock B — Notification service → "now add a daily digest at the user's local time"
Phase 1: real-time notifications. Phase 2: "Add a daily digest email at each user's local 8 AM for 50M users." **Expected revision:** add a scheduler + time-zone sharding + per-user aggregation + idempotent sends; reuse the existing async pipeline. **Signal:** extends the design without rebuilding; handles time-zone fan-out + idempotent scheduled sends.

### Revision Mock C — DMS → "a huge enterprise tenant appears" / "EU data-residency law"
Phase 1: single-region modest-scale DMS. Phase 2a: "A tenant with 10M docs." **Revision:** shard by tenant_id, isolate the big tenant (hot shard). Phase 2b: "EU residency law." **Revision:** multi-region partitioned by tenant home region + per-tenant keys. **Signal:** recognizes when each complexity becomes justified (not before).

---

# PART 4 — SELF-SCORING

After each mock, score with the [INTERVIEW-CHECKLIST rubric](../INTERVIEW-CHECKLIST.md) (11 dimensions, 1–5). Target: avg ≥ 3.5, none below 3. Log weak dimensions in [PROGRESS](../PROGRESS.md) Part 7–8. Patterns across mocks tell you where to spend remaining prep (usually failure handling and trade-off communication for most candidates).

**Section complete.** Next section: [14-revision](../14-revision/one-page-system-design-framework.md)
