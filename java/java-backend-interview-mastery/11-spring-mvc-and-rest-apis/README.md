# 11 — Spring MVC & REST APIs

> **Priority: P0.** You must be able to trace a request from the socket to the database and back, name where each concern attaches (filter/interceptor/AOP), and separate DTOs from entities. This is the backbone of every Spring Boot web service.

**Prerequisites:** [09 Spring Core](../09-spring-core-and-dependency-injection/), [23 HTTP](../23-networking-and-http-fundamentals/).
**Related:** [13 Transactions](../13-spring-transactions/), [12 JPA](../12-spring-data-jpa-and-hibernate/), [18 API engineering](../18-api-engineering/).

---

## A. Concept & intuition
Spring MVC maps HTTP requests to Java methods and serializes responses back, handling parsing, validation, content negotiation, and error translation so you write handlers, not socket code. Before it, you wrote raw servlets with manual parameter parsing and response writing.

## B. The request lifecycle — trace it end to end
```
Client
  → TCP/TLS (servlet container: Tomcat) accepts, assigns a worker thread
  → Filter chain (Servlet Filters: security, CORS, logging, compression)
  → DispatcherServlet (the front controller)
  → HandlerMapping picks the @Controller method
  → HandlerInterceptor.preHandle
  → argument resolution (@PathVariable/@RequestParam/@RequestBody via HttpMessageConverter)
  → Bean Validation (@Valid)
  → Controller method
  → Service (@Transactional boundary usually here)
  → Repository → JDBC → Database
  ← return value
  → HttpMessageConverter serializes (Jackson → JSON)
  → HandlerInterceptor.postHandle / afterCompletion
  → Filter chain (response side)
  → Client
```
Key facts: one request occupies **one servlet worker thread** for its whole duration (unless async) — blocking a handler ties up that thread (relates to pool sizing, [06](../06-java-concurrency/), and virtual threads). Exceptions bubble to `@ControllerAdvice`/`@ExceptionHandler` for consistent error responses.

### Filters vs interceptors vs AOP (MVC-03)
| Mechanism | Scope | Sees | Use for |
|---|---|---|---|
| Servlet Filter | before DispatcherServlet, all requests incl. static | raw request/response | security, CORS, compression, request logging, correlation ID |
| HandlerInterceptor | around controller dispatch | handler + ModelAndView | auth checks tied to handlers, timing |
| AOP (`@Around`) | any Spring bean method | method args/return | business cross-cutting, `@Transactional` style |

## C. Code — a clean 3-layer endpoint
```java
// DTO at the boundary — NOT the entity
record CreateUserRequest(@NotBlank String name, @Email String email) {}
record UserResponse(Long id, String name) {}

@RestController
@RequestMapping("/api/v1/users")
class UserController {
    private final UserService service;
    UserController(UserService service){ this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest req) { // @Valid triggers validation
        return service.create(req.name(), req.email());
    }

    @GetMapping
    Page<UserResponse> list(Pageable pageable) { return service.list(pageable); } // pagination/sorting
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail onValidation(MethodArgumentNotValidException ex) { // RFC 7807 ProblemDetail (Spring 6)
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setDetail("Validation failed");
        return pd;
    }
}
```
```java
// BROKEN: returning the JPA entity directly
@GetMapping("/{id}") User get(@PathVariable Long id){ return repo.findById(id).orElseThrow(); }
// Problems: leaks DB columns (passwordHash!), triggers lazy-loading during serialization
// (LazyInitializationException / N+1), and couples the API contract to the schema.
// FIX: map to a DTO in the service (projection), never serialize entities.
```

## D. Trade-offs

| Decision | Option A | Option B | Guidance |
|---|---|---|---|
| Boundary type | Expose entity | DTO/record | DTO: decouples contract, avoids over-posting & lazy issues |
| Validation | manual ifs | `@Valid` + Bean Validation | annotations: declarative, consistent 400s |
| Error handling | per-controller try/catch | `@ControllerAdvice` | centralize for consistent error bodies |
| Pagination | OFFSET `Pageable` | keyset | keyset for deep pages ([16](../16-database-indexing-and-query-optimization/)) |
| Blocking vs async | servlet thread per request | reactive/virtual threads | blocking + virtual threads (Java 21) is simple and scales for IO |

## E. Interview Q&A

### MVC-01 (P1) — Trace a request through Spring MVC.
**Strong answer:** The container accepts the connection and assigns a worker thread; servlet filters run (security, CORS, logging); the DispatcherServlet (front controller) uses HandlerMapping to find the controller method; interceptors' preHandle run; arguments are resolved and the body deserialized by an HttpMessageConverter; `@Valid` validates; the controller calls the service (transaction boundary) which calls the repository/DB; the return value is serialized by a converter; interceptors' postHandle/afterCompletion and the response-side filters run; the response goes back. One request = one servlet thread for its duration.

### MVC-02 (P1) — `@Controller` vs `@RestController`; how does JSON happen?
**Strong answer:** `@RestController` = `@Controller` + `@ResponseBody` on every method, so return values are serialized to the body (JSON via Jackson by HttpMessageConverters) instead of resolved as view names. Content negotiation picks the converter from the `Accept` header.

<a id="mvc-03"></a>
### MVC-03 (P0) — Filter vs interceptor vs AOP — where does each sit?
**Strong answer:** Servlet filters run earliest, outside Spring MVC, on every request including static resources, and see the raw request/response — ideal for security, CORS, compression, and correlation IDs. HandlerInterceptors run inside the DispatcherServlet around the handler and can access the chosen handler and ModelAndView — good for handler-scoped concerns like auth and timing. AOP advice wraps any Spring bean method and sees method arguments/return — it's how `@Transactional` works and where I put business cross-cutting logic. I pick by what context I need and how broadly it should apply.

