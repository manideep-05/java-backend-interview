# Bridge Pattern

> Decouple an abstraction from its implementation so the two can vary independently.

## What
Split a class that varies along **two independent dimensions** into two hierarchies — an "abstraction" and an "implementation" — and connect them with a reference (the bridge), rather than inheriting across both dimensions.

## Why it exists
When a type varies on two axes, pure inheritance multiplies: shapes (circle, square) × colors (red, blue) = `RedCircle`, `BlueCircle`, `RedSquare`, `BlueSquare`... Adding one color or shape multiplies the class count. Bridge turns N×M into N+M.

## Where it's used in real apps
- JDBC: the `Driver` abstraction bridges to vendor implementations.
- GUI toolkits separating a widget from its platform rendering.
- A `Notification` abstraction bridged to `MessageChannel` implementations (email/SMS/push).

## The exact problem (see `without/`)
`RedCircle`, `BlueCircle`, `RedSquare`, `BlueSquare` — a class per shape×color combination. Adding "green" means adding a class for every shape.

## How it's applied (see `with/`)
`Shape` (abstraction) holds a reference to a `Color` (implementation). `Circle`/`Square` extend `Shape`; `Red`/`Blue` implement `Color`. Combinations are composed at runtime: `new Circle(new Red())`.

---

## Interview Questions & Traps

**Q: What problem does Bridge really solve?**
Two (or more) independent dimensions of variation. Pure inheritance makes N×M classes; Bridge splits them into two hierarchies joined by composition, turning N×M into N+M.

**Q: Bridge vs Strategy?** *(trap)*
Structurally similar (composition + delegation). Bridge is **structural** — it separates an abstraction from its implementation, usually decided at construction and long-lived. Strategy is **behavioral** — it swaps an algorithm, often changed at runtime. Intent differs.

**Q: Bridge vs Adapter?**
Adapter makes existing, incompatible things work together *after the fact*. Bridge is designed *up front* to let two hierarchies vary independently.

**Follow-ups**
- *"Real-world example?"* — JDBC `Driver` abstraction over vendor implementations; a `Notification` type bridged to `MessageChannel` (email/SMS/push); GUI widget vs platform renderer.
- *"How does Bridge relate to DIP?"* — The abstraction depends on an implementation *interface*, not a concrete class — a direct application of DIP.
- *"When is Bridge overkill?"* — When there's only one dimension of change; a single hierarchy or Strategy suffices.

**Traps to avoid**
- Applying Bridge with only one axis of variation (unnecessary indirection).
- Confusing it with Strategy — remember structural vs behavioral intent.
- Leaking the implementation type into clients of the abstraction.
