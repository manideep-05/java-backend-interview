# 21 — Testing & Code Quality

> **Priority: P1.** The signal is choosing the right test level, knowing when mocking hides bugs, and testing failure/async paths — not chasing coverage numbers.

**Prerequisites:** [09 Spring Core](../09-spring-core-and-dependency-injection/), [11 MVC](../11-spring-mvc-and-rest-apis/), [12 JPA](../12-spring-data-jpa-and-hibernate/).

---

## A. Concept & intuition
Tests exist to let you change code with confidence and to document intended behavior. The engineering problem: fast feedback (unit tests) vs realistic confidence (integration tests). The **test pyramid** — many fast unit tests, fewer integration tests, a thin layer of end-to-end — balances speed, reliability, and confidence. Coverage is a weak proxy; what matters is testing the behavior and the failure paths.

## B. Core concepts

### Test levels
- **Unit:** one class/method in isolation; collaborators mocked; milliseconds.
- **Integration:** several components together (e.g., repository + real DB via Testcontainers); seconds.
- **Component/slice:** a Spring slice (`@WebMvcTest` for the web layer, `@DataJpaTest` for persistence) with a minimal context.
- **End-to-end:** the whole system; slow and brittle — keep few.

### Test doubles (mock vs stub vs spy)
- **Stub:** returns canned data (state).
- **Mock:** verifies interactions (behavior) — assert it was called with X.
- **Spy:** wraps a real object, overriding some methods.
Over-mocking tests your mocks, not your code — a classic trap.

### Spring test slices
- `@SpringBootTest`: full application context (closest to real, slower) — for integration/wiring.
- `@WebMvcTest`: only the web layer (controllers, converters, advice) with services mocked (`@MockBean`) — fast controller tests.
- `@DataJpaTest`: JPA layer with an embedded/Testcontainers DB.

