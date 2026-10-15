# Flyweight Pattern

> Use sharing to support large numbers of fine-grained objects efficiently.

## What
Separate an object's state into:
- **Intrinsic state**: shared, context-independent (can be reused across many objects).
- **Extrinsic state**: context-dependent (passed in by the client each time).

Then share one flyweight instance per unique intrinsic state instead of allocating a new object per element.

## Why it exists
Rendering a forest with a million trees, or a document with millions of characters, would exhaust memory if each element stored its own copy of shared data (texture, font glyph). Flyweight shares that data so memory stays bounded.

## Where it's used in real apps
- `java.lang.Integer.valueOf()` caches small Integers (-128..127).
- String interning / string pool.
- Game engines sharing sprites/meshes; text editors sharing glyphs.

## The exact problem (see `without/`)
Each `Tree` stores its own copy of the heavy texture/mesh data. A million trees means a million copies — huge memory use.

## How it's applied (see `with/`)
`TreeType` (the flyweight) holds the shared intrinsic data and is cached in a factory. Each `Tree` stores only its extrinsic state (x, y) plus a reference to a shared `TreeType`.

---

## Interview Questions & Traps

**Q: Intrinsic vs extrinsic state?** *(central question)*
Intrinsic = shared, context-independent state stored in the flyweight (reused across many contexts). Extrinsic = context-dependent state passed in by the client at call time (never stored in the shared flyweight).

**Q: Why must flyweights be immutable?**
Because they're shared across many clients, any mutation would affect all of them. Intrinsic state is set once and never changed.

**Q: Give a JDK example.**
`Integer.valueOf()` caches boxed Integers from -128 to 127 (why `==` works for small boxed ints but not large ones). Also String interning / the string pool.

**Follow-ups**
- *"Thread safety?"* — The factory's cache needs safe concurrent access (e.g. `ConcurrentHashMap`); immutable flyweights are otherwise safe to share.
- *"Flyweight vs Singleton?"* — Singleton = exactly one instance. Flyweight = one shared instance *per unique intrinsic state* (potentially many).
- *"When does Flyweight backfire?"* — When objects are mostly unique (little shared state) — the factory/indirection overhead isn't worth it.

**Traps to avoid**
- Storing extrinsic state inside the flyweight (breaks sharing, reintroduces the memory problem).
- Making flyweights mutable.
- Using it when there aren't actually many near-duplicate objects.