### MVC-04 (P1) — `@Valid` vs `@Validated`; where do validation errors go?
**Strong answer:** `@Valid` (Bean Validation) on a `@RequestBody` triggers constraint checks and throws `MethodArgumentNotValidException` on failure, which I map to a 400 in `@ControllerAdvice`. `@Validated` (Spring) adds validation **groups** and enables method-level validation on beans (e.g., validating `@RequestParam`), throwing `ConstraintViolationException`. I use `@Valid` for request bodies and `@Validated` when I need groups or method parameter validation.

<a id="mvc-05"></a>
### MVC-05 (P0) — Why separate DTOs from entities?
**Strong answer:** Returning entities leaks the database schema into the API contract (including sensitive columns), couples clients to the persistence model so schema changes break them, triggers lazy-loading during JSON serialization (LazyInitializationException / N+1), and on input enables over-posting (clients setting fields they shouldn't). DTOs/records give an explicit, stable contract, let me expose only what's intended, and keep the entity free to evolve. I map entity↔DTO in the service layer, ideally via projections for reads.

### MVC-06 (P1) — How do you return consistent errors?
**Strong answer:** A `@RestControllerAdvice` with `@ExceptionHandler` methods translating exceptions to a consistent body — Spring 6 provides `ProblemDetail` (RFC 7807). I map validation to 400, not-found to 404, conflicts (e.g., optimistic lock / unique violation) to 409, auth to 401/403, and unexpected errors to 500 without leaking stack traces. Consistent, documented error shapes are part of the API contract ([18](../18-api-engineering/)).

### MVC-07 (P2) — How do you handle large file upload/download and streaming?
**Strong answer:** For uploads, `MultipartFile` or stream the `InputStream` to avoid buffering the whole file in memory, and set size limits. For downloads/streaming, return a `StreamingResponseBody` or `ResponseEntity<Resource>` and set `Content-Disposition`; stream rather than materialize to bound memory. Watch request timeouts and backpressure for very large transfers.

## F. Production relevance
MVC choices determine contract stability and resource safety: DTOs prevent accidental data leaks and lazy-loading explosions in production; centralized error handling gives clients predictable responses and keeps stack traces out of bodies; understanding the one-thread-per-request model explains latency under load and informs pool sizing and the move to virtual threads; filters for correlation IDs make distributed tracing possible ([26](../26-observability-and-reliability/)). The "endpoint slow but query fast" incident ([XT-02](../27-cross-topic-interview-scenarios/)) usually traces to serialization-time N+1 from exposing entities.

## G. Revision

### 10 key takeaways
1. One request = one servlet worker thread (blocking ties it up).
2. Lifecycle: filters → DispatcherServlet → mapping → interceptors → args/validation → controller → service(tx) → repo/DB → converter → back.
3. Filters (raw, broad) vs interceptors (handler-scoped) vs AOP (bean methods).
4. `@RestController` serializes return values (Jackson converters).
5. Always use DTOs/records at the boundary, never entities.
6. `@Valid` for bodies; `@Validated` for groups/method params.
7. Centralize errors in `@ControllerAdvice`; use `ProblemDetail`.
8. Map conflicts to 409, validation to 400, not-found to 404.
9. Stream large uploads/downloads; set limits/timeouts.
10. Blocking + virtual threads scales IO simply (Java 21).

### 10 rapid-fire
1. Front controller? (DispatcherServlet) 2. Where does CORS run? (filter) 3. JSON converter? (Jackson/HttpMessageConverter) 4. Global error handling? (@ControllerAdvice) 5. Validate a body? (@Valid) 6. Validation exception type? (MethodArgumentNotValidException) 7. RFC 7807 type? (ProblemDetail) 8. Threads per request? (one) 9. Expose entity in response? (no) 10. Pagination abstraction? (Pageable/Page)

### 5 tricky
1. Why did exposing an entity cause LazyInitializationException? 2. Filter vs interceptor for a correlation ID? (filter) 3. Why is one slow handler a scaling problem? (holds a worker thread) 4. `@Valid` vs `@Validated` difference? 5. Why map unique-violation to 409 not 500?

### 5 exercises
1. Build a 3-layer endpoint with DTOs, `@Valid`, and a `@ControllerAdvice`.
2. Add a correlation-ID filter and log it (MDC) through the request.
3. Reproduce entity-serialization N+1, fix with a DTO projection.
4. Return `ProblemDetail` for validation and 409 for optimistic-lock conflicts.
5. Stream a large file download with `StreamingResponseBody`.

### One-page revision sheet
> **Lifecycle:** container thread → filters(raw, all requests: security/CORS/correlation) → DispatcherServlet → HandlerMapping → interceptors(preHandle) → arg resolution + @Valid → controller → service(@Transactional) → repo/DB → HttpMessageConverter(JSON) → interceptors(post/afterCompletion) → filters → client. **One request=one thread.** Filters(raw/broad) vs interceptors(handler) vs AOP(bean methods). **DTOs/records at boundary, never entities** (leaks schema, lazy issues, over-posting). @Valid(bodies)/@Validated(groups/params). Errors → @ControllerAdvice + ProblemDetail; 400/404/409/401/403/500. Stream big files.

### Prerequisites to revisit
- Spring proxies/DI (ch.09); transaction boundary (ch.13); JPA lazy loading (ch.12); HTTP methods/status (ch.18, 23).
