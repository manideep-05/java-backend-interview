# Strategy Pattern

> Define a family of algorithms, encapsulate each one, and make them interchangeable at runtime.

## What
Pull each variant of an algorithm into its own class behind a common interface, then let the context hold one and swap it freely.

## Why it exists
A method full of `if (type == A) ... else if (type == B) ...` for each algorithm variant is hard to extend and test. Strategy replaces the conditional with polymorphism — add a new algorithm by adding a class (hello, Open/Closed).

## Where it's used in real apps
- `Comparator` passed to `Collections.sort()` / `List.sort()`.
- Payment/discount/pricing strategies, compression algorithm selection.
- Routing: shortest vs fastest vs cheapest route.

## The exact problem (see `without/`)
`PaymentContext` has a `switch` over payment type inside `pay()`. Each new method edits this method.

## How it's applied (see `with/`)
A `PaymentStrategy` interface with concrete strategies. The context holds a strategy and delegates to it; callers inject the strategy they want.

---

## Interview Questions & Traps

**Q: Strategy vs State?** *(the #1 trap)*
Identical structure (delegate to a swappable object), opposite intent. Strategy: the *client* picks the algorithm; strategies are independent and don't know each other. State: the object transitions between states, and states often decide the next state.

**Q: How does Strategy relate to OCP?**
Adding a new algorithm = adding a new strategy class, with no change to the context — textbook open/closed. It replaces a growing `switch`/`if-else` on type.

**Q: Give the canonical JDK example.**
`Comparator` passed to `Collections.sort()` / `list.sort()` — the comparison algorithm is a pluggable strategy.

**Follow-ups**
- *"Can a strategy be a lambda?"* — Yes. If the interface is functional (one method), a lambda or method reference is a lightweight strategy (common in modern Java).
- *"Who decides which strategy to use?"* — The client/caller, or a factory. The context just delegates.
- *"Downside?"* — Clients must know the strategies exist to choose one; more classes/objects.

**Traps to avoid**
- Confusing it with State (independent algorithm vs stateful transitions).
- Making the context aware of concrete strategies (defeats decoupling).
- Overusing it for a single algorithm that never varies.
