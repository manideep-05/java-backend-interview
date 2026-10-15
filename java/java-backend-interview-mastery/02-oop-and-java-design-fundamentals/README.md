# 02 — OOP & Java Design Fundamentals

> **Priority: P1.** Not LLD problems (those are prepared separately) — this is about Java's *object behavior*: polymorphism, binding, composition vs inheritance, and interface design, which underpin how your code and Spring behave.

**Prerequisites:** [01 Core Java](../01-core-java/).
**Related:** [09 Spring DI](../09-spring-core-and-dependency-injection/) (program to interfaces), [04 Generics](../04-generics-and-type-system/).

---

## A. Concept & intuition
OOP organizes code around objects bundling state and behavior. The four pillars: **encapsulation** (hide internals behind an interface), **abstraction** (expose essential behavior, hide implementation), **inheritance** (reuse via an is-a hierarchy), **polymorphism** (one reference type, many runtime behaviors). The engineering payoff is loose coupling and substitutability — which is exactly what DI and testing exploit.

## B. Internals & key distinctions

### Overloading vs overriding; static vs dynamic binding
- **Overloading** (compile-time/static binding): same method name, different parameter lists; resolved by the compiler from the *static* type.
- **Overriding** (runtime/dynamic dispatch): a subclass redefines a superclass method; resolved at runtime from the *actual object type* via the vtable. This is how polymorphism works.
```java
class Animal { String sound(){ return "?"; } }
class Dog extends Animal { @Override String sound(){ return "woof"; } }
Animal a = new Dog();
a.sound(); // "woof" — dynamic dispatch picks Dog's method at runtime
```
- **Fields and `static` methods are NOT polymorphic** — they bind by static type (a classic trap): `Animal a = new Dog(); a.someField` uses `Animal`'s field; a hidden static method uses the declared type.

### Composition vs inheritance
Inheritance couples a subclass to the superclass's implementation (fragile base class, exposes protected internals). **Composition** (hold a collaborator and delegate) is more flexible, testable, and avoids deep hierarchies. "Favor composition over inheritance" — use inheritance only for genuine is-a with a stable base; use composition for has-a/uses-a.

### Abstract class vs interface
- **Interface**: a contract; a type can implement many; supports `default` and `static` methods (Java 8+) and constants. No instance state.
- **Abstract class**: partial implementation with state; single inheritance. Use when you share state/implementation among closely related types.
Prefer interfaces for capability contracts (and DI); use abstract classes for shared skeletal implementation.

### Constructors & initialization order
`super()` runs first, then instance initializers/field initializers top-to-bottom, then the constructor body. Static initializers run once at class load. Calling an overridable method from a constructor is dangerous — the subclass override runs before the subclass's fields are initialized.

### Covariant return types
An override may return a subtype of the original return type (`clone()` returning the concrete type), improving the API without casts.

## C. Code — composition over inheritance
```java
// FRAGILE: extending to add behavior couples you to the parent's implementation
class CountingList<E> extends ArrayList<E> {
    int count = 0;
    @Override public boolean add(E e){ count++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c){ count += c.size(); return super.addAll(c); }
    // BUG: ArrayList.addAll internally calls add() in some versions -> double counting (implementation leak)
}
```
```java
// ROBUST: composition + delegation, independent of the parent's internals
class CountingList<E> {
    private final List<E> delegate = new ArrayList<>();
    private int count = 0;
    public boolean add(E e){ count++; return delegate.add(e); }
    public boolean addAll(Collection<? extends E> c){ count += c.size(); return delegate.addAll(c); }
    public int count(){ return count; }
}
```

## D. Trade-offs

| Choice | Pros | Cons | Use when |
|---|---|---|---|
| Inheritance | reuse, polymorphism | tight coupling, fragile base, single parent | genuine stable is-a |
| Composition | flexible, testable, swappable | more delegation code | has-a/uses-a (default) |
| Interface | multiple, contract, DI-friendly | no state | capability contracts |
| Abstract class | shared state+skeleton | single inheritance | closely related types sharing code |

## E. Interview Q&A

### OOP-01 (P1) — Overloading vs overriding; which binds when?
**Strong answer:** Overloading is resolved at compile time by the static types of the arguments (static binding); overriding is resolved at runtime by the actual object type via dynamic dispatch, which is what gives polymorphism. A trap: fields and static methods bind by static type, so they aren't polymorphic — only instance method overrides are.

