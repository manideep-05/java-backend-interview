# Abstract Factory Pattern

> Provide an interface for creating **families of related objects** without specifying their concrete classes.

## What
A "factory of factories." One factory produces a whole set of products that are meant to work together (and must stay consistent with each other).

## Why it exists
Sometimes products come in matching sets. A Windows UI needs a Windows button *and* a Windows checkbox; mixing a Windows button with a Mac checkbox is a bug. Abstract Factory guarantees you get a consistent family in one shot.

## Factory Method vs Abstract Factory
- Factory Method creates **one** product via subclass override.
- Abstract Factory creates **a family** of related products via a factory object.

## Where it's used in real apps
- Cross-platform UI toolkits (Swing look-and-feels, AWT).
- `DocumentBuilderFactory`, `javax.xml` factories.
- Database driver families (connection + command + reader for a given DB).

## The exact problem (see `without/`)
Code creates `new WindowsButton()` and `new MacCheckbox()` by hand and can accidentally mix platforms, producing an inconsistent UI.

## How it's applied (see `with/`)
A `GUIFactory` interface declares `createButton()` and `createCheckbox()`. `WindowsFactory` and `MacFactory` each return a matching family. Client code picks one factory and gets a guaranteed-consistent set.

---

## Interview Questions & Traps

**Q: When is Abstract Factory the right choice over Factory Method?**
When you must create **families of related products that have to be consistent with each other** (a Windows button + Windows checkbox, never mixed). Factory Method handles a single product; Abstract Factory handles a coordinated set.

**Q: How do the two patterns relate?**
Abstract Factory is often *implemented using* factory methods — each `createX()` on the factory is a factory method. Think of Abstract Factory as a grouping of related factory methods.

**Q: What's the main downside?**
Adding a new *product type* to the family (say, a new `Slider`) forces a new method on the factory interface and every concrete factory — a change that ripples across all families. Adding a new *family* is easy; adding a new *product* is hard.

**Follow-ups**
- *"Real-world example?"* — Cross-platform UI toolkits, DB driver families (connection + statement + result set for a vendor), `DocumentBuilderFactory`.
- *"How do you choose the family at runtime?"* — A single decision point (config/OS check) picks the concrete factory; the rest of the app stays oblivious.
- *"Abstract Factory vs Builder?"* — Abstract Factory returns products immediately in families; Builder constructs one complex object step by step.

**Traps to avoid**
- Using it when there's only one product (Factory Method or a simple factory is enough).
- Letting clients mix products from different factories (defeats the consistency guarantee).
- Confusing it with Builder — different intent (families vs step-by-step construction).
