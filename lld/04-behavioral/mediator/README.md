# Mediator Pattern

> Define an object that encapsulates how a set of objects interact, promoting loose coupling by keeping objects from referring to each other directly.

## What
Instead of components talking to each other directly (many-to-many), they talk only to a central mediator, which coordinates them.

## Why it exists
When every component holds references to many others, you get a tangle (N² connections) that's impossible to change — touching one component ripples everywhere. A mediator centralizes the interaction logic so components stay simple and independent.

## Where it's used in real apps
- Chat rooms (users send to the room, not to each other).
- GUI dialogs where widgets coordinate (enabling a button when a field is filled).
- Air traffic control; `java.util.concurrent` coordination objects.

## The exact problem (see `without/`)
Each `User` holds direct references to every other user and loops over them to broadcast. Adding/removing a user means updating everyone.

## How it's applied (see `with/`)
A `ChatRoom` mediator holds the users. Users send messages to the room; the room decides who receives them. Users don't reference each other.

---

## Interview Questions & Traps

**Q: What problem does Mediator solve?**
Many-to-many coupling between peers (N² references). It centralizes interaction so each colleague only knows the mediator, reducing coupling to N.

**Q: Mediator vs Observer?**
Observer is one-to-many broadcast (subject → observers). Mediator coordinates many-to-many interaction and can contain complex routing/business rules. A mediator is sometimes *implemented* with observer-style notifications.

**Q: Mediator vs Facade?**
Facade is one-directional (clients → subsystem) and doesn't add interaction logic between subsystem parts. Mediator is bidirectional and owns the interaction logic among peers.

**Follow-ups**
- *"Real-world examples?"* — Chat room, air traffic control, GUI dialog coordinating widgets, `java.util.Timer`.
- *"What's the risk?"* — The mediator can become a God object that accumulates all logic. Keep it focused or split it.
- *"How does it aid testability?"* — Colleagues are simple and independent; interaction logic is tested in one place.

**Traps to avoid**
- Letting the mediator grow into an unmaintainable God object.
- Colleagues keeping back-references to each other anyway (defeats the pattern).
- Overusing it for simple interactions where direct calls are clearer.
