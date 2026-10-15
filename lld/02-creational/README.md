# Creational Design Patterns

These patterns deal with **object creation** — making a system independent of *how* its objects are created, composed, and represented. They hide the `new` keyword behind intention-revealing abstractions.

| Pattern | Problem it solves | One-liner |
|---------|-------------------|-----------|
| [Singleton](singleton) | Need exactly one shared instance (config, logger, connection pool). | One instance, global access. |
| [Factory Method](factory-method) | Creating code shouldn't know the concrete class. | Subclass decides what to build. |
| [Abstract Factory](abstract-factory) | Need families of related objects that must match. | Factory of factories. |
| [Builder](builder) | Object has many optional params; telescoping constructors hurt. | Build step by step, fluently. |
| [Prototype](prototype) | Creating from scratch is expensive; clone an existing one. | Copy, don't construct. |

### Factory Method vs Abstract Factory (common interview question)
- **Factory Method**: one product, picked by a subclass overriding a creation method.
- **Abstract Factory**: multiple related products created together so they stay consistent (e.g. a whole UI toolkit: button + checkbox + menu all matching one theme).
