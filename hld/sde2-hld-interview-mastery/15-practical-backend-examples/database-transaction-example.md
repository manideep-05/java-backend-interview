# Database Transactions (Java / Spring Boot)

> Concept: [transactions-and-isolation](../03-databases-and-data-modeling/transactions-and-isolation.md)

**HLD point:** group multiple writes into one atomic unit (all-or-nothing) and choose the isolation level deliberately. The transaction is the DB's guarantee; `@Transactional` is how the app declares the boundary.

**Where the concern lives:** **the database** provides ACID; the app declares the boundary and the isolation level.

```java
@Service
class TransferService {
    private final AccountRepo accounts;

    // All-or-nothing: both updates commit or neither does.
    @Transactional
    public void transfer(long fromId, long toId, BigDecimal amount) {
        Account from = accounts.findById(fromId).orElseThrow();
        if (from.getBalance().compareTo(amount) < 0)
            throw new InsufficientFundsException(); // rollback: nothing persisted
        accounts.debit(fromId, amount);
        accounts.credit(toId, amount);
        // on commit: both persisted atomically; on exception: both rolled back
    }
}
```

```java
// Choosing an isolation level deliberately (default is usually Read Committed).
@Transactional(isolation = Isolation.SERIALIZABLE)  // only where you need the strongest guarantee
public void applyFinancialInvariant(...) { ... }
```

```java
// Atomic conditional update — prevent oversell WITHOUT read-then-write (no race):
@Modifying
@Query("UPDATE Inventory i SET i.qty = i.qty - 1 WHERE i.sku = :sku AND i.qty > 0")
int tryDecrement(@Param("sku") String sku);
// returns rows affected: 1 = success, 0 = out of stock. No check-then-act race.
```

**Key points to say:**
- Keep transactions **short** — long transactions hold locks and connections (contention; see [database-capacity-and-connection-pools](../11-estimation-and-bottleneck-analysis/database-capacity-and-connection-pools.md)). Don't do slow I/O (external calls, file streaming) inside a transaction.
- **Escalate isolation only where needed** (Serializable for specific invariants) — it costs concurrency/aborts. Read Committed is the pragmatic default (see [transactions-and-isolation](../03-databases-and-data-modeling/transactions-and-isolation.md)).
- For "last item" / counters, prefer an **atomic conditional update** (`WHERE qty > 0`) over read-check-then-write — the latter races under concurrency (oversell).
- `@Transactional` on a **public** method called from *another* bean (Spring proxy); self-invocation bypasses the proxy — a classic bug.
- Transactions are **single-datastore** — they don't span services/shards (that needs sagas; see [distributed-transactions](../03-databases-and-data-modeling/distributed-transactions.md)).

**Caveat:** production needs careful transaction scoping (avoid wrapping external calls), rollback-for config on checked exceptions, and awareness of the connection held for the transaction's duration.

**Maps to your resume:** your Oracle workflow modules (approvals/revisions) rely on transactional correctness; the atomic-decrement pattern is how you'd prevent concurrent approval/state races.
