# Distributed Rate Limiting (Java / Spring Boot + Redis)

> Concept: [rate-limiting](../01-networking-and-api-design/rate-limiting.md) · [rate-limiter design](../12-system-design-problems/rate-limiter.md)

**HLD point:** across N app instances, per-instance counters let a client exceed the limit N×, so the counter must be **shared** (Redis) and the check-and-decrement **atomic** (a Lua script in one round trip).

**Where the concern lives:** **Redis holds the shared state and runs the atomic script**; the app enforces the decision.

```java
// Token-bucket check as a single atomic Lua script (refill + check + consume).
// Atomicity in ONE Redis round trip prevents two instances both seeing a free token.
@Component
class RedisRateLimiter {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script;

    RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class);
    }

    /** @return true if allowed. key = e.g. "rl:" + apiKey */
    boolean allow(String key, int capacity, double refillPerSec) {
        long now = System.currentTimeMillis();
        Long allowed = redis.execute(script, List.of(key),
                String.valueOf(capacity), String.valueOf(refillPerSec), String.valueOf(now));
        return allowed != null && allowed == 1L;
    }

    private static final String TOKEN_BUCKET_LUA = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local refill = tonumber(ARGV[2])      -- tokens per second
        local now = tonumber(ARGV[3])         -- ms
        local b = redis.call('HMGET', key, 'tokens', 'ts')
        local tokens = tonumber(b[1]); local ts = tonumber(b[2])
        if tokens == nil then tokens = capacity; ts = now end
        local delta = math.max(0, now - ts) / 1000.0
        tokens = math.min(capacity, tokens + delta * refill)   -- refill
        local allowed = 0
        if tokens >= 1 then tokens = tokens - 1; allowed = 1 end -- consume
        redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
        redis.call('PEXPIRE', key, 60000)
        return allowed
        """;
}
```

```java
// Enforce at a filter/interceptor; return 429 + Retry-After on reject.
@Component
class RateLimitFilter extends OncePerRequestFilter {
    private final RedisRateLimiter limiter;

    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String apiKey = req.getHeader("X-API-Key");
        boolean allowed;
        try {
            allowed = limiter.allow("rl:" + apiKey, 100, 100.0 / 60); // 100/min
        } catch (RuntimeException redisDown) {
            allowed = true; // FAIL-OPEN for availability (configurable; fail-closed for abuse-critical)
        }
        if (!allowed) {
            res.setStatus(429);
            res.setHeader("Retry-After", "30");
            return;
        }
        chain.doFilter(req, res);
    }
}
```

**Key points to say:**
- **Per-instance counters fail** for a global limit (N× overshoot) → shared Redis state.
- The **Lua script is atomic in one round trip** — prevents the check-and-decrement race (two instances both seeing a token).
- **Fail-open vs fail-closed** when Redis is down is an explicit, configured decision (fail-open with a local cap for user-facing availability; fail-closed for abuse-critical). Shown as fail-open here.
- Return **429 + Retry-After**; key by the right dimension (API key / user / IP) for the abuse you're preventing.

**Caveat:** production needs a local fallback cap for the Redis-down case, hot-key handling (shard the limiter key / local pre-check), and tight Redis timeouts so a slow Redis can't hang requests.

**Maps to your resume:** protecting your notification platform / APIs from abuse or runaway clients; the fail-open decision mirrors your production reliability thinking.
