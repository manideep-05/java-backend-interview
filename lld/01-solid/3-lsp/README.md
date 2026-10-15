# Liskov Substitution Principle (LSP)

> Objects of a superclass should be **replaceable with objects of its subclasses without breaking the program**.

## What
If `S` is a subtype of `T`, you should be able to use an `S` wherever a `T` is expected and everything still works correctly. A subclass must honor the **contract** (expectations) of its parent — not just the method signatures.

## Why it exists
Inheritance implies an "is-a" promise. When a subclass breaks that promise (throws where the parent didn't, weakens guarantees, or needs special-casing by callers), polymorphism stops being safe and you end up writing `if (obj instanceof X)` checks everywhere — defeating the point of the hierarchy.

## The classic trap
`Square extends Rectangle`. A square "is-a" rectangle mathematically, but a Rectangle's contract says "setting width leaves height unchanged." A Square can't honor that. Code written against Rectangle breaks when handed a Square.

## Where it's used in real apps
- Collection hierarchies (an immutable list that `extends List` but throws on `add()` violates LSP — hello `UnsupportedOperationException`).
- Any plugin/strategy where callers rely on base-class guarantees.

## The exact problem (see `without/`)
`Square extends Rectangle` and overrides the setters so width and height stay equal. A method that stretches a rectangle's width and asserts the height is unchanged **fails** for a Square.

## How it's applied (see `with/`)
Don't force a false "is-a". Introduce a common abstraction (`Shape` with `area()`) and let `Rectangle` and `Square` be independent implementations. No subclass lies about its parent's contract.

---

## Interview Questions & Traps

**Q: State LSP simply.**
Anywhere you use a base type, you should be able to substitute any subtype without the program behaving incorrectly. Subtypes must honor the base type's contract.

**Q: Why is `Square extends Rectangle` the classic violation?**
Rectangle's contract allows width and height to vary independently. A Square can't honor that (setting width must change height), so code written against Rectangle breaks when given a Square. The "is-a" relationship is false at the behavioral level.

**Q: What are the formal rules behind LSP?**
A subtype may: weaken preconditions (accept more), strengthen postconditions (guarantee more), and must preserve invariants. It must NOT: strengthen preconditions, weaken postconditions, or throw new exceptions the base didn't.

**Follow-ups**
- *"Give a real-world LSP violation."* — A read-only collection extending `List` but throwing `UnsupportedOperationException` on `add()`. It breaks code that relies on `List`'s contract.
- *"How do you fix an LSP violation?"* — Don't force a false hierarchy. Use a shared abstraction and composition (e.g. both are `Shape`), or split the interface.
- *"How does LSP relate to ISP?"* — Fat interfaces often force LSP violations (stubs that throw); segregating interfaces removes the pressure.

**Traps to avoid**
- Judging "is-a" by real-world taxonomy instead of behavioral contract (a square *is* a rectangle in math, but not behaviorally).
- Overriding a method to throw `UnsupportedOperationException` — a red flag for an LSP (and ISP) violation.
- Returning `null` or a narrower type where the base promised a valid, broader result.
