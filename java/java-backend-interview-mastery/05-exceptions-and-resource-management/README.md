# 05 — Exceptions & Resource Management

> **Priority: P1.** Exception design and resource handling reveal engineering maturity: do you leak connections, swallow failures, and design error responses that help callers — or hide bugs?

**Prerequisites:** [01 Core Java](../01-core-java/).
**Related:** [11 MVC error handling](../11-spring-mvc-and-rest-apis/), [13 Transactions rollback](../13-spring-transactions/).

---

## A. Concept & intuition
Exceptions separate error handling from the happy path and carry context (type, message, stack, cause) to a handler that can act. Before structured exceptions you returned error codes everyone forgot to check. The engineering goal: fail loudly where you can't handle, handle where you can, and never leak resources.

## B. Internals & key distinctions

### Checked vs unchecked
- **Checked** (`extends Exception`): compiler forces `throws` or catch. Intended for recoverable conditions.
- **Unchecked** (`extends RuntimeException`): no compiler enforcement. Intended for programming errors/unrecoverable conditions.
- **`Error`**: JVM-level (OOM, StackOverflow) — don't catch.

Modern practice leans toward unchecked exceptions for most application errors (less boilerplate, no leaky `throws` across layers), wrapping checked lower-level exceptions in domain-specific unchecked ones. **Remember:** Spring rolls back only on unchecked by default ([13 TX-04](../13-spring-transactions/)).

### Propagation & chaining
An uncaught exception unwinds the stack until a matching handler. Always preserve the **cause** (`throw new DomainException("...", e)`) so you don't lose the original stack trace.

### try-with-resources & suppressed exceptions
`try (var x = ...)` auto-closes `AutoCloseable` resources in reverse order, even on exception — eliminating the leaked-connection bug of manual `finally`. If both the body and `close()` throw, the body's exception propagates and the close exception is attached as **suppressed** (`getSuppressed()`), not lost.

## C. Code — broken → fixed
```java
// BROKEN: manual close leaks on exception; swallows the real error
Connection c = ds.getConnection();
Statement s = c.createStatement();
// if this throws, c and s never close -> connection leak -> pool exhaustion
s.executeUpdate(sql);
s.close(); c.close();
```
```java
// FIXED: try-with-resources closes everything, in reverse order, even on exception
try (Connection c = ds.getConnection();
     PreparedStatement ps = c.prepareStatement(sql)) {
    ps.executeUpdate();
} // c and ps auto-closed; a close() failure becomes a suppressed exception
```
```java
// BROKEN: swallowed exception hides failures
try { risky(); } catch (Exception e) { /* nothing */ } // silent data loss / undebuggable
// FIXED: handle meaningfully or rethrow with context
try { risky(); } catch (IOException e) { throw new DocumentProcessingException("parsing failed for " + id, e); }
```

## D. Trade-offs

| Decision | Option | When | Caveat |
|---|---|---|---|
| checked vs unchecked | unchecked (default modern) | most app errors | won't force callers to handle |
| | checked | truly recoverable, caller must act | boilerplate `throws` across layers |
| exception vs return | exception | exceptional/unexpected | expensive if used for control flow |
| | `Optional`/result type | expected "not found" | not for genuine errors |
| broad vs specific catch | specific | normal | catching `Exception`/`Throwable` hides bugs |

## E. Interview Q&A

### EXC-01 (P1) — Checked vs unchecked; when each?
**Strong answer:** Checked extend `Exception` and the compiler forces handling — meant for recoverable conditions the caller should address; unchecked extend `RuntimeException` for programming errors or unrecoverable states. Modern services often prefer unchecked application exceptions (wrapping lower-level checked ones) to avoid `throws` leaking through every layer, and because Spring only rolls back on unchecked by default. I reserve checked for cases where I genuinely expect the caller to recover.

<a id="exc-02"></a>
### EXC-02 (P1) — try-with-resources and suppressed exceptions?
**Strong answer:** try-with-resources auto-closes any `AutoCloseable` in reverse declaration order, even when the body throws, which eliminates leaked connections/streams that manual `finally` blocks often got wrong. If both the body and `close()` throw, the body's exception propagates and the close exception is attached as a *suppressed* exception rather than silently replacing it — I can inspect it via `getSuppressed()`. It's the correct default for all resource handling.

