# Structural Design Patterns

These patterns deal with how classes and objects are **composed** into larger structures while keeping them flexible and efficient.

| Pattern | Problem it solves | One-liner |
|---------|-------------------|-----------|
| [Adapter](adapter) | Two existing interfaces don't match. | A plug converter. |
| [Decorator](decorator) | Add behavior to one object without touching its class or others. | Wrap to extend. |
| [Facade](facade) | A subsystem is complex to use. | One simple front door. |
| [Composite](composite) | Treat a tree of objects and leaves uniformly. | Part-whole as one type. |
| [Proxy](proxy) | Control access to an object (lazy load, cache, guard). | A controlled stand-in. |
| [Bridge](bridge) | Two dimensions of variation explode into N×M classes. | Split abstraction from implementation. |
| [Flyweight](flyweight) | Too many similar objects eat memory. | Share the common parts. |

### Decorator vs Proxy vs Adapter (common confusion)
- **Adapter** changes an interface so two things can connect (different interface).
- **Decorator** keeps the same interface but adds behavior (same interface, more behavior).
- **Proxy** keeps the same interface but controls access (same interface, same behavior, gated).
