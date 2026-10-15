# Timeout Configuration (Java / Spring Boot)

> Concept: [partial-failures-and-timeouts](../06-distributed-systems/partial-failures-and-timeouts.md) · [timeouts-retries-and-deadlines](../07-reliability-and-resilience/timeouts-retries-and-deadlines.md)

**HLD point:** **every** network call needs a timeout. A missing timeout lets a slow dependency hang threads → pool exhaustion → cascading outage (Little's Law). Base timeouts on the dependency's p99, and propagate a deadline down the chain.

**Where the concern lives:** app (every client config); the discipline is "no un-timed network calls."

```java
// HTTP client (RestClient / RestTemplate) — connect + read timeouts.
@Bean
RestClient restClient() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(1000);   // ms to establish the connection
    factory.setReadTimeout(2000);      // ms to wait for the response (~ just above dependency p99)
    return RestClient.builder().requestFactory(factory).build();
}
```

```java
// WebClient (reactive) with a response timeout.
@Bean
WebClient webClient() {
    HttpClient http = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        .responseTimeout(Duration.ofMillis(2000));
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(http)).build();
}
```

```java
// JDBC query timeout (don't let a slow query hold a connection forever).
jdbcTemplate.setQueryTimeout(3); // seconds
// or per @Transactional:
@Transactional(timeout = 3)
public void doWork() { ... }
```

```java
// Deadline propagation: pass the remaining budget down so retries/hops fit the client's deadline.
long deadlineMs = requestStartMs + 800;           // overall request budget
long remaining = deadlineMs - System.currentTimeMillis();
int callTimeout = (int) Math.min(2000, Math.max(1, remaining)); // min(p99-based, remaining)
// use callTimeout for the next hop; if remaining <= 0, fail fast (client already gave up).
```

**Key points to say:**
- **Every** network call (HTTP, DB, cache, RPC) must have a timeout — a missing one is a hang waiting to happen; via Little's Law, slow calls pile up in-flight requests → thread/connection pool exhaustion → cascade (see [partial-failures-and-timeouts](../06-distributed-systems/partial-failures-and-timeouts.md)).
- Base the value on the dependency's **p99** (slightly above) — too tight fails healthy slow requests; too loose holds resources during incidents.
- **Deadline propagation**: each hop uses `min(its p99-based timeout, remaining request budget)`, so nested calls/retries don't exceed the client's patience (wasted work after the client gave up).
- A timeout can be *wrong* (the work succeeded just after you gave up) → retries need **idempotency** (see [retries-and-duplicate-execution](../06-distributed-systems/retries-and-duplicate-execution.md)).
- Pair timeouts with a **circuit breaker** (fail fast when a dependency is down) — see [circuit-breaker-example](./circuit-breaker-example.md).

**Caveat:** production needs timeouts on *all* clients (easy to miss one), deadline propagation through the call chain, and consistent config (often via a shared client library/service mesh).

**Maps to your resume:** your EPDDMS↔VDMS fault-tolerant integration — timeouts + retries + circuit breakers are what make a cross-system call resilient instead of a cascade risk.
