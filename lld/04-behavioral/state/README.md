# State Pattern

> Allow an object to alter its behavior when its internal state changes. The object appears to change its class.

## What
Represent each state as its own class implementing a common interface. The context delegates behavior to its current state object, and states handle transitions to other states.

## Why it exists
A single class with giant `switch(state)` blocks in every method becomes a tangle: the same state checks are duplicated across methods, and adding a state touches them all. State moves each state's behavior and transitions into its own cohesive class.

## State vs Strategy
Same structure (delegate to a swappable object), different intent: Strategy variants are independent and chosen by the client; State objects know about each other and drive transitions.

## Where it's used in real apps
- Order lifecycle (New → Paid → Shipped → Delivered).
- TCP connection states, workflow/approval engines.
- Media player (Playing/Paused/Stopped), vending machines.

## The exact problem (see `without/`)
A `Document` uses an enum + `switch` in every method. Each method repeats the state logic; transitions are scattered and easy to get inconsistent.

## How it's applied (see `with/`)
A `DocumentState` interface with `Draft`, `Moderation`, `Published` states. The document delegates to its current state, and each state decides the next one.

---

## Interview Questions & Traps

**Q: State vs Strategy?** *(the #1 trap)*
Same structure, different intent. State models *transitions* — states know about and switch to one another, and behavior changes as the object moves through its lifecycle. Strategy is a client-selected, independent algorithm with no transitions.

**Q: Who should drive the state transition — the context or the state?**
Commonly the state object decides the next state (as in `with/`), keeping transition rules cohesive per state. Alternatively the context owns a transition table. Be ready to discuss both; the state-driven approach spreads knowledge, the table centralizes it.

**Q: What's the smell State replaces?**
Repeated `switch(currentState)` blocks in every method, with transition logic scattered and easy to make inconsistent.

**Follow-ups**
- *"Real-world examples?"* — Order lifecycle, TCP connection states, vending machine, media player (Play/Pause/Stop), approval workflows.
- *"How do you avoid creating a new state object every transition?"* — Cache/share stateless state singletons (states with no instance data).
- *"How does this relate to a finite state machine?"* — It's an object-oriented FSM; each state is a class, transitions are method calls.

**Traps to avoid**
- Confusing with Strategy (transitions vs independent algorithm choice).
- Letting the context keep giant switches anyway (half-applied pattern).
- Allocating a new state object on every transition when a shared instance would do.
