# Transactional Outbox (Java / Spring Boot)

> Concept: [transactional-outbox-and-cdc](../05-messaging-and-event-driven-systems/transactional-outbox-and-cdc.md)

**HLD point:** solve the **dual-write problem** — "update the DB *and* publish an event" aren't atomic, so a crash in between loses the event (or publishes a phantom). Write the event to an **outbox table in the same transaction** as the business change; a relay publishes it afterward.

**Where the concern lives:** the **DB transaction** makes the business change + event atomic; a relay (or CDC) publishes; the broker delivers at-least-once.

```java
@Entity
@Table(name = "outbox")
class OutboxEvent {
    @Id @GeneratedValue Long id;
    String aggregateType;      // e.g. "Document"
    String aggregateId;
    String type;               // e.g. "document.updated"
    @Column(columnDefinition = "text") String payload;
    @Enumerated(EnumType.STRING) Status status = Status.PENDING; // PENDING/SENT
    Instant createdAt = Instant.now();
    enum Status { PENDING, SENT }
}
```

```java
// 1) Business change + outbox insert in ONE transaction -> atomic. No dual write.
@Service
class DocumentService {
    private final DocumentRepo docs;
    private final OutboxRepo outbox;

    @Transactional
    public void update(Long id, UpdateCmd cmd) {
        docs.update(id, cmd);                                   // business change
        outbox.save(new OutboxEvent("Document", id.toString(),
                "document.updated", serialize(cmd)));           // event, SAME transaction
        // commit: both persisted together, or neither. The event's existence is guaranteed.
    }
}
```

```java
// 2) Relay: poll PENDING outbox rows and publish; mark SENT. (At-least-once -> consumers idempotent.)
@Component
class OutboxRelay {
    private final OutboxRepo outbox;
    private final RabbitTemplate rabbit;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outbox.findTop100ByStatusOrderByCreatedAt(Status.PENDING);
        for (OutboxEvent e : batch) {
            rabbit.convertAndSend("notify.exchange", e.getType(), e.getPayload());
            e.setStatus(Status.SENT); // if the relay crashes before this, the row stays PENDING -> re-published
        }
    }
}
```

**Key points to say:**
- The **dual-write problem**: DB and broker are two systems with no shared transaction; a crash between "commit DB" and "publish" loses the event. The outbox makes the event's *existence* atomic with the business change (one DB transaction).
- The relay publishing is **at-least-once** (relay may crash after publishing, before marking SENT → re-publish) → **downstream consumers must be idempotent** (dedup on event id).
- **Polling relay** (shown) is simple; **CDC** (e.g., Debezium streaming the WAL/outbox table) is the near-real-time, robust alternative that needs no app publish code (see [transactional-outbox-and-cdc](../05-messaging-and-event-driven-systems/transactional-outbox-and-cdc.md)).
- Monitor **relay lag** (outbox PENDING depth) — a stuck relay means stale downstreams.

**Caveat:** production needs claim/locking so multiple relay instances don't double-publish (e.g., `SELECT ... FOR UPDATE SKIP LOCKED`), outbox cleanup/retention, and CDC instead of polling at high volume.

**Maps directly to your resume:** your notification platform "uses the outbox pattern." This is the Java/Spring expression — in an interview, explain the dual-write problem it solves (that's the senior insight most candidates miss).