### Testcontainers
Spin up a **real** PostgreSQL/Oracle/Redis/Kafka in a container for integration tests — far more faithful than H2 (which hides dialect/behavior differences, e.g., Oracle's `''`=NULL, Postgres MVCC). Prefer it for DB tests.

### Transactional test caveat
`@Transactional` on a test rolls back after each test for isolation — convenient, but it can **hide** bugs: it never actually commits (so flush-timing/trigger/commit-only behavior isn't exercised), and it keeps everything in one transaction so you won't see cross-transaction visibility issues. For those, test without the rollback or commit explicitly.

### Testing async & flaky tests
Don't `Thread.sleep` to wait for async work — use Awaitility or latches to poll a condition. **Flaky tests** (pass/fail nondeterministically) usually come from timing, shared state, test order dependence, or real clocks/randomness — fix the root cause (inject clocks, isolate data), don't `@Disabled` or retry blindly.

### Code quality
Static analysis (SpotBugs, Error Prone, SonarQube) catches bugs/smells; coverage shows what's *executed*, not what's *verified* (you can have 100% coverage with zero assertions). Refactor legacy code safely by adding characterization tests first.

## C. Code
```java
// Unit test with Mockito: stub a dependency, verify an interaction
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock PaymentClient payments;
    @InjectMocks OrderService service;
    @Test void chargesOnce() {
        when(payments.charge(any())).thenReturn(PaymentStatus.OK); // stub
        service.place(new Order(1));
        verify(payments, times(1)).charge(any());                  // mock verification
    }
}
```
```java
// Web slice: fast controller test, service mocked
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mvc; @MockBean UserService service;
    @Test void validationReturns400() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(APPLICATION_JSON).content("{}"))
           .andExpect(status().isBadRequest());
    }
}
```
```java
// Integration test with a REAL Postgres (Testcontainers) — faithful dialect/behavior
@SpringBootTest
@Testcontainers
class UserRepositoryIT {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void props(DynamicPropertyRegistry r){
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }
    // ... assert real SQL/constraint behavior, e.g., a unique-constraint violation
}
```

## D. Trade-offs

| Level | Speed | Confidence | Use for |
|---|---|---|---|
| Unit | fast | logic correctness | business rules, edge cases |
| Slice (@WebMvcTest/@DataJpaTest) | medium | layer wiring | controllers, repositories |
| Integration (Testcontainers) | slower | real DB/behavior | SQL, constraints, transactions |
| E2E | slowest | whole-system | critical user journeys (few) |

## E. Interview Q&A

### TEST-01 (P1) — What's the test pyramid and why?
**Strong answer:** Many fast unit tests at the base, fewer integration tests, and a thin layer of end-to-end tests on top. Unit tests give fast feedback and pinpoint failures; integration tests catch wiring/DB issues; E2E tests catch whole-system problems but are slow and flaky, so I keep few. Inverting the pyramid (mostly E2E) gives slow, brittle suites.

### TEST-02 (P1) — Mock vs stub vs spy; when does mocking hide bugs?
**Strong answer:** A stub returns canned values (state-based), a mock verifies interactions (behavior-based), a spy wraps a real object. Mocking hides bugs when I mock the very thing under test or mock away real integration behavior — e.g., mocking the repository means I never exercise the actual SQL/constraints, so a broken query passes. I mock external/slow collaborators but test real DB behavior with integration tests.

<a id="test-03"></a>
### TEST-03 (P1) — `@WebMvcTest` vs `@SpringBootTest`?
**Strong answer:** `@WebMvcTest` loads only the web slice (controllers, converters, validation, advice) with services mocked via `@MockBean` — fast, focused controller tests (serialization, status codes, validation). `@SpringBootTest` boots the full context — for integration and wiring tests, slower. I use the slice for controller behavior and full context (ideally with Testcontainers) for end-to-end persistence/transaction behavior.

### TEST-04 (P1) — Why can `@Transactional` tests hide bugs?
**Strong answer:** They roll back after each test for isolation, so the transaction never commits — flush timing, commit-only triggers, and cross-transaction visibility are never exercised, and a bug that only appears on commit passes. For those cases I disable the rollback or commit explicitly and clean up manually, often with Testcontainers.

### TEST-05 (P1) — How do you test asynchronous code without flakiness?
**Strong answer:** I avoid `Thread.sleep` and instead poll for the expected condition with Awaitility or synchronize with latches/`CompletableFuture`, inject a controllable clock/scheduler, and make the async boundary observable (a callback or a stored result). Flakiness usually comes from timing, shared mutable state, or test-order dependence, which I fix at the root rather than retrying.

### TEST-06 (P2) — Why is high coverage not enough?
**Strong answer:** Coverage measures which lines executed, not whether behavior was asserted — you can run every line with no meaningful assertions and still pass. I care about testing behavior, edge cases, and failure paths, and I treat coverage as a floor/finder of untested code, not a goal.

## F. Production relevance
Good tests are what let a team ship fast without regressions. The failure modes interviewers probe: over-mocked unit tests that pass while production breaks (because the real DB/query was never exercised — Testcontainers fixes this), flaky suites that erode trust and get ignored, transactional tests that hide commit-time bugs, and coverage-chasing with no assertions. The mature stance: a healthy pyramid, real dependencies for integration tests, failure-path tests (ch.05/13/17), and flakiness treated as a bug to root-cause.

## G. Revision

### 10 key takeaways
1. Pyramid: many unit, fewer integration, thin E2E.
2. Stub=state, mock=interaction, spy=partial real.
3. Over-mocking tests your mocks, not your code.
4. `@WebMvcTest` = web slice (services mocked); `@SpringBootTest` = full context.
5. Testcontainers = real DB; more faithful than H2.
6. `@Transactional` tests roll back → can hide commit-time bugs.
7. Test async with Awaitility/latches, not sleep.
8. Flaky tests are bugs (timing/shared state/order) — root-cause them.
9. Coverage ≠ verification; assert behavior and failure paths.
10. Characterization tests before refactoring legacy code.

### 10 rapid-fire
1. Fast layer? (unit) 2. Verify a call? (mock) 3. Canned return? (stub) 4. Web slice annotation? (@WebMvcTest) 5. Full context? (@SpringBootTest) 6. Real DB in tests? (Testcontainers) 7. Tx test default? (rollback) 8. Wait for async? (Awaitility) 9. Coverage guarantees? (execution, not assertions) 10. Before refactor? (characterization tests)

### 5 tricky
1. Why did a 100%-coverage test miss a bug? (no assertions / mocked DB) 2. Why did a passing repo test fail in prod? (H2 vs real dialect) 3. Why did a tx test hide a commit bug? 4. Why is sleep-based async testing flaky? 5. When does mocking defeat the purpose?

### 5 exercises
1. Write a Mockito unit test with a stub and an interaction verification.
2. Write a `@WebMvcTest` asserting a 400 for invalid input.
3. Write a Testcontainers integration test asserting a unique-constraint violation.
4. Convert a sleep-based async test to Awaitility.
5. Add characterization tests around a legacy method, then refactor safely.

### One-page revision sheet
> **Pyramid**: many unit / fewer integration / thin E2E. Doubles: stub(state)/mock(interaction)/spy(partial). **Over-mocking hides bugs** (mock externals, test real DB). **@WebMvcTest**(web slice, @MockBean services) vs **@SpringBootTest**(full). **Testcontainers** = real DB (H2 hides dialects). **@Transactional tests roll back → hide commit-time bugs** (commit explicitly when needed). Async: Awaitility/latches, not sleep. Flaky = bug (timing/shared state/order). Coverage ≠ assertions. Characterization tests before legacy refactor.

### Prerequisites to revisit
- DI/testability (ch.09); MVC (ch.11); JPA/tx behavior to test (ch.12, 13, 17).