### OOP-02 (P1) — Composition vs inheritance — why favor composition?
**Strong answer:** Inheritance couples a subclass to the superclass's implementation details, so internal changes break subclasses (the fragile base class problem — like overriding `ArrayList` and getting double-counted calls because `addAll` calls `add` internally). Composition holds a collaborator and delegates, so it depends only on the public contract, is easy to test and swap, and avoids deep hierarchies. I use inheritance only for a true, stable is-a relationship.

### OOP-03 (P1) — Abstract class vs interface — when each?
**Strong answer:** Interfaces are capability contracts a type can implement many of, now with `default`/`static` methods but no instance state — ideal for DI and polymorphism. Abstract classes provide shared state and a skeletal implementation but only single inheritance — good when related types share real code. I default to interfaces and reach for an abstract class to share implementation among closely related types.

### OOP-04 (P1) — Why is calling an overridable method from a constructor dangerous?
**Strong answer:** Because of initialization order: the superclass constructor runs before the subclass's fields are initialized, so an overridden method invoked from the super constructor sees uninitialized (null/zero) subclass state. I avoid calling overridable methods from constructors, or make them `final`/`private`.

### OOP-05 (P1) — What does "program to an interface" mean and why does it matter?
**Strong answer:** Depend on abstractions (interfaces) rather than concrete classes, so implementations can be swapped (test doubles, alternative backends) without changing callers. It's the basis of DI and the Liskov/dependency-inversion ideas, and it's what makes code testable and loosely coupled.

### OOP-06 (P2) — What are default methods and why were they added?
**Strong answer:** `default` methods let interfaces carry implementation, added in Java 8 so new methods (like `Collection.stream()`) could be added to existing interfaces without breaking every implementer. They enable interface evolution but should be used sparingly to avoid confusing multiple-inheritance-of-behavior situations.

## F. Production relevance
These fundamentals shape maintainable services: composition and interfaces make code testable (inject fakes), keep coupling low, and let Spring wire alternative implementations; understanding dynamic dispatch explains how Spring proxies and strategy patterns work; the constructor/initialization trap and non-polymorphic fields cause subtle real bugs. Good interface design is also API design — stable contracts that evolve via default methods rather than breaking changes.

## G. Revision

### 10 key takeaways
1. Four pillars: encapsulation, abstraction, inheritance, polymorphism.
2. Overloading=static binding; overriding=dynamic dispatch.
3. Fields and static methods are not polymorphic.
4. Favor composition over inheritance (fragile base class).
5. Interface=contract (multiple, default/static, no state); abstract class=shared state+skeleton (single).
6. Program to interfaces → testable, loosely coupled, DI-friendly.
7. Don't call overridable methods from constructors.
8. Init order: super → field/instance initializers → constructor body; static once at load.
9. Covariant return types refine overrides.
10. Default methods enable interface evolution.

### 10 rapid-fire
1. Overriding binds when? (runtime) 2. Overloading binds when? (compile) 3. Fields polymorphic? (no) 4. Default inheritance style to prefer? (composition) 5. Multiple inheritance of type via? (interfaces) 6. State in interfaces? (no) 7. First in a constructor? (super()) 8. Static init runs? (once at class load) 9. Return subtype in override? (covariant, yes) 10. DI relies on? (interfaces)

### 5 tricky
1. Why did overriding `ArrayList.addAll` double-count? (impl leak → use composition) 2. Why did a field access ignore the subclass value? (static binding) 3. Why did a constructor see null subclass fields? (init order) 4. Interface vs abstract class for a strategy? 5. Why is deep inheritance a smell?

### 5 exercises
1. Show dynamic dispatch vs field hiding with a parent/child pair.
2. Rewrite a fragile subclass as composition+delegation.
3. Demonstrate the constructor-calls-overridable-method bug.
4. Convert a concrete dependency to an injected interface and unit-test with a fake.
5. Add a `default` method to an interface and show existing implementers still compile.

### One-page revision sheet
> Pillars: encapsulation/abstraction/inheritance/polymorphism. **Overloading=static bind; overriding=dynamic dispatch.** Fields & statics NOT polymorphic. **Favor composition over inheritance** (fragile base class). Interface=contract(many, default/static, no state); abstract=shared state+skeleton(single). Program to interfaces → DI/testable. Don't call overridable methods in constructors. Init: super→fields→body; static once. Covariant returns. Default methods=interface evolution.

### Prerequisites to revisit
- Objects & references (ch.01); feeds DI (ch.09) and generics variance (ch.04).
