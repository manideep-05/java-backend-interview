# Prototype Pattern

> Create new objects by **cloning an existing instance** (the prototype), instead of constructing from scratch.

## What
Keep a pre-built example object and produce new ones by copying it, then tweak the copy.

## Why it exists
Sometimes building an object is expensive (heavy initialization, a DB/network round-trip, lots of config) or the exact concrete class isn't known at compile time. Cloning a ready-made prototype is cheaper and decouples the client from concrete classes.

## Deep vs shallow copy (key interview point)
- **Shallow copy**: copies field values; object references are shared. Mutating a shared nested object affects both copies.
- **Deep copy**: also copies the nested objects, so clones are fully independent. Prefer deep copy when the object holds mutable references.

## Where it's used in real apps
- `Object.clone()` / `Cloneable` in Java (with caveats).
- Game dev: spawn many enemies from one configured template.
- Copying complex configuration or document templates.

## The exact problem (see `without/`)
Each new enemy is built via a costly constructor that re-loads shared config, even though every enemy starts from the same template.

## How it's applied (see `with/`)
Build the template once, then `clone()` it for each new enemy and adjust only what differs. We implement an explicit **deep copy** to avoid shared-reference bugs.

---

## Interview Questions & Traps

**Q: Deep copy vs shallow copy?** *(the central trap)*
Shallow copy duplicates field values but shares referenced objects — mutating a nested object affects both copies. Deep copy also duplicates the nested objects, so clones are independent. For a prototype holding mutable references, you almost always need a deep copy.

**Q: What's wrong with Java's `Cloneable` / `Object.clone()`?**
It's widely considered broken: `clone()` is `protected` on `Object`, `Cloneable` is a marker with no `clone` method, it bypasses constructors, and default `clone()` is shallow. Prefer a copy constructor or a static factory (as in `with/`).

**Q: When is Prototype actually useful?**
When creating an object from scratch is expensive (heavy init, I/O, config) or when the concrete class isn't known at compile time and you clone a pre-registered example.

**Follow-ups**
- *"How do you deep-copy a complex graph with cycles?"* — Track visited objects in a map (identity map) to avoid infinite loops; or serialize/deserialize (slower, requires Serializable).
- *"Prototype vs Factory?"* — Factory builds from scratch; Prototype copies an existing instance.
- *"Where in the JDK?"* — `Object.clone()`, and `ArrayList(Collection)` style copy constructors.

**Traps to avoid**
- Shallow-copying an object with mutable collections/references and getting shared-state bugs.
- Relying on `Cloneable` without understanding its pitfalls.
- Deep-copying immutable fields unnecessarily (wasteful — immutables can be shared safely).
