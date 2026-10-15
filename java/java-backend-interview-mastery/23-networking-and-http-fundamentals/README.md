# 23 — Networking & HTTP Fundamentals

> **Priority: P1.** Interview-relevant basics that help you debug, deploy, and operate services — DNS, TCP, TLS, HTTP versions, keep-alive, timeouts. Not a networking deep-dive; enough to reason about latency and failures.

**Prerequisites:** [18 API](../18-api-engineering/).
**Related:** [22 Debugging](../22-debugging-and-production-engineering/), [14 Security](../14-spring-security/).

---

## A. Concept & intuition
When your service calls another, several layers must succeed: resolve a name (DNS), open a connection (TCP), secure it (TLS), and exchange HTTP messages. Each layer adds latency and failure modes. Understanding them explains timeouts, connection-pool behavior, and "slow first request" symptoms.

## B. Core concepts

### DNS
Resolves a hostname to IPs; results are cached (TTL). A slow/failing resolver, or a stale cache after a failover, causes connection delays/errors. The JVM also caches DNS (configurable); in containers, resolution goes through the platform resolver.

### TCP
A reliable, ordered byte stream. Setup is a **3-way handshake** (SYN, SYN-ACK, ACK) — one round trip before any data, which is why connection reuse matters. Teardown is a 4-way (FIN/ACK). `TCP_NODELAY` disables Nagle's algorithm for latency-sensitive small messages.

### TLS (HTTPS)
On top of TCP, a **handshake** negotiates the protocol version and cipher, authenticates the server via its certificate (chain validated to a trusted CA), and establishes session keys. In TLS 1.2 this adds ~2 round trips; **TLS 1.3** cuts it to ~1 (and 0-RTT for resumption). This is why the *first* HTTPS request to a host is slower — DNS + TCP + TLS before any HTTP — and why connection reuse/pools help so much.

### HTTP versions
- **HTTP/1.1:** one request at a time per connection (pipelining rarely used); keep-alive reuses the connection; concurrency needs multiple connections → **head-of-line blocking** per connection.
- **HTTP/2:** multiplexes many streams over one connection (no app-level HOL blocking), header compression, server push — fewer connections, better latency.
- **HTTP/3 (QUIC over UDP):** removes TCP-level head-of-line blocking and speeds up connection setup; useful on lossy networks (P2 depth).

### Keep-alive & connection pooling
Reusing connections avoids repeating DNS/TCP/TLS per request — a big latency win. HTTP clients (Apache HttpClient, OkHttp, the JDK `HttpClient`) pool connections; size the pool to concurrency and set idle/max-lifetime so stale connections are recycled (a server or LB may close idle connections, causing sporadic failures if not handled).

### Proxies, reverse proxies, load balancers
A reverse proxy / LB (NGINX, ALB) terminates client connections, balances across instances, and often terminates TLS. Know that it can add timeouts, buffering, and connection limits of its own — a common hidden cause of 502/504s.

### Cookies & sessions
Cookies carry state (session id, tokens) across stateless HTTP. `HttpOnly`/`Secure`/`SameSite` flags matter for security ([14](../14-spring-security/)).

### CORS & preflight
Browsers enforce the same-origin policy; cross-origin calls trigger a **preflight** `OPTIONS` for non-simple requests, which the server must answer with the allowed origins/methods/headers. CORS is a browser mechanism, not server-side security ([14 SEC-04](../14-spring-security/)).

### Timeouts (the operational essentials)
- **Connect timeout:** cap time to establish the TCP/TLS connection.
- **Read/socket timeout:** cap time waiting for response bytes.
- No timeout = a slow dependency can hang your threads indefinitely → cascading failure ([22](../22-debugging-and-production-engineering/)). Always set both.

## C. Code
```java
// JDK HttpClient with explicit timeouts + connection reuse (HTTP/2 by default)
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(2))                 // connect timeout
    .build();                                              // reuses connections (keep-alive)
HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.example.com/x"))
    .timeout(Duration.ofSeconds(5))                        // request (read) timeout
    .GET().build();
HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
```

## D. Trade-offs

| Topic | Option | Note |
|---|---|---|
| Connection | new per request | pays DNS+TCP+TLS each time; slow |
| | pooled/keep-alive | reuse; size to concurrency, recycle idle |
| HTTP version | 1.1 | simple, per-connection HOL blocking |
| | 2 | multiplexed, fewer connections |
| TLS | 1.2 | ~2 RTT handshake |
| | 1.3 | ~1 RTT, 0-RTT resumption |

## E. Interview Q&A

### NET-01 (P1) — What happens, step by step, when your service makes an HTTPS call?
**Strong answer:** Resolve the hostname via DNS (cached by TTL), open a TCP connection with the 3-way handshake (1 RTT), perform the TLS handshake to negotiate version/cipher and validate the server certificate and derive session keys (~1 RTT in TLS 1.3, ~2 in 1.2), then send the HTTP request and read the response. The first call is slowest because of DNS+TCP+TLS; reusing a pooled keep-alive connection skips those on subsequent calls. If any layer stalls and I have no timeout, my thread hangs.

