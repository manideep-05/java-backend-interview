# 01 — Core Java & Object Semantics

> **Priority: P0.** This is the floor everything else stands on. If you can't reason precisely about references, equality, immutability, and the String pool, every later topic (collections, concurrency, JPA entities) gets shaky. Interviewers use these to calibrate quickly.

**Prerequisites:** basic Java syntax.
**Related:** [03 Collections](../03-collections-framework/) (equals/hashCode in maps), [06 Concurrency](../06-java-concurrency/) (immutability & safe publication), [12 JPA](../12-spring-data-jpa-and-hibernate/) (entity equals/hashCode).

---

## Table of contents
1. [JDK vs JRE vs JVM; compilation & bytecode](#1-platform)
2. [Primitives, wrappers, autoboxing & the Integer cache](#2-primitives)
3. [Pass-by-value & references; stack vs heap](#3-pass-by-value)
4. [Object identity vs equality; equals/hashCode](#4-equality)
5. [Immutability & defensive copying; `final`](#5-immutability)
6. [String immutability, the String pool, String vs StringBuilder](#6-strings)
7. [Records, enums, sealed classes](#7-modern)
8. [Trade-offs](#d-trade-offs)
9. [Interview Q&A](#e-interview-qa)
10. [Production relevance](#f-production-relevance)
11. [Revision](#g-revision)

---

## 1. Platform

### A. Concept & intuition
- **JDK** (Development Kit) = JRE + tools (`javac`, `jar`, `jlink`, debuggers).
- **JRE** (Runtime Environment) = JVM + standard libraries needed to *run* bytecode. (Since Java 11 there's no standalone JRE download; you ship a runtime via `jlink`.)
- **JVM** = the abstract machine that loads and executes `.class` bytecode. It provides portability ("write once, run anywhere"): `javac` compiles `.java` → platform-neutral bytecode; the JVM for each OS/CPU interprets and JIT-compiles it to native code.

### B. Execution path
`.java` → `javac` → `.class` (bytecode) → classloader loads it → bytecode verifier → interpreter runs it; the **JIT** compiles hot methods to native code; the GC manages heap memory. (Deeper JIT/GC coverage in [07 JVM](../07-jvm-memory-and-performance/).) The key language guarantee here: bytecode is portable; performance characteristics (JIT, GC) are implementation details of the JVM you run.

---

## 2. Primitives

### A. Concept
Primitives (`int`, `long`, `double`, `boolean`, …) hold values directly; wrappers (`Integer`, `Long`, …) are objects on the heap. **Autoboxing** converts primitive → wrapper automatically; **unboxing** the reverse.

### The Integer cache `==` trap (CJ-01)
```java
Integer a = 127, b = 127;
Integer c = 128, d = 128;
System.out.println(a == b); // true  — cached
System.out.println(c == d); // false — different objects
System.out.println(c.equals(d)); // true — value equality
```
Why: autoboxing uses `Integer.valueOf`, which **caches** instances for −128..127 (configurable upper bound). Within the cache range, `==` compares the same cached reference (true); outside, two separate objects (false). The lesson: **never compare wrappers with `==`**; use `equals` (or compare primitives). This is a *framework/implementation* behavior (`valueOf` caching), not a reason `==` ever means value-equality for objects.

**Other autoboxing hazards:**
- Unboxing a `null` wrapper throws `NullPointerException` (`int x = someIntegerThatIsNull;`).
- Autoboxing in tight loops creates garbage (`Long sum` vs `long sum`) — a real perf/GC issue.

---

## 3. Pass-by-value

### A. Concept — Java is always pass-by-value
Java passes **copies**. For primitives, a copy of the value. For objects, a copy of the **reference** (the pointer value), not the object. So:
```java
void mutate(List<String> list) { list.add("x"); }  // affects caller's list (same object)
void reassign(List<String> list) { list = new ArrayList<>(); } // does NOT affect caller
```
`mutate` changes the shared object through the copied reference. `reassign` only rebinds the local copy — the caller's variable still points to the original. Saying "Java passes objects by reference" is **wrong** and interviewers catch it; the precise statement is "pass-by-value, where the value for an object is its reference."

### Stack vs heap
Local variables and references live on the **stack** (per-thread frame); objects live on the **heap** (shared). This is why two threads sharing an object is dangerous ([06](../06-java-concurrency/)) but their local variables are private.

---

## 4. Equality

### Identity vs logical equality
- `==` compares **references** (identity) for objects, values for primitives.
- `equals()` compares **logical equality** as the class defines it. Default `Object.equals` is identity, so you must override it for value types.

### CJ-04 — the equals/hashCode contract
If you override `equals`, you **must** override `hashCode` consistently:
1. **Reflexive, symmetric, transitive, consistent.**
2. **If `a.equals(b)` then `a.hashCode() == b.hashCode()`.** (The reverse is NOT required — unequal objects *may* collide.)

Violating #2 breaks every hash-based collection: you can put an entry in a `HashMap` and never get it back (different bucket), or get duplicates in a `HashSet`. (See [03 §equals](../03-collections-framework/).)

```java
// Correct value type
final class Point {
    private final int x, y;
    Point(int x, int y){ this.x = x; this.y = y; }
    @Override public boolean equals(Object o){
        return o instanceof Point p && p.x == x && p.y == y; // pattern matching (Java 16+)
    }
    @Override public int hashCode(){ return java.util.Objects.hash(x, y); }
    @Override public String toString(){ return "Point(" + x + "," + y + ")"; }
}
```
Use `java.util.Objects.equals`/`hash` to handle nulls and reduce boilerplate; or use a `record` (§7) which generates all three correctly.

---

## 5. Immutability

### A. Concept & why it matters
An immutable object's observable state can't change after construction. Benefits: inherently **thread-safe** (safe to share without synchronization — [06 safe publication](../06-java-concurrency/)), safe as a `Map`/`Set` key, no defensive copying on reads, easier to reason about.

### Building a correct immutable type (CJ-05)
```java
// BROKEN: leaks internal mutable state
final class Period {
    private final java.util.Date start, end;
    Period(java.util.Date s, java.util.Date e){ this.start = s; this.end = e; } // stores caller's refs
    java.util.Date getStart(){ return start; } // returns internal ref -> caller can mutate it
}
```
A caller can mutate the `Date` they passed in or the one they got out, breaking immutability.

```java
// FIXED: defensive copies in (constructor) and out (getters); prefer immutable types
final class Period {
    private final java.time.Instant start, end;   // java.time types are immutable -> no copies needed
    Period(java.time.Instant s, java.time.Instant e){
        if (s.isAfter(e)) throw new IllegalArgumentException("start after end");
        this.start = s; this.end = e;
    }
    java.time.Instant getStart(){ return start; }
    java.time.Instant getEnd(){ return end; }
}
```
Recipe: `final` class (or no accessible subclass), all fields `private final`, no setters, defensive-copy mutable inputs/outputs (or use immutable field types like `java.time`, `String`, `List.copyOf`). The modern move is to use immutable field types so copies are unnecessary.

### `final` nuances
- `final` variable: can't be reassigned (the *reference* is fixed; the object may still be mutable).
- `final` method: can't be overridden. `final` class: can't be subclassed.
- `final` fields get a JMM guarantee for safe publication ([06 §3](../06-java-concurrency/)).

---

## 6. Strings

### CJ-07 — why String is immutable, and the pool
`String` is immutable: its backing data never changes after construction. Reasons: **security** (paths, URLs, class names can't be mutated after validation), **thread-safety** (freely shared), **hashcode caching** (computed once — fast as map keys), and the **String pool** (interned literals are shared to save memory). The pool is a JVM-managed table of interned strings; string **literals** are automatically interned, so `"a" == "a"` is true, but `new String("a") == "a"` is false (the `new` forces a distinct heap object). `str.intern()` returns the pooled instance.

```java
String a = "hello";
String b = "hello";
String c = new String("hello");
System.out.println(a == b);        // true  (both the pooled literal)
System.out.println(a == c);        // false (c is a new object)
System.out.println(a == c.intern()); // true  (intern returns the pooled one)
System.out.println(a.equals(c));   // true  (always compare with equals)
```

### CJ-11 — String vs StringBuilder vs StringBuffer
- `String` concatenation in a **loop** creates a new object each iteration (O(n²) garbage) — a real hot-path bug.
- `StringBuilder`: mutable, **not** thread-safe, the right default for building strings.
- `StringBuffer`: like `StringBuilder` but every method is `synchronized` — legacy; the synchronization is almost never useful (you rarely share a builder across threads) and adds overhead.
```java
// BROKEN on a hot path: quadratic allocation
String s = "";
for (String part : parts) s += part;   // new String each iteration
// FIXED:
StringBuilder sb = new StringBuilder();
for (String part : parts) sb.append(part);
String s = sb.toString();
```
(Note: a *single* `a + b + c` expression is fine — the compiler optimizes it; the problem is concatenation inside loops.)

---

## 7. Modern

- **`record`** (Java 16+): a transparent carrier for immutable data; auto-generates constructor, accessors, `equals`/`hashCode`/`toString`. Ideal for DTOs and value types. You can add validation in a compact constructor.
```java
record Money(long amountMinor, String currency) {
    Money { if (amountMinor < 0) throw new IllegalArgumentException("negative"); } // compact ctor validation
}
```
- **`enum`**: a fixed set of singletons; can have fields, methods, and per-constant bodies. The canonical **singleton** implementation and great for state machines/strategy.
- **sealed classes** (Java 17+): restrict which types can extend/implement (`sealed … permits …`), enabling exhaustive `switch` pattern matching and modeling closed hierarchies (e.g., a result type). A *language* feature for making illegal states unrepresentable.

---

## D. Trade-offs

| Choice | Pros | Cons | Use when | Poor choice when |
|---|---|---|---|---|
| primitive | no allocation, no null, fast | no generics/collections directly | arithmetic, hot loops | need nullability/collection element |
| wrapper | nullable, usable as generic/collection element | boxing cost, `==` trap, NPE on unbox | map/list elements, optional numbers | tight numeric loops |
| mutable class | flexible | unsafe to share, defensive copies | local scratch state | shared/map-key/returned data |
| immutable class/record | thread-safe, safe key, simple | new object per change | value types, DTOs, keys | huge objects changed often |
| String | safe, pooled | immutable → concat cost | identifiers, keys, literals | building large strings in loops |
| StringBuilder | fast building | not thread-safe | assembling strings | sharing across threads |

---

## E. Interview Q&A

<a id="cj-01"></a>
### CJ-01 (P0) — `==` vs `equals`, and the Integer cache trap.
**Strong answer:** For objects, `==` compares references (identity); `equals` compares logical equality as the class defines it (default is identity, so value types must override it). The Integer trap: autoboxing calls `Integer.valueOf`, which caches −128..127, so `Integer a=127,b=127; a==b` is true but `128==128` boxed is false — same cached reference vs two objects. So I never use `==` on wrappers; I use `equals` or compare primitives. The caching is an implementation detail of `valueOf`, not a change to what `==` means.
**Weak answer:** "`==` compares values." False for objects; this is exactly the trap.

### CJ-02 (P1) — Is Java pass-by-value or pass-by-reference?
**Strong answer:** Always pass-by-value. For an object, the value copied is the *reference*, so a method can mutate the shared object through it, but reassigning the parameter doesn't affect the caller's variable. "Pass by reference" would mean the caller's variable itself could be rebound, which Java never does.

### CJ-03 (P1) — Stack vs heap; what lives where?
**Strong answer:** Each thread has a stack of frames holding local variables and references; objects live on the shared heap. Primitives-as-locals and reference *variables* are on the stack; the objects they point to are on the heap. This is why locals are thread-private but shared objects need synchronization.

<a id="cj-04"></a>
### CJ-04 (P0) — State the equals/hashCode contract and what breaks if you violate it.
**Strong answer:** `equals` must be reflexive, symmetric, transitive, consistent, and `x.equals(null)` false; and crucially, equal objects must have equal hashCodes (unequal objects may share a hashCode). If I override `equals` but not `hashCode`, hash collections break: I can `put` into a `HashMap` and `get` returns null because the key hashes to a different bucket, and `HashSet` admits logical duplicates. For entities with generated ids this is especially subtle (ch.12).

### CJ-05 (P1) — How do you make a class immutable? Why bother?
**Strong answer:** `final` class, `private final` fields, no setters, defensive-copy mutable inputs and outputs (or use immutable field types like `java.time`/`String`/`List.copyOf`), validate in the constructor. Immutability gives thread-safety without locks, safe use as map/set keys, and simpler reasoning — I'd reach for a `record` when it's a pure data carrier.

### CJ-06 (P1) — What does `final` guarantee, and what does it not?
**Strong answer:** `final` fixes a variable's binding (can't reassign), prevents method override, or prevents subclassing. It does **not** make the referenced object immutable — a `final List` can still be mutated. `final` fields also carry a JMM safe-publication guarantee.

<a id="cj-07"></a>
### CJ-07 (P0) — Why is String immutable, and how does the pool work?
**Strong answer:** Immutability gives security (validated strings can't be mutated later), thread-safety (free sharing), hashcode caching (fast map keys), and enables the string pool, which interns literals so identical literals share one object and save memory. Literals are auto-interned, so `"a"=="a"` is true, but `new String("a")` creates a distinct object; `intern()` returns the pooled instance. I always compare strings with `equals`, never `==`.

### CJ-08 (P2) — What's the difference between `String s = "x"` and `new String("x")`?
**Strong answer:** The literal is the pooled instance; `new String("x")` forces a separate heap object with equal content but different identity. `new String` is almost always a mistake unless I deliberately want a distinct instance.

<a id="cj-09"></a>
### CJ-09 (P0) — What happens if you use a mutable object as a HashMap key and then mutate it?
**Strong answer:** The entry is placed in the bucket for the key's hash at insert time. If I mutate fields that affect `hashCode`/`equals`, lookups compute a different bucket and can't find the entry — it's effectively lost and leaks (even `remove(key)` may miss it). That's why keys should be effectively immutable; I use immutable types or records as keys. (Cross-ref COL-07.)

### CJ-10 (P1) — When would you use an enum over constants?
**Strong answer:** Enums give type safety (can't pass an arbitrary int), a fixed set, and can carry behavior/fields and per-constant logic; they're also the cleanest singleton. I prefer them over `int`/`String` constants for closed sets like statuses, and use `switch` exhaustiveness for safety.

<a id="cj-11"></a>
### CJ-11 (P1) — String vs StringBuilder vs StringBuffer?
**Strong answer:** `String` is immutable, so concatenating in a loop is quadratic allocation — a hot-path bug. `StringBuilder` is the mutable, non-thread-safe default for building strings; `StringBuffer` is its synchronized legacy twin whose locking is almost never needed and adds overhead. A single `a+b+c` expression is fine because the compiler optimizes it; the loop case is the problem.

### CJ-12 (P2) — What are records and when do they not fit?
**Strong answer:** Records are transparent immutable data carriers with generated accessors/equals/hashCode/toString — perfect for DTOs/value objects. They don't fit when you need mutability, inheritance of state, or encapsulation that hides the components, since records expose all components by design.

---

## F. Production relevance
These fundamentals quietly cause real bugs: wrapper `==` comparisons that pass tests at small values and fail at scale; broken entity `equals`/`hashCode` dropping set/map entries and corrupting JPA collections (ch.12); mutable keys leaking memory; quadratic string building spiking CPU on large payloads; and shared mutable objects causing the concurrency bugs in ch.06. Immutability and correct equality are the cheapest defenses you have — they remove whole classes of concurrency and collection bugs before they start.

---

## G. Revision

### 10 key takeaways
1. JDK⊃JRE⊃JVM; bytecode is portable, JIT/GC are JVM impl details.
2. Never compare wrappers with `==` (Integer cache −128..127).
3. Java is pass-by-value; for objects the value is the reference.
4. `==` = identity, `equals` = logical equality.
5. equals⇒equal hashCode (not vice versa); violate it and hash collections break.
6. Immutable = thread-safe, safe key; use `final` + defensive copies or immutable field types.
7. `final` fixes bindings, not object mutability.
8. String is immutable → pooled literals; compare with `equals`.
9. Build strings with `StringBuilder`, not `+` in loops.
10. Prefer records for DTOs, enums for closed sets, sealed for closed hierarchies.

### 10 rapid-fire
1. `Integer 127==127`? (true) 2. `Integer 128==128`? (false) 3. Java param passing? (by value) 4. Does equals require hashCode override? (yes if you override equals) 5. `new String("a")=="a"`? (false) 6. Is a `final List` immutable? (no) 7. Thread-safe string builder? (StringBuffer, rarely needed) 8. Best DTO construct? (record) 9. Singleton via? (enum) 10. Compare strings with? (equals)

### 5 tricky
1. Why does a wrapper comparison pass locally but fail in prod? (values crossed the cache boundary)
2. Why did a HashMap "lose" a key? (mutated key / bad hashCode)
3. Why is `s += x` in a loop slow? (quadratic allocation)
4. Why is `new String` almost always wrong? (defeats pooling, extra object)
5. What does `final` NOT guarantee? (deep immutability)

### 5 exercises
1. Demonstrate the Integer cache boundary with 127 vs 128.
2. Write a correct immutable `Period` with validation and no leaks.
3. Break a HashMap with a key missing `hashCode`; fix and verify `get`.
4. Benchmark loop `+` vs `StringBuilder` for 100k parts (observe, don't guess numbers).
5. Convert a mutable DTO class to a `record` and show equals/hashCode come for free.

### One-page revision sheet
> JDK⊃JRE⊃JVM; `javac`→bytecode→JVM(interpret+JIT). **Wrappers: never `==`** (cache −128..127); unboxing null→NPE. **Pass-by-value** (object value = reference): mutate-through yes, reassign no. `==`=identity, `equals`=logical. **equals⇒equal hashCode**; break it → lost map/set entries. Immutable: final class + private final + defensive copies / immutable field types; thread-safe, safe key. `final`≠deep immutable. **String immutable + pool**; literals interned; `new String` distinct; compare with equals. Loop concat → `StringBuilder`. Records=DTO/value; enum=closed set/singleton; sealed=closed hierarchy.

### Prerequisites to revisit
- None (this is the base). Next: equals/hashCode in collections (ch.03) and immutability for concurrency (ch.06).
