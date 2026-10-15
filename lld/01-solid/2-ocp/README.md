# Open/Closed Principle (OCP)

> Software entities should be **open for extension, but closed for modification**.

## What
You should be able to add new behavior by **adding new code**, not by **editing existing, tested code**.

## Why it exists
Every time you edit a class that already works, you risk introducing a bug into something that was fine. A long `if/else` or `switch` on a type is the classic warning sign: every new type forces you to reopen and re-test that method.

## Where it's used in real apps
- Payment processors: adding `UpiPayment` shouldn't mean editing the checkout engine.
- Notification systems: adding `SlackNotifier` shouldn't touch existing email/SMS code.
- Discount/pricing engines, file exporters (PDF/CSV/XML), plugin architectures.

## The exact problem (see `without/`)
`PaymentProcessor` has a `switch(type)`. Adding a new payment method means editing that switch — and risking the already-shipped credit-card path.

## How it's applied (see `with/`)
Introduce a `PaymentMethod` abstraction. Each payment type is its own class implementing `pay()`. The processor depends on the abstraction and never changes when a new method is added — you just add a new class.

---

## Interview Questions & Traps

**Q: What does "closed for modification" actually mean?**
Once a class is written and tested, you add new behavior by extending (new subclass/implementation), not by editing that class's existing code. Existing, shipped code stays untouched.

**Q: How is OCP typically achieved?**
Through abstraction + polymorphism: depend on an interface, add new implementations. Strategy, Factory Method, and Template Method are common vehicles. It usually leans on DIP.

**Q: Can you ever fully close a class to modification?**
No — you can't anticipate every change. You close it against the *anticipated axis of variation* (e.g. "new payment types") while accepting other changes may still require edits. Don't try to make everything open; it leads to over-engineering.

**Follow-ups**
- *"Isn't adding an interface for everything over-engineering?"* — Yes, if the variation never materializes. Apply OCP where change is likely, not speculatively (YAGNI).
- *"How does a `switch` statement signal an OCP problem?"* — A switch on type that grows with every new type is the classic smell; replace with polymorphism.
- *"OCP vs the Strategy pattern?"* — Strategy is one concrete way to realize OCP for swappable algorithms.

**Traps to avoid**
- Speculative generality: abstracting before you have two real cases.
- Thinking OCP forbids *all* edits — bug fixes and new axes of change still happen.
- Leaving a `default:` branch that throws, then still editing the switch for every new type (you didn't actually close it).
