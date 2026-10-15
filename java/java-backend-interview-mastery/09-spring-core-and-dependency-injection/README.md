# 09 — Spring Core & Dependency Injection

> **Priority: P0.** DI is the heart of Spring. Interviewers want you to explain *how* the container wires beans, why constructor injection is preferred, how proxies enable `@Transactional`/`@Async`/caching, and how circular dependencies arise — because these explain the behavior of every other Spring feature.

**Prerequisites:** [01 Core Java](../01-core-java/).
**Related:** [10 Boot internals](../10-spring-boot-internals/), [13 Transactions](../13-spring-transactions/) (proxies), [11 MVC](../11-spring-mvc-and-rest-apis/).

---

## A. Concept & intuition
**Inversion of Control (IoC):** instead of objects creating their own dependencies, a container creates and supplies them. **Dependency Injection** is the mechanism. The problem it solves: hard-wired `new` calls create tight coupling, make testing hard (can't substitute a fake), and scatter object-graph construction across the codebase. With DI, you declare *what* you need; the container decides *how* to provide it, so wiring is centralized and swappable.

**What breaks without it:** untestable classes that `new` their collaborators, duplicated construction logic, and no single place to manage lifecycles/scopes/cross-cutting concerns.

## B. Internal working — how the container wires beans
1. **Metadata collection:** component scanning finds `@Component`/`@Service`/`@Repository`/`@Controller` classes; `@Configuration` classes contribute `@Bean` methods. Each becomes a **BeanDefinition** (class, scope, dependencies, init/destroy callbacks).
2. **BeanFactoryPostProcessors** run (can modify definitions, e.g., property placeholders).
3. For each singleton, the container **instantiates** it, **resolves and injects** dependencies (by type, disambiguated by `@Qualifier`/`@Primary`/name), then runs **BeanPostProcessors** (which is where proxies get created — AOP, `@Transactional`), then `@PostConstruct`/`InitializingBean`.
4. On shutdown, `@PreDestroy`/`DisposableBean` run.

`ApplicationContext` is the full-featured container (events, i18n, AOP, auto post-processing) built on top of `BeanFactory` (bare lazy container). You almost always use `ApplicationContext`.

### Proxies (why this matters everywhere)
Cross-cutting features (`@Transactional`, `@Async`, `@Cacheable`, security) work by wrapping a bean in a **proxy** (JDK dynamic proxy if it implements an interface, else CGLIB subclass). Callers hold the proxy; the proxy runs advice then delegates. This is exactly why **self-invocation bypasses them** ([13 TX-02](../13-spring-transactions/)) — an internal `this.method()` call doesn't go through the proxy.

## C. Code — injection styles & circular deps
```java
// PREFERRED: constructor injection — fields can be final, dependencies explicit, testable
@Service
class OrderService {
    private final PaymentClient payments;
    private final OrderRepository repo;
    OrderService(PaymentClient payments, OrderRepository repo) { // @Autowired optional on sole ctor
        this.payments = payments; this.repo = repo;
    }
}
```
```java
// DISCOURAGED: field injection — can't be final, hidden deps, hard to unit test without reflection
@Service
class OrderService {
    @Autowired private PaymentClient payments; // null until container injects; invisible in constructor
}
```
```java
// BROKEN: constructor circular dependency A->B->A fails fast at startup
@Service class A { A(B b){} }
@Service class B { B(A a){} }   // BeanCurrentlyInCreationException
// FIX: break the cycle (extract a third collaborator), or @Lazy one side, or (rarely) setter injection.
// Best fix is usually a design change — a cycle signals muddled responsibilities.
```
Observable behavior: with constructor injection a cycle fails at startup (good — fail fast). With field/setter injection Spring can sometimes paper over it, hiding a design smell.

## D. Trade-offs

| Injection | Pros | Cons | Use when |
|---|---|---|---|
| Constructor | final fields, explicit deps, testable, fails fast on cycles | verbose with many deps (a smell itself) | **default, always** |
| Setter | optional deps, re-injectable | mutable, deps hidden | genuinely optional dependency |
| Field | concise | not final, untestable without reflection, hides deps | avoid (OK only in quick tests) |

| Scope | Lifetime | Use when | Gotcha |
|---|---|---|---|
| singleton (default) | one per container | stateless services | never store request state |
| prototype | new each injection | stateful helpers | container won't call destroy |
| request/session | web-bound | per-request state | needs proxy to inject into singletons |

## E. Interview Q&A

<a id="sc-01"></a>
### SC-01 (P0) — How does Spring DI work internally?
**Strong answer:** At startup Spring builds BeanDefinitions from component scanning and `@Bean` methods, runs BeanFactoryPostProcessors, then for each bean instantiates it, injects dependencies resolved by type (disambiguated by `@Qualifier`/`@Primary`), applies BeanPostProcessors — which is where it wraps beans in proxies for AOP/transactions — and runs lifecycle callbacks. The ApplicationContext holds the singletons and hands out the (possibly proxied) references. So "injection" is the container resolving and setting collaborators according to the definitions.
**Weak answer:** "Spring creates objects for you." True but misses proxies and lifecycle, which are what later questions hinge on.

### SC-02 (P1) — ApplicationContext vs BeanFactory?
**Strong answer:** BeanFactory is the basic lazy container; ApplicationContext adds eager singleton creation, event publishing, i18n, resource loading, and automatic BeanPostProcessor registration (so AOP/annotations work). In practice you always use ApplicationContext.

### SC-03 (P1) — `@Component` vs `@Bean`; the stereotype annotations?
**Strong answer:** `@Component` (and its specializations `@Service`/`@Repository`/`@Controller`) marks a class for component scanning to auto-register. `@Bean` is a factory method in a `@Configuration` class — used when I need to construct the bean myself (third-party classes I can't annotate, conditional creation, custom wiring). `@Repository` also enables persistence exception translation; `@Controller`/`@RestController` mark web handlers.

<a id="sc-04"></a>
### SC-04 (P0) — Constructor vs field vs setter injection — which and why?
**Strong answer:** Constructor injection is the default: it lets fields be `final` (immutable, thread-safe), makes dependencies explicit in the signature (so the class is honest about what it needs), is trivially unit-testable with plain `new`, and fails fast on circular dependencies. Field injection hides dependencies, prevents `final`, and needs reflection to test. Setter injection is for genuinely optional dependencies. A constructor with too many parameters is itself a signal the class does too much.

### SC-05 (P1) — Is Spring's singleton the same as the singleton pattern?
**Strong answer:** No. Spring singleton means one instance **per container** (per bean name), managed by Spring; the GoF singleton is one instance **per classloader/JVM** enforced by the class itself (often a static). Spring singletons are easier to test and swap. Because they're shared across request threads, they must be stateless ([06 §13](../06-java-concurrency/)).

### SC-06 (P1) — How do circular dependencies happen and how do you fix them?
**Strong answer:** Two beans needing each other via constructor injection can't both be built first, so Spring throws at startup — which is good, it surfaces a design problem. Fixes: extract the shared logic into a third bean, use `@Lazy` on one side so a proxy is injected and the real bean resolved later, or (rarely) use setter injection. I prefer the design fix because a cycle usually means responsibilities are tangled.

### SC-07 (P2) — What runs in the bean lifecycle, in order?
**Strong answer:** Instantiate → populate/inject → `BeanNameAware`/`*Aware` → BeanPostProcessor `postProcessBeforeInitialization` → `@PostConstruct`/`afterPropertiesSet`/custom init → BeanPostProcessor `postProcessAfterInitialization` (proxy creation) → bean in use → on shutdown `@PreDestroy`/`destroy`.

## F. Production relevance
DI decisions shape testability and reliability: constructor injection makes services unit-testable without the Spring context (fast tests); understanding proxies explains why `@Transactional`/`@Async`/`@Cacheable` silently do nothing on self-calls (a recurring production bug); stateless singletons avoid concurrency bugs; fail-fast startup on cycles/missing beans catches misconfiguration before traffic. Being able to narrate the bean lifecycle lets you debug "which bean got wired and why" ([10 SB-02](../10-spring-boot-internals/)).

## G. Revision

### 10 key takeaways
1. IoC = container owns object creation; DI is the mechanism.
2. BeanDefinitions → instantiate → inject → BeanPostProcessors (proxies) → lifecycle.
3. Constructor injection is the default (final, explicit, testable, fail-fast).
4. Proxies implement `@Transactional`/`@Async`/`@Cacheable`; self-invocation bypasses them.
5. ApplicationContext ⊃ BeanFactory.
6. `@Component` scans; `@Bean` constructs manually.
7. Spring singleton ≠ GoF singleton; it's per-container and must be stateless.
8. Circular constructor deps fail fast — fix the design.
9. Scopes: singleton/prototype/request/session; don't store request state in singletons.
10. `@PostConstruct`/`@PreDestroy` for lifecycle hooks.

### 10 rapid-fire
1. Default scope? (singleton) 2. Preferred injection? (constructor) 3. What creates proxies? (BeanPostProcessors) 4. `@Bean` lives in? (`@Configuration`) 5. Disambiguate beans? (`@Qualifier`/`@Primary`) 6. Cycle with ctor injection? (fails at startup) 7. Full container type? (ApplicationContext) 8. `@Repository` extra feature? (exception translation) 9. Make a singleton safe? (stateless) 10. Lazy init a bean? (`@Lazy`)

### 5 tricky
1. Why does `@Transactional` self-call do nothing? (proxy bypass) 2. Why prefer constructor injection for testing? (plain `new`, no reflection) 3. Why is a 10-arg constructor a smell? (class does too much) 4. Spring vs GoF singleton? 5. Why can field injection hide a cycle that constructor injection exposes?

### 5 exercises
1. Convert a field-injected service to constructor injection; unit-test it with plain `new`.
2. Create a circular dependency and resolve it three ways.
3. Add a `BeanPostProcessor` that logs each bean name as it initializes.
4. Define a bean with `@Bean` for a third-party class and inject it.
5. Show `@Transactional` failing on self-invocation, then fix it (ties to ch.13).

### One-page revision sheet
> IoC+DI: container builds BeanDefinitions (scan + @Bean) → instantiate → inject by type (@Qualifier/@Primary) → BeanPostProcessors create **proxies** (@Transactional/@Async/@Cacheable) → @PostConstruct. **Constructor injection default** (final/explicit/testable/fail-fast on cycles). ApplicationContext⊃BeanFactory. Spring singleton=per-container→**stateless**. Self-invocation bypasses proxies. Cycle=design smell; @Lazy/extract/setter to break. Scopes: singleton/prototype/request/session.

### Prerequisites to revisit
- Object references & immutability (ch.01); proxies feed into transactions (ch.13) and MVC (ch.11).