<a id="net-02"></a>
### NET-02 (P1) — Walk through the TLS handshake and why the first request is slow.
**Strong answer:** The client and server negotiate the TLS version and cipher suite, the server presents its certificate chain which the client validates up to a trusted CA (and checks hostname/expiry), and they establish symmetric session keys (via ECDHE). That costs round trips on top of the TCP handshake, so the first HTTPS request to a host pays DNS + TCP + TLS before any HTTP bytes — which is why connection reuse and TLS 1.3 (fewer RTTs, resumption) matter for latency.

### NET-03 (P1) — HTTP/1.1 vs HTTP/2?
**Strong answer:** HTTP/1.1 handles one request at a time per connection with keep-alive, so concurrency needs multiple connections and each connection has head-of-line blocking. HTTP/2 multiplexes many streams over a single connection with header compression, reducing connections and latency and eliminating application-level HOL blocking (though TCP-level HOL remains, which HTTP/3 over QUIC addresses). For service-to-service calls, HTTP/2 means fewer, better-utilized connections.

### NET-04 (P2) — Why does HTTP/3 exist?
**Strong answer:** HTTP/2 still runs over TCP, so a lost packet blocks all multiplexed streams (TCP head-of-line blocking). HTTP/3 runs over QUIC (UDP) with per-stream delivery and faster (often 0-RTT) connection setup, so loss on one stream doesn't stall the others — a win on lossy/mobile networks.

### NET-05 (P1) — Why do connection pools and timeouts matter?
**Strong answer:** Pooling reuses connections so I don't repay DNS/TCP/TLS per request — a major latency and CPU saving — but I must size the pool to concurrency and recycle idle/old connections because a server or load balancer may silently close idle ones, causing sporadic errors. Timeouts (connect and read) are mandatory: without them a slow or dead dependency ties up threads indefinitely and cascades into a full outage.

### NET-06 (P1) — What is a CORS preflight and is CORS a security control?
**Strong answer:** For non-simple cross-origin requests, the browser first sends an `OPTIONS` preflight asking which origins/methods/headers are allowed; the server responds and the browser then allows or blocks the real request. CORS is a **browser** policy to relax the same-origin restriction — it protects users' browsers, not the server, so it's not a server-side authorization mechanism; the API still needs real authn/authz.

## F. Production relevance
These basics explain and prevent real incidents: missing timeouts turning a slow dependency into a thread-exhaustion outage ([22](../22-debugging-and-production-engineering/)); "slow first request" from cold DNS/TCP/TLS fixed by connection pooling; sporadic connection-reset errors from LBs closing idle pooled connections (set max-lifetime/idle); 502/504 from a reverse proxy's own timeouts; and CORS confusion where developers treat it as security. Knowing the layers lets you read a latency breakdown and point at the right culprit.

## G. Revision

### 10 key takeaways
1. A call = DNS → TCP handshake → TLS handshake → HTTP exchange.
2. First HTTPS request is slow (DNS+TCP+TLS); reuse connections.
3. TCP needs a 3-way handshake (1 RTT) before data.
4. TLS 1.3 ~1 RTT vs 1.2 ~2 RTT; validates the server cert.
5. HTTP/1.1 = one-at-a-time per connection (HOL); HTTP/2 multiplexes.
6. HTTP/3 (QUIC) removes TCP HOL blocking.
7. Keep-alive/pooling avoids repeating setup; recycle idle connections.
8. Always set connect + read timeouts.
9. Reverse proxies/LBs add their own timeouts/limits.
10. CORS is a browser policy, not server security.

### 10 rapid-fire
1. Name→IP? (DNS) 2. TCP setup RTTs? (1, 3-way) 3. TLS 1.3 RTT? (~1) 4. HTTP/2 feature? (multiplexing) 5. HTTP/3 transport? (QUIC/UDP) 6. Avoid repeating setup? (keep-alive/pool) 7. Mandatory on calls? (timeouts) 8. Cross-origin check? (preflight OPTIONS) 9. CORS = security? (no) 10. Slow first request cause? (cold DNS/TCP/TLS)

### 5 tricky
1. Why is the first request to a host slow? 2. Why sporadic connection resets with a pool? (LB closed idle) 3. Why did a missing timeout cause an outage? 4. HTTP/2 vs HTTP/3 HOL blocking? 5. Why isn't CORS enough to secure an API?

### 5 exercises
1. Configure the JDK HttpClient with connect/read timeouts and HTTP/2.
2. Measure first-vs-subsequent request latency to see connection reuse.
3. Set connection max-lifetime/idle and explain why.
4. Configure CORS allowed origins and trigger a preflight.
5. Trace a 504 to a reverse-proxy timeout vs an upstream timeout.

### One-page revision sheet
> Call = **DNS → TCP(3-way, 1 RTT) → TLS(1.3 ~1 RTT, validates cert) → HTTP**. First HTTPS request slow → **reuse pooled keep-alive connections**. HTTP/1.1(one-at-a-time, HOL) → HTTP/2(multiplex, header compress) → HTTP/3(QUIC/UDP, no TCP HOL). **Always set connect + read timeouts** (no timeout = thread hang = outage). Pool: size to concurrency, recycle idle (LBs close idle → resets). Reverse proxy/LB has its own timeouts (502/504). **CORS = browser policy, not server auth** (preflight OPTIONS).

### Prerequisites to revisit
- API timeouts/retries (ch.18); feeds the slow-dependency playbook (ch.22); TLS ties to security (ch.14).
