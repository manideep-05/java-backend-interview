# OOP Fundamentals

The vocabulary of every LLD answer. Interviewers probe these to check you *understand* OOP, not just use it.

---

## 1. Composition vs Inheritance — "favor composition"
- **Inheritance (is-a):** a subclass *is a* kind of the parent. Rigid: you inherit everything, and deep hierarchies break under change (fragile base class).
- **Composition (has-a):** an object *has* another and delegates to it. Flexible: swap the part at runtime, no inheritance coupling.
- **Rule:** prefer composition; use inheritance only for a true, stable is-a with a shared contract. Most "reuse" is better served by composition. (This is why Strategy/Decorator beat subclass explosions.)

## 2. Interface vs Abstract class
- **Interface:** a pure contract (capabilities). A class can implement many. Use for "can-do" roles (`Comparable`, `Runnable`). Since Java 8, can have `default` methods.
- **Abstract class:** a partial implementation + state. Single inheritance only. Use when subclasses share code/fields and a common identity (Template Method lives here).
- **Pick interface** by default (flexibility, multiple inheritance of type); **abstract class** when you need shared state or a skeleton implementation.

## 3. equals() & hashCode() — the contract
- If `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` **must** hold.
- Equal objects → equal hash codes (required). Unequal objects *may* share a hash code (collision, allowed).
- Break this and `HashMap`/`HashSet` silently misbehave (lookups miss, duplicates appear). Always override both together.

## 4. Immutability
- All fields `final`, set once in the constructor, no setters, defensive-copy mutable inputs/outputs.
- Benefits: inherently **thread-safe** (no mutation = no races), safe to share and cache, good map keys.
- This is why value objects in the design problems are immutable.

## 5. Comparable vs Comparator
- **Comparable** (`compareTo`): the type's *natural* ordering, baked into the class. One per class.
- **Comparator**: an *external*, swappable ordering (Strategy!). Many per use-site: `Comparator.comparing(...)`.
- Use Comparable for the one obvious order; Comparator when you need multiple/at-call-site orderings.

## 6. Enums with behavior
- Java enums are full classes: they can have fields, constructors, methods, and even per-constant method overrides.
- Great for state machines, strategies-with-a-fixed-set, and replacing `int`/`String` constants with type safety.

---

## Files
- `CompositionVsInheritance.java` — the same feature done both ways.
- `Shapes.java` — interface vs abstract class.
- `EqualsHashCode.java` — the contract, and what breaks without it.
- `ImmutableMoney.java` — a correct immutable value object.
- `Sorting.java` — Comparable + Comparator.
- `Operation.java` — an enum with per-constant behavior.
- `Main.java` — runs them all.
