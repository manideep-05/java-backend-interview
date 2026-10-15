# Progress Tracker

Tick a box only when the statement is **genuinely true** — especially the "explained aloud without notes" boxes. A file existing is not progress.

Legend: `[ ]` not started · `[~]` in progress · `[x]` done.

---

## Part 1 — Concepts studied (read + understood)

### Foundations
- [ ] What system design means in an interview; HLD vs LLD
- [ ] Functional vs non-functional requirements, scope, assumptions
- [ ] Latency, throughput, availability, durability, consistency
- [ ] p50 / p95 / p99 / tail latency
- [ ] MAU vs DAU vs RPS vs peak concurrency
- [ ] Back-of-the-envelope estimation
- [ ] Interview communication framework

### Networking & APIs
- [ ] DNS, TCP, TLS, HTTP request lifecycle
- [ ] HTTP/1.1 vs HTTP/2 vs HTTP/3
- [ ] REST and API design
- [ ] Pagination, filtering, sorting
- [ ] Authentication & authorization
- [ ] API gateway & load balancing (L4 vs L7)
- [ ] Rate limiting algorithms
- [ ] Webhooks & callbacks

### Compute & architecture
- [ ] Monolith vs modular monolith vs microservices
- [ ] Horizontal vs vertical scaling
- [ ] Stateless vs stateful services
- [ ] Reverse proxy & load balancers
- [ ] Service discovery
- [ ] Sync vs async processing
- [ ] Serverless & managed compute
- [ ] Autoscaling & capacity planning
- [ ] The 6-stage scaling journey (1 → millions of users)

### Databases & data modeling
- [ ] Relational vs NoSQL
- [ ] DB internals for system design (B-tree, LSM, WAL)
- [ ] Indexing & query optimization
- [ ] Transactions & isolation levels
- [ ] Normalization & denormalization
- [ ] Replication & read replicas / lag
- [ ] Partitioning & sharding; shard keys; hot partitions
- [ ] Distributed transactions (2PC, saga)
- [ ] Consistency models
- [ ] SQL vs document vs key-value vs wide-column
- [ ] Time-series & search databases
- [ ] Data archival & retention
- [ ] Schema migrations & online evolution

### Caching
- [ ] Caching fundamentals (layers)
- [ ] Cache-aside / read-through / write-through
- [ ] Write-behind / write-around
- [ ] Cache invalidation & consistency
- [ ] TTL, eviction, memory management
- [ ] Stampede, hot keys, penetration, avalanche
- [ ] Local vs distributed cache
- [ ] Redis design & failure modes

### Messaging & event-driven
- [ ] Queues vs pub/sub vs event streams
- [ ] Kafka internals & partitioning
- [ ] RabbitMQ internals & routing
- [ ] Delivery semantics (at-most / at-least / effectively-once)
- [ ] Ordering & consumer groups
- [ ] Retries, backoff, jitter
- [ ] Dead-letter queues & poison messages
- [ ] Idempotency & deduplication
- [ ] Transactional outbox & CDC
- [ ] Saga pattern
- [ ] Event sourcing & CQRS
- [ ] Backpressure & consumer lag

### Distributed systems
- [ ] Why distributed systems are hard
- [ ] Partial failures & timeouts
- [ ] Retries & duplicate execution
- [ ] Consistency & CAP theorem
- [ ] Quorum reads & writes
- [ ] Leader election & split-brain
- [ ] Distributed locks, leases, fencing tokens
- [ ] Clock skew & logical clocks
- [ ] ID generation & sequences
- [ ] Exactly-once processing & its limits
- [ ] Circuit breakers & bulkheads
- [ ] Graceful degradation
- [ ] Multi-region architecture

### Reliability, security, observability, storage
- [ ] Availability & reliability math
- [ ] Redundancy & failover
- [ ] Timeouts, retries, deadlines
- [ ] Circuit breaker pattern
- [ ] Graceful shutdown & draining
- [ ] Disaster recovery, RPO/RTO
- [ ] Backups & point-in-time recovery
- [ ] SLIs / SLOs / SLAs
- [ ] AuthN/AuthZ, RBAC, JWT, OAuth2/OIDC
- [ ] Secrets & key management
- [ ] Encryption in transit & at rest
- [ ] Multi-tenant isolation
- [ ] Logs, metrics, traces; distributed tracing
- [ ] Alerting, incident response, RCA
- [ ] Deployment strategies (rolling, blue-green, canary)
- [ ] Object storage & file uploads
- [ ] CDN & edge caching
- [ ] Search indexing & engines

---

## Part 2 — Explained aloud without notes (2–3 min each)
- [ ] Latency vs throughput; p99 and why it matters
- [ ] MAU → peak RPS estimation
- [ ] Why adding app servers can worsen the DB bottleneck
- [ ] ACID isolation levels and anomalies they prevent
- [ ] Replication lag: cause and user-visible effect
- [ ] Shard key selection and hot partitions
- [ ] Cache-aside read + write path; stampede + fixes
- [ ] What happens when Redis goes down
- [ ] At-least-once vs effectively-once; duplicate side effects
- [ ] Transactional outbox pattern
- [ ] CAP theorem and its limitations
- [ ] Unsafe check-then-charge vs idempotent payment
- [ ] Distributed locks and fencing tokens
- [ ] Circuit breaker states and when to open

---

## Part 3 — Estimation exercises completed
- [ ] Drills 1–5 (traffic / RPS)
- [ ] Drills 6–10 (storage / retention)
- [ ] Drills 11–15 (bandwidth / cache sizing)
- [ ] Drills 16–20 (connections / queue / latency budget)

---

## Part 4 — Architecture diagrams drawn independently
- [ ] DMS scaling stages 1–6
- [ ] URL shortener
- [ ] Rate limiter
- [ ] Notification service
- [ ] Chat / messaging
- [ ] News feed
- [ ] Payment system

---

## Part 5 — Design problems completed under 45-min timer
- [ ] URL shortener
- [ ] Rate limiter
- [ ] Notification service
- [ ] File storage service
- [ ] Document management system
- [ ] Chat / messaging system
- [ ] News feed
- [ ] Payment system
- [ ] (Track B) Ride-hailing
- [ ] (Track B) Ticket booking
- [ ] (Track B) Video streaming

---

## Part 6 — Follow-up questions answered confidently
- [ ] Scaling follow-ups (10×, first bottleneck, sharding)
- [ ] Database follow-ups (SQL vs NoSQL, replica lag, big migration, overselling)
- [ ] Caching follow-ups (Redis down, stampede, invalidation, hot key, stale data)
- [ ] Messaging follow-ups (consumer crash, duplicates, poison messages, lag, ordering)
- [ ] Distributed follow-ups (exactly-once, partial failure, duplicate payment, split lock)
- [ ] Reliability follow-ups (AZ failure, corruption recovery, dependency outage, SLO)

---

## Part 7 — Mock interviews completed (scored)
- [ ] Mock 1 — URL shortener
- [ ] Mock 2 — Rate limiter
- [ ] Mock 3 — Notification service
- [ ] Mock 4 — Chat system
- [ ] Mock 5 — Payment system
- [ ] Mock 6 — Ticket booking
- [ ] Mock 7 — News feed
- [ ] Mock 8 — File storage
- [ ] Mock 9 — Job scheduler
- [ ] Mock 10 — Document management system

---

## Part 8 — Weak areas log

| Date | Topic | What went wrong | Fix / revisit date |
|---|---|---|---|
|  |  |  |  |

Update after every mock. Patterns here tell you where to spend your remaining time.
