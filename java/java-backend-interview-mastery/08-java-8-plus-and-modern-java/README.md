# 08 — Java 8+ & Modern Java

> **Priority: P1.** Streams, `Optional`, and `CompletableFuture` show up constantly. The signal interviewers look for is *judgment*: knowing when a stream is clearer than a loop, when `Optional` is misused, and when parallel streams are a trap.

**Prerequisites:** [01 Core Java](../01-core-java/), [03 Collections](../03-collections-framework/).
**Related:** [06 Concurrency](../06-java-concurrency/) (CompletableFuture, parallel streams).

---

## A. Concept & intuition
Java 8 added lambdas, functional interfaces, streams, and `Optional`, letting you express *what* to compute declaratively. Later versions added `var`, records, sealed classes, pattern matching, text blocks, and virtual threads. These reduce boilerplate — but used carelessly they reduce clarity and add overhead.

## B. Internals & key concepts

### Lambdas & functional interfaces
A lambda is an instance of a **functional interface** (one abstract method): `Function`, `Predicate`, `Consumer`, `Supplier`, `BiFunction`, etc. Method references (`User::getName`) are shorthand. Lambdas capture **effectively final** locals (not mutable ones) — the reason you can't reassign a captured variable.

### Streams
A pipeline: **source** → **intermediate** ops (lazy: `map`, `filter`, `sorted`) → **terminal** op (eager: `collect`, `reduce`, `forEach`, `count`). Nothing runs until the terminal op; intermediate ops are fused and lazy. Streams are single-use.
- **`map` vs `flatMap`:** `map` is 1→1; `flatMap` flattens 1→many (a stream of streams into one stream) — e.g., `orders.stream().flatMap(o -> o.getItems().stream())`.
- **`reduce` vs `collect`:** `reduce` folds into a single immutable result with an associative operator; `collect` accumulates into a mutable container (`toList`, `groupingBy`, `joining`) — prefer `collect` for building collections.

### Optional
A container that may hold a value, designed as a **return type** to make "no result" explicit and avoid NPEs. **Misuse:** as a field, a method parameter, or in collections; calling `get()` without checking; using it where a plain null-check or default is clearer.

### CompletableFuture
Async composition (`thenApply`, `thenCompose`, `thenCombine`, `exceptionally`) — covered in depth in [06 §11](../06-java-concurrency/). Key reminders: pass your own executor, handle exceptions, don't block a request thread.

### Date/time (java.time)
`Instant`, `LocalDate`, `LocalDateTime`, `ZonedDateTime`, `Duration`, `Period` — immutable and thread-safe, replacing the broken `Date`/`Calendar`. Store instants in UTC; apply zones at the edges.

### Records, sealed, pattern matching
See [01 §7](../01-core-java/). Pattern matching for `switch` + sealed types enables exhaustive, safe modeling.

## C. Code — streams done well and badly
```java
// Clear and declarative
Map<Department, List<Employee>> byDept =
    employees.stream().collect(Collectors.groupingBy(Employee::department));

double avgSalary =
    employees.stream().mapToDouble(Employee::salary).average().orElse(0);
```
```java
// BROKEN: side effects + stateful lambda in a stream (and worse if parallel)
List<String> out = new ArrayList<>();
items.parallelStream().forEach(out::add); // data race on a non-thread-safe list; order lost
// FIX: collect, don't mutate external state
List<String> out = items.stream().map(Item::name).collect(Collectors.toList());
```
```java
// Optional misuse vs good use
String nameBad = Optional.ofNullable(user).get().getName(); // get() without check -> defeats the point
String nameGood = Optional.ofNullable(user).map(User::getName).orElse("anonymous");
```
```java
// When a loop is clearer than a stream: early exit with side effects / index logic
for (int i = 0; i < rows.size(); i++) {
    if (rows.get(i).isTerminator()) { process(i); break; } // index + break: awkward as a stream
}
```

## D. Trade-offs

| Choice | Pros | Cons | Use when | Poor choice when |
|---|---|---|---|---|
| Stream | declarative, composable | overhead, debugging, stack traces | transforms/aggregations | tight hot loops, index/early-exit logic |
| Loop | explicit, debuggable, fast | verbose | index, early exit, side effects | multi-stage data transforms |
| Optional (return) | explicit absence | overhead if overused | "might not exist" results | fields, params, collections |
| Parallel stream | uses cores | shared common pool, needs associativity/thread-safety | large CPU-bound, independent, associative | IO-bound, ordered, shared state |

## E. Interview Q&A

### MJ-01 (P1) — Intermediate vs terminal operations; what is laziness?
**Strong answer:** Intermediate ops (`map`, `filter`) are lazy and return a new stream; nothing executes until a terminal op (`collect`, `count`, `forEach`) runs, at which point the pipeline is fused into as few passes as possible. Laziness enables short-circuiting (`findFirst`, `limit`) to avoid processing the whole source. Streams are single-use.

### MJ-02 (P1) — reduce vs collect?
**Strong answer:** `reduce` combines elements into one immutable result with an associative, stateless accumulator — great for sums/mins. `collect` uses a mutable container with a supplier/accumulator/combiner (via `Collector`s like `toList`, `groupingBy`, `joining`) — the right tool for building collections or maps, and the only safe one for mutable accumulation in parallel.

