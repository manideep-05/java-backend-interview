# Factory Method Pattern

> Define an interface for creating an object, but let subclasses decide which class to instantiate.

## What
Instead of calling `new ConcreteThing()` directly in business code, you call a factory method. Subclasses (or a parameterized factory) override/choose which concrete product to create.

## Why it exists
When creation logic is scattered as `new` calls across the codebase, every new product type forces edits everywhere a product is created. Centralizing creation behind a factory method:
- Removes `new ConcreteType` from client code (decouples client from concretes).
- Puts the "which class?" decision in one place.
- Plays directly into the Open/Closed Principle.

## Where it's used in real apps
- `java.util.Calendar.getInstance()`, `NumberFormat.getInstance()`.
- Logging frameworks returning a `Logger` based on config.
- Document apps: a `createDocument()` the subclasses override to produce Word/PDF docs.

## The exact problem (see `without/`)
A `LogisticsApp` has `if (type == "road") new Truck()... else new Ship()`. Adding air freight means editing that method and every similar one.

## How it's applied (see `with/`)
A `Logistics` base class declares `createTransport()` (the factory method). `RoadLogistics` and `SeaLogistics` override it to return a `Truck`/`Ship`. The planning code calls `createTransport()` and never names a concrete class.

---

## Interview Questions & Traps

**Q: Factory Method vs Simple/Static Factory?** *(common trap)*
A static method that returns objects based on a parameter (`ShapeFactory.create("circle")`) is the **simple factory idiom**, not a GoF pattern. GoF **Factory Method** uses a *subclass overriding a creation method* to decide the concrete type. Interviewers often check if you conflate them.

**Q: Factory Method vs Abstract Factory?**
Factory Method creates **one** product via subclass override. Abstract Factory creates a **family** of related products via a factory object (and often *uses* factory methods internally).

**Q: How does Factory Method support OCP?**
Clients code against the product interface and the creator; adding a new product means adding a new creator subclass + product class, with no edits to existing creation code.

**Follow-ups**
- *"Where's this in the JDK?"* — `Calendar.getInstance()`, `NumberFormat.getInstance()` (these are more simple-factory style), and framework hooks that subclasses override.
- *"Downside of Factory Method?"* — Can require a parallel subclass hierarchy (one creator per product), adding classes.
- *"When would a plain constructor be better?"* — When there's no polymorphic choice to make. Don't add a factory just to wrap `new`.

**Traps to avoid**
- Calling any method that returns an object a "factory pattern."
- Introducing a factory when the concrete type never varies (over-engineering).
- Mixing creation logic back into clients, defeating the purpose.
