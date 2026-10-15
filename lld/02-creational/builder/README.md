# Builder Pattern

> Separate the construction of a complex object from its representation so the same construction process can create different representations.

## What
Build an object step by step via a fluent builder, instead of a giant constructor with many parameters.

## Why it exists
Objects with many fields (several optional) lead to:
- **Telescoping constructors**: `new Pizza(12, true, false, true, false, 2)` — unreadable, easy to swap args.
- Mutable objects with lots of setters, which can be left half-initialized.

A builder gives readable, named steps, enforces required fields, and can produce an **immutable** final object.

## Where it's used in real apps
- `StringBuilder`, `java.time` builders, `Stream.Builder`.
- Lombok's `@Builder`, `HttpClient.newBuilder()`, OkHttp's `Request.Builder`.
- Any config/DTO with many optional fields.

## The exact problem (see `without/`)
`Burger` has a telescoping constructor. Callers pass a line of booleans/ints and can't tell which is which; adding a field breaks every call site.

## How it's applied (see `with/`)
A static nested `Builder` with chainable methods (`addCheese()`, `addBacon()`) and a `build()` that returns an immutable `Burger`. Required fields go in the builder's constructor; optional ones default sensibly.

---

## Interview Questions & Traps

**Q: When do you reach for Builder over a constructor?**
When an object has many parameters (especially many optional ones), or you want an immutable object built in readable, named steps. Rule of thumb: 4+ parameters, several optional.

**Q: Builder vs telescoping constructors vs JavaBeans (setters)?**
- Telescoping constructors: unreadable, error-prone with many params.
- JavaBeans/setters: readable but leave the object mutable and possibly in a half-built, invalid state.
- Builder: readable *and* can produce an immutable, fully-validated object.

**Q: Where should validation go?**
In `build()`, before constructing the product — so an invalid object is never created. You can also validate per-step.

**Follow-ups**
- *"Is this the GoF Builder or the 'Effective Java' Builder?"* — The common fluent static-nested-class form is the *Effective Java* idiom. GoF's Builder separates a `Director` orchestrating a `Builder` interface to build different representations. Know both; interviewers may mean either.
- *"How does Builder help immutability?"* — The product has only final fields and a private constructor; the builder is the only way in.
- *"Thread safety?"* — A builder instance isn't thread-safe; use one builder per thread. The built immutable product is safe to share.

**Traps to avoid**
- Using Builder for simple objects with 1–2 fields (over-engineering).
- Forgetting to validate in `build()`, allowing invalid objects.
- Making the product mutable anyway (defeats a key benefit).