### EXC-03 (P1) — Why is swallowing exceptions harmful?
**Strong answer:** An empty catch hides failures: data is silently lost, bugs become invisible, and incidents are undebuggable because the stack trace never surfaces. If I catch, I either handle meaningfully, log with context and rethrow, or wrap in a domain exception preserving the cause. In a transaction, swallowing also causes a surprise commit of partial work ([13](../13-spring-transactions/)).

<a id="exc-04"></a>
### EXC-04 (P1) — How do you design good error responses for an API?
**Strong answer:** Map exceptions to meaningful HTTP statuses in a `@ControllerAdvice` (400 validation, 404 not found, 409 conflict, 401/403 auth, 429 rate limit, 500 unexpected), return a consistent body (`ProblemDetail`/RFC 7807) with a stable error code and a safe message, never leak stack traces or internal details, and include a correlation ID so clients and logs line up. The contract for errors is as important as for success ([18](../18-api-engineering/)).

### EXC-05 (P2) — `finally` caveats?
**Strong answer:** `finally` always runs, but a `return`/`throw` inside `finally` overrides whatever the try/catch was returning or throwing — hiding the real result or exception, which is a bug. I never `return` from `finally`; I use try-with-resources for cleanup instead.

### EXC-06 (P1) — When should you use exceptions vs return values?
**Strong answer:** Exceptions for genuinely exceptional, unexpected conditions; expected outcomes like "not found" are better modeled with `Optional` or a result type, since using exceptions for ordinary control flow is costly (stack capture) and obscures logic. Validation failures are a gray area — I surface them as structured results/400s rather than deep exception chains.

## F. Production relevance
Resource and exception discipline directly affects reliability: try-with-resources prevents the connection/file-handle leaks that exhaust pools and crash services ([22](../22-debugging-and-production-engineering/)); preserving causes makes incidents debuggable; consistent error mapping keeps clients working and avoids leaking internals; and understanding checked-vs-unchecked prevents the silent-commit transaction bug. "I never swallow exceptions and I always use try-with-resources" is a small but strong signal.

## G. Revision

### 10 key takeaways
1. Checked=compiler-enforced/recoverable; unchecked=programming/unrecoverable; don't catch `Error`.
2. Modern apps lean unchecked; wrap lower-level checked exceptions.
3. Spring rolls back on unchecked by default.
4. Preserve the cause (`new X("...", e)`).
5. try-with-resources closes in reverse order, even on exception.
6. Close failures become suppressed exceptions, not lost.
7. Never swallow exceptions.
8. Map errors to stable HTTP statuses + ProblemDetail; no stack traces to clients.
9. Don't `return`/`throw` from `finally`.
10. Exceptions for the exceptional; Optional/result for expected absence.

### 10 rapid-fire
1. Checked base class? (Exception) 2. Unchecked base? (RuntimeException) 3. Auto-close interface? (AutoCloseable) 4. Lost close error becomes? (suppressed) 5. Spring rollback default? (unchecked only) 6. Not-found better as? (Optional) 7. 409 means? (conflict) 8. Catch Throwable? (no) 9. Return from finally? (no) 10. Keep original error via? (cause/chaining)

### 5 tricky
1. Why did a `return` in `finally` hide an exception? 2. Why does swallowing cause a surprise commit? 3. Where did the connection leak come from? (manual close on exception path) 4. Why wrap checked in unchecked across layers? 5. Why not use exceptions for "not found"?

### 5 exercises
1. Rewrite manual JDBC close into try-with-resources; force an exception and confirm closure.
2. Demonstrate a suppressed exception via a resource whose `close()` throws.
3. Build a `@ControllerAdvice` mapping 4 exception types to statuses + ProblemDetail.
4. Show a swallowed exception hiding a bug, then fix with rethrow+context.
5. Replace a "not found" exception with an `Optional` return.

### One-page revision sheet
> Checked(compiler-forced/recoverable) vs unchecked(programming/unrecoverable); don't catch Error. Modern=prefer unchecked, wrap+preserve cause. **Spring rolls back unchecked only.** **try-with-resources**: AutoCloseable, reverse order, even on exception; close failure=**suppressed**. Never swallow. Errors→@ControllerAdvice→stable status+ProblemDetail, no stack traces, correlation id. No return/throw in finally. Exceptions=exceptional; Optional/result=expected absence.

### Prerequisites to revisit
- Core Java objects (ch.01); applied in MVC errors (ch.11) and transactions (ch.13).