<a id="mj-03"></a>
### MJ-03 (P1) — map vs flatMap?
**Strong answer:** `map` transforms each element 1→1. `flatMap` maps each element to a *stream* and flattens all of them into one — I use it to flatten nested structures, like turning a stream of orders into a stream of all their line items, or to drop empty `Optional`s. Using `map` where you need `flatMap` leaves you with a `Stream<Stream<T>>` or `Stream<Optional<T>>`.

### MJ-04 (P1) — When is a loop better than a stream?
**Strong answer:** When there's index arithmetic, early exit with `break`, mutation of external state, or when the stream version is harder to read or debug (stream stack traces are noisy). Streams shine for multi-stage transformations and aggregations; loops are clearer for imperative control flow and are often faster on hot paths. I choose for readability first, performance where measured.

<a id="mj-05"></a>
### MJ-05 (P1) — How is `Optional` commonly misused?
**Strong answer:** Using it as a field or method parameter (it adds indirection and still allows null itself), putting it in collections, and calling `get()` without `isPresent()` (which just moves the NPE). It's designed as a return type to force callers to handle absence; I use `map`/`filter`/`orElse`/`orElseThrow` and never `Optional.get()` unchecked.

### MJ-06 (P1) — Why are side effects in streams dangerous?
**Strong answer:** Streams assume stateless, non-interfering operations. Mutating external state from a `map`/`forEach` breaks that, is order-dependent, and becomes a data race in parallel streams. I accumulate with `collect` instead of mutating a shared list.

<a id="mj-07"></a>
### MJ-07 (P1) — Risks of parallel streams?
**Strong answer:** They use the shared `ForkJoinPool.commonPool`, so a blocking or long task starves every other parallel stream in the JVM; they require associative, stateless operations and thread-safe accumulation (use `collect`); and they only help for large, CPU-bound, independent work with enough elements to offset the splitting overhead. For IO-bound work they're the wrong tool — use an executor/`CompletableFuture` or virtual threads. I rarely use them in request-handling code because of the shared pool.

### MJ-08 (P2) — What problems does `java.time` fix?
**Strong answer:** `Date`/`Calendar` were mutable, not thread-safe, and had confusing APIs (0-based months). `java.time` types are immutable, thread-safe, and clear; I store `Instant` in UTC and convert to a `ZonedDateTime` only at display boundaries to avoid timezone bugs.

## F. Production relevance
Modern Java tightens correctness and readability: `java.time` eliminates a whole category of timezone/mutability bugs; `Optional` return types document absence; records cut DTO boilerplate and give correct equals/hashCode. The traps that hit production: parallel streams starving the common pool and causing latency spikes, side-effecting streams causing races, and over-clever stream pipelines that are hard to debug during an incident. Favor clarity; reserve parallel streams for measured CPU-bound work.

## G. Revision

### 10 key takeaways
1. Lambda = functional interface instance; captures effectively-final locals.
2. Streams: lazy intermediate ops, eager terminal, single-use, fused.
3. `map` 1→1; `flatMap` flattens 1→many.
4. `collect` for building collections; `reduce` for folding.
5. `Optional` is a return type; don't use it as field/param or call `get()` blindly.
6. No side effects in streams; use `collect`.
7. Parallel streams share the common pool — avoid for IO/blocking.
8. Loops win for index/early-exit/debuggability.
9. `java.time` is immutable; store UTC, zone at edges.
10. Records/sealed/pattern-matching model data safely.

### 10 rapid-fire
1. Terminal or intermediate: `filter`? (intermediate) 2. Build a map by key? (`groupingBy`) 3. Flatten nested lists? (`flatMap`) 4. Optional default? (`orElse`) 5. Pool used by parallel streams? (common ForkJoinPool) 6. Stream reusable? (no) 7. Thread-safe date type? (java.time) 8. Join strings? (`Collectors.joining`) 9. Early-exit terminal? (`findFirst`) 10. Mutating a list in `forEach`? (avoid)

### 5 tricky
1. Why did a parallel stream slow the whole service? (common pool starvation) 2. Why `Stream<Optional<T>>` after `map`? (needed flatMap) 3. Why is `Optional` as a field a smell? 4. Why did a stream give wrong results in parallel? (non-associative / shared state) 5. Why is a loop sometimes clearer? (index/break)

### 5 exercises
1. Rewrite a nested loop as `flatMap` + `collect`, then argue which reads better.
2. Group employees by department and compute average salary per group.
3. Refactor null-returning code to `Optional` returns used with `map`/`orElseThrow`.
4. Show a side-effecting parallel stream race; fix with `collect`.
5. Replace `Date` arithmetic with `java.time` and handle a timezone correctly.

### One-page revision sheet
> Lambda=functional interface; captures effectively-final. Streams: **lazy intermediate** (map/filter/sorted) → **eager terminal** (collect/reduce/count); single-use; short-circuit via findFirst/limit. **map(1→1), flatMap(flatten)**; **collect(build), reduce(fold)**. Optional=**return type**; use map/orElse/orElseThrow, never blind get()/field/param. **No side effects**; collect don't mutate. **Parallel streams = shared common pool** → avoid for IO/blocking, need associativity+thread-safety. Loops for index/early-exit/debug. java.time immutable; store UTC. Records/sealed/pattern matching.

### Prerequisites to revisit
- equals/hashCode & immutability (ch.01); CompletableFuture & pools (ch.06).
