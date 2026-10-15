# Idempotency Key (Java / Spring Boot)

> Concept: [idempotency-and-deduplication](../05-messaging-and-event-driven-systems/idempotency-and-deduplication.md) · [retries-and-duplicate-execution](../06-distributed-systems/retries-and-duplicate-execution.md)

**HLD point:** make a non-idempotent operation (create order, charge, send) safe to retry by recording a client-supplied key atomically *before* the effect; a repeat with the same key returns the stored result instead of re-acting.

**Where the concern lives:** the **unique constraint in the DB** is what makes the check-and-record atomic (not the app logic); the app orchestrates.

```java
// 1) The dedup record — the UNIQUE constraint is the atomicity guarantee.
@Entity
@Table(name = "idempotency_keys",
       uniqueConstraints = @UniqueConstraint(columnNames = "idem_key"))
class IdempotencyRecord {
    @Id @GeneratedValue Long id;
    @Column(name = "idem_key", nullable = false) String idemKey;
    @Column(columnDefinition = "text") String resultJson; // stored response
    Instant createdAt;
}

// 2) Service: insert-first (atomic), then perform the effect, then store the result.
@Service
class OrderService {
    private final IdempotencyRepo repo;
    private final OrderRepo orders;

    @Transactional
    public OrderResult create(String idemKey, CreateOrderCmd cmd) {
        // Try to claim the key. The unique constraint makes this the atomic gate:
        // two concurrent duplicates -> one insert wins, the other throws.
        try {
            repo.saveAndFlush(new IdempotencyRecord(idemKey)); // flush to surface the constraint now
        } catch (DataIntegrityViolationException dup) {
            // Key already used -> return the original result (no second effect).
            IdempotencyRecord existing = repo.findByIdemKey(idemKey).orElseThrow();
            return deserialize(existing.getResultJson());
        }
        // First time: perform the effect exactly once.
        Order order = orders.save(cmd.toOrder());
        OrderResult result = OrderResult.from(order);
        repo.updateResult(idemKey, serialize(result)); // same transaction
        return result;
    }
}
```

```java
// 3) Controller: the client supplies the key (e.g., a UUID per logical request).
@PostMapping("/orders")
ResponseEntity<OrderResult> create(
        @RequestHeader("Idempotency-Key") String idemKey,
        @RequestBody CreateOrderCmd cmd) {
    return ResponseEntity.status(201).body(orderService.create(idemKey, cmd));
}
```

**Key points to say in an interview:**
- The **atomicity is in the unique-constraint insert**, not an application `if (exists)` check — the latter races under concurrency (two requests both see "not exists").
- `saveAndFlush` forces the constraint violation to surface *now* (within the transaction), so you can catch the duplicate and return the stored result.
- Store the **result** so a retry returns the original outcome, not just "already done."
- For **external side effects** (charge a card), also pass the key to the provider so *it* dedupes (see [payment-system](../12-system-design-problems/payment-system.md)); the local key alone can't cover a crash between the external call and the local write — that's what **reconciliation** is for.
- **Retention:** expire old keys (TTL ≥ your max retry window) so the table doesn't grow forever.

**Caveat:** production needs result serialization/versioning, key TTL/cleanup, and handling the "in-progress" state (a second request arriving while the first is mid-flight) — e.g., a status column (PENDING/DONE) and a short wait/retry.

**This maps to your resume:** your EPDDMS↔VDMS idempotent sync and your JobFinder idempotent persistence are exactly this pattern (keyed per document+revision / per listing). See [YOUR-RESUME-STUDY-GUIDE](../YOUR-RESUME-STUDY-GUIDE.md).
