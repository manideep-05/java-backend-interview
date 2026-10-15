# Facade Pattern

> Provide a unified, simplified interface to a set of interfaces in a subsystem.

## What
A single "front door" class that wraps a complicated subsystem and exposes a few easy methods for the common use cases.

## Why it exists
Clients shouldn't need to know the correct order to call ten subsystem classes, or how they wire together. A facade hides that complexity and reduces coupling between the client and the subsystem internals.

## Where it's used in real apps
- `javax.faces`, SLF4J as a facade over logging backends.
- A `OrderService.placeOrder()` that internally coordinates inventory, payment, and shipping.
- SDK "client" classes that hide dozens of lower-level calls.

## The exact problem (see `without/`)
To start a movie, the client must call the amplifier, the lights, the projector, and the player in exactly the right sequence. That knowledge leaks into every caller.

## How it's applied (see `with/`)
A `HomeTheaterFacade` exposes `watchMovie()` and `endMovie()`. It orchestrates the subsystem internally; the client makes one call. (The subsystem is still usable directly if needed — a facade doesn't hide it, it simplifies it.)

---

## Interview Questions & Traps

**Q: Does a Facade hide the subsystem completely?**
No. It provides a simpler entry point for common cases but doesn't prevent advanced clients from using the subsystem classes directly. It reduces coupling, it doesn't enforce encapsulation.

**Q: Facade vs Adapter?** *(trap)*
Adapter converts an existing interface to a specific target interface clients already expect. Facade defines a brand-new, simplified interface over many classes; there's no pre-existing target to match.

**Q: Facade vs Mediator?**
Facade is one-directional: clients call into the subsystem through it. Mediator is bidirectional: it coordinates peer objects that talk back and forth through it.

**Follow-ups**
- *"Can a Facade become a God object?"* — Yes, if it accumulates logic. Keep it a thin coordinator; push real logic into the subsystem.
- *"Real-world examples?"* — SLF4J over logging backends, a `CheckoutService` coordinating inventory/payment/shipping, SDK client classes.
- *"How does Facade support loose coupling?"* — Clients depend on the facade, not the many subsystem classes, so subsystem internals can change freely.

**Traps to avoid**
- Stuffing business rules into the facade (it should orchestrate, not own domain logic).
- Forcing all access through the facade when a client legitimately needs a subsystem detail.
- Treating it as the same as Adapter.
