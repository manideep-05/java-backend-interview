# 04 — Generics & Type System

> **Priority: P1.** PECS, type erasure, and invariance separate people who *use* generics from people who *read* generic signatures and understand their runtime limits.

**Prerequisites:** [01 Core Java](../01-core-java/), [03 Collections](../03-collections-framework/).

---

## A. Concept & intuition
Generics add **compile-time type safety** and remove casts: `List<String>` guarantees only strings go in and come out, caught by the compiler. Before generics (Java 1.4), collections held `Object` and you cast on every read — unsafe and verbose. Generics moved those errors from runtime `ClassCastException` to compile time.

## B. Internals

### Type erasure
Generics are a **compile-time** feature. The compiler checks types, then **erases** them: `List<String>` and `List<Integer>` are both just `List` at runtime (erased to the bound, or `Object` if unbounded). Consequences:
- No `new T[]`, no `T.class`, no `instanceof List<String>` — the type isn't available at runtime (not *reified*).
- Overloads differing only by type parameter don't compile (same erasure).
- The compiler inserts **bridge methods** and casts to preserve polymorphism.

### Invariance and wildcards (PECS)
Generics are **invariant**: `List<String>` is **not** a `List<Object>` (that would allow inserting an `Integer`). Wildcards restore flexibility safely:
- `? extends T` — a **producer** you read T from (can't add).
- `? super T` — a **consumer** you write T into (reads come back as `Object`).
- **PECS: Producer Extends, Consumer Super.**

### Arrays vs generics
Arrays are **covariant** and reified (`String[]` is an `Object[]`, type checked at runtime → `ArrayStoreException`); generics are invariant and erased. That mismatch is why you can't create generic arrays and why mixing them causes **heap pollution** / unchecked warnings.

## C. Code
```java
// PECS in action: copy from a producer (extends) to a consumer (super)
static <T> void copy(List<? extends T> src, List<? super T> dst) {
    for (T t : src) dst.add(t);   // read T from src (producer), write T into dst (consumer)
}
// copy(List<Integer>, List<Number>) works thanks to the wildcards.
```
```java
// Erasure consequences
List<String> ls = new ArrayList<>();
List<Integer> li = new ArrayList<>();
System.out.println(ls.getClass() == li.getClass()); // true -> same runtime class List
// if (ls instanceof List<String>) {}  // compile error: can't check erased type
// T[] arr = new T[10];                 // compile error: can't create generic array
```
```java
// BROKEN: raw type -> heap pollution, loses safety
List raw = new ArrayList();      // raw
raw.add("x"); raw.add(42);       // compiler can't stop you
List<String> ls2 = raw;          // unchecked warning
String s = ls2.get(1);           // ClassCastException at runtime
// FIX: always parameterize; never use raw types.
```

## D. Trade-offs

| Feature | Benefit | Limit |
|---|---|---|
| Generics | compile-time safety, no casts | erased at runtime (no reflection on type args) |
| `? extends` | read-flexible | can't add (except null) |
| `? super` | write-flexible | reads come back as Object |
| Bounded `<T extends Comparable<T>>` | use bound's methods | still erased |

## E. Interview Q&A

### GEN-01 (P1) — What problem do generics solve and how?
**Strong answer:** They move container type errors from runtime to compile time and remove casts. `List<String>` lets the compiler reject non-strings and auto-cast on read. Pre-generics, collections were `Object`-typed, so mistakes surfaced as runtime `ClassCastException`.

<a id="gen-02"></a>
### GEN-02 (P1) — Explain PECS with an example.
**Strong answer:** Producer Extends, Consumer Super. If a parameter *produces* values I read, use `? extends T` (e.g., `List<? extends Number>` to sum numbers) — I can read but not add. If it *consumes* values I write, use `? super T` (e.g., `List<? super Integer>` to add integers) — I can add but reads come back as `Object`. `Collections.copy(dest, src)` is the canonical signature: `src` is `? extends`, `dest` is `? super`. Wildcards restore the flexibility that invariance removes, safely.

<a id="gen-03"></a>
### GEN-03 (P1) — What is type erasure and what are its consequences?
**Strong answer:** The compiler enforces generic types then erases them, so at runtime `List<String>` is just `List`. Consequences: I can't do `new T[]`, `T.class`, or `instanceof List<String>`; I can't overload on type parameter alone; and the compiler adds bridge methods and casts. Erasure keeps backward compatibility with pre-generics bytecode but means generics give no runtime type information (not reified).

### GEN-04 (P1) — Why are generics invariant while arrays are covariant?
**Strong answer:** Arrays are covariant and reified, so `String[]` is an `Object[]`, but a bad store is caught at runtime with `ArrayStoreException`. Generics chose invariance for compile-time safety: `List<String>` is not a `List<Object>`, because if it were you could insert a non-String and only fail later. That's safer but less flexible, which is exactly why wildcards exist.

### GEN-05 (P2) — What is heap pollution?
**Strong answer:** When a variable of a parameterized type references an object of a different parameterized type (usually via raw types or unchecked casts), so a later read throws `ClassCastException`. Raw types and unsafe varargs generics cause it; I avoid raw types and heed unchecked warnings.

## F. Production relevance
Generics keep APIs type-safe and self-documenting; reading `Map<UserId, List<Order>>` tells you the shape at a glance. Erasure explains real limitations you hit in frameworks: why Jackson/Spring need `TypeReference`/`ParameterizedTypeReference` to deserialize `List<Foo>` (the type arg isn't at runtime), and why reflection on generic types is limited. PECS makes utility/library methods flexible without sacrificing safety. Raw types are a latent `ClassCastException` and should never appear in modern code.

## G. Revision

### 10 key takeaways
1. Generics = compile-time safety, no casts; errors move to compile time.
2. Type erasure: generics gone at runtime; `List<String>`→`List`.
3. No `new T[]`, `T.class`, `instanceof List<String>`.
4. Generics are invariant; arrays are covariant+reified.
5. PECS: Producer Extends, Consumer Super.
6. `? extends` read-only; `? super` write-only (reads as Object).
7. Bounded types let you call the bound's methods.
8. Raw types cause heap pollution — never use them.
9. Frameworks need TypeReference because of erasure.
10. Bridge methods are the compiler's erasure glue.

### 10 rapid-fire
1. When erased? (compile → runtime) 2. PECS? (producer extends, consumer super) 3. Add to `List<? extends Number>`? (no) 4. Arrays covariant? (yes) 5. `new T[]`? (illegal) 6. `instanceof List<String>`? (illegal) 7. Raw type risk? (heap pollution/CCE) 8. Deserialize `List<Foo>` needs? (TypeReference) 9. `? super Integer` read type? (Object) 10. Overload on type param only? (no)

### 5 tricky
1. Why can't you create a generic array? 2. Why is `List<String>` not a `List<Object>`? 3. Why does Jackson need `TypeReference`? 4. What's `ArrayStoreException` vs `ClassCastException`? 5. Why does a raw type compile but crash later?

### 5 exercises
1. Write a PECS `copy` method and call it with mismatched-but-compatible types.
2. Show two `List`s with different type args share a runtime class.
3. Trigger heap pollution with a raw type and a `ClassCastException`.
4. Deserialize JSON into `List<Foo>` with Jackson `TypeReference`.
5. Write `<T extends Comparable<T>> T max(List<T>)`.

### One-page revision sheet
> Generics=**compile-time** safety, no casts; **erased** at runtime (List<String>→List) → no new T[], T.class, instanceof List<String>, no type-param-only overloads; bridge methods added. **Invariant** (List<String>≠List<Object>) vs arrays **covariant+reified** (ArrayStoreException). **PECS**: ? extends=producer(read), ? super=consumer(write, read as Object). Raw types→heap pollution/CCE—never use. Frameworks need TypeReference/ParameterizedTypeReference due to erasure.

### Prerequisites to revisit
- Collections (ch.03); objects & casting (ch.01).
