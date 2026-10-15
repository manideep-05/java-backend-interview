# Redis Caching — Cache-Aside (Java / Spring Boot)

> Concept: [cache-aside-read-through-write-through](../04-caching/cache-aside-read-through-write-through.md) · [cache-invalidation-and-consistency](../04-caching/cache-invalidation-and-consistency.md) · [redis-design-and-failure-modes](../04-caching/redis-design-and-failure-modes.md)

**HLD point:** offload read-heavy, change-tolerant reads from the DB with cache-aside; **delete** the key on write (not update); define TTL + a Redis-down fallback.

**Where the concern lives:** app orchestrates cache-aside; Redis stores; the DB stays the source of truth.

```java
@Service
class DocumentMetadataService {
    private final StringRedisTemplate redis;
    private final DocumentRepo repo;
    private static final Duration TTL = Duration.ofMinutes(5);

    public DocumentMeta get(Long id) {
        String key = "doc:meta:" + id;
        // 1) check cache
        String cached = safeGet(key);
        if (cached != null) return deserialize(cached);
        // 2) miss -> load from DB (source of truth)
        DocumentMeta meta = repo.findMetaById(id).orElseThrow();
        // 3) populate cache with TTL (+ small jitter to avoid synchronized expiry)
        safeSet(key, serialize(meta), TTL.plusSeconds(ThreadLocalRandom.current().nextInt(30)));
        return meta;
    }

    @Transactional
    public void update(Long id, UpdateCmd cmd) {
        repo.update(id, cmd);          // 1) write DB
        safeDelete("doc:meta:" + id);  // 2) DELETE the key (not update) -> next read reloads
    }

    // Redis-down resilience: a cache failure must NOT fail the request.
    private String safeGet(String key) {
        try { return redis.opsForValue().get(key); }
        catch (RuntimeException e) { return null; } // treat as a miss -> fall back to DB
    }
    private void safeSet(String key, String val, Duration ttl) {
        try { redis.opsForValue().set(key, val, ttl); } catch (RuntimeException ignored) {}
    }
    private void safeDelete(String key) {
        try { redis.delete(key); } catch (RuntimeException ignored) {}
    }
}
```

Spring's declarative caching (`@Cacheable` / `@CacheEvict`) does the same with less code when the semantics fit:
```java
@Cacheable(value = "docMeta", key = "#id")
public DocumentMeta get(Long id) { return repo.findMetaById(id).orElseThrow(); }

@CacheEvict(value = "docMeta", key = "#id")   // delete-on-write (invalidate)
public void update(Long id, UpdateCmd cmd) { repo.update(id, cmd); }
```

**Key points to say:**
- **Delete, don't update**, the key on write — avoids concurrent-writer races; the next read reloads the authoritative value (see [cache-aside](../04-caching/cache-aside-read-through-write-through.md)). Note the residual delete-on-write race (a slow reader can repopulate a stale value) — bound it with the TTL.
- **TTL jitter** prevents synchronized expiry (cache avalanche; see [ttl-eviction-and-memory-management](../04-caching/ttl-eviction-and-memory-management.md)).
- **Redis-down must not fail requests** — treat cache errors as a miss and fall back to the DB; at scale add a concurrency limiter so the fallback doesn't stampede the DB (see [redis-design-and-failure-modes](../04-caching/redis-design-and-failure-modes.md)).
- **Don't cache strongly-consistent data** (e.g., permissions) with a long TTL — stale authz is a security bug; use short TTL + prompt invalidation.

**Caveat:** production needs a stampede guard (request coalescing) for hot keys, metrics on hit ratio, and a serialization strategy; the `safeGet`-returns-null-on-error approach needs a DB-overload guard at high scale.

**Maps to your resume:** your 90% Oracle optimization used materialized views (precompute) and could pair with cache-aside for hot read paths; JobFinder's Redis usage is this pattern.
