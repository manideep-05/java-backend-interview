# Optimistic Locking (Java / Spring Boot)

> Concept: [transactions-and-isolation](../03-databases-and-data-modeling/transactions-and-isolation.md) (lost updates)

**HLD point:** prevent two concurrent edits from clobbering each other (lost update) *without holding locks* — detect the conflict at write time via a version column; retry or return `409` on conflict. Great when conflicts are rare (typical web edits).

**Where the concern lives:** the **DB version column + JPA `@Version`** detect the conflict; the app decides retry vs 409.

```java
@Entity
class Document {
    @Id Long id;
    String name;

    @Version                       // JPA auto-increments and checks this on update
    long version;
}
```

JPA generates `UPDATE ... SET name=?, version=version+1 WHERE id=? AND version=:expected`. If another transaction already bumped the version, **0 rows update** → JPA throws `OptimisticLockException` (`ObjectOptimisticLockingFailureException` in Spring).

```java
@Service
class DocumentService {
    private final DocumentRepo repo;

    public Document rename(Long id, String newName, long expectedVersion) {
        Document doc = repo.findById(id).orElseThrow();
        if (doc.getVersion() != expectedVersion)
            throw new ConflictException("stale"); // client sent an old version
        doc.setName(newName);
        try {
            return repo.save(doc);               // version check happens here
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("concurrent modification"); // -> HTTP 409
        }
    }
}
```

```java
// Controller surfaces 409 so the client reloads + retries.
@PatchMapping("/documents/{id}")
ResponseEntity<Document> rename(@PathVariable Long id, @RequestBody RenameReq req) {
    try {
        return ResponseEntity.ok(service.rename(id, req.name(), req.version()));
    } catch (ConflictException e) {
        return ResponseEntity.status(409).build(); // client: reload and retry
    }
}
```

**Optional: auto-retry transient conflicts** (when the "latest wins" is acceptable):
```java
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
           maxAttempts = 3, backoff = @Backoff(delay = 50))
public Document renameWithRetry(Long id, String newName) { ... }
```

**Key points to say:**
- **Optimistic** (no locks, detect at write, retry on conflict) — best when conflicts are *rare* (two people rarely edit the same doc at once).
- **Pessimistic** (`SELECT ... FOR UPDATE` / `@Lock(PESSIMISTIC_WRITE)`) — hold the lock; use when conflicts are *frequent* (hot rows), accepting contention/deadlock risk.
- The client sends the `version` it read; a mismatch (or the DB check) → `409`; the client reloads and retries. This is standard REST conflict handling.
- Optimistic locking is a *single-node* mechanism — it doesn't coordinate across services (see [distributed-locks](../06-distributed-systems/distributed-locks.md) for cross-machine exclusion, and why that needs fencing).

**Caveat:** production needs the `version` exposed in the API responses, a clear client retry UX, and a decision on auto-retry vs surfacing the conflict.

**Maps to your resume:** your document revision/approval workflows — concurrent reviewers editing the same document — are exactly the lost-update problem optimistic locking solves.
