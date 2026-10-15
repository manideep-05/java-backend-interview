# Behavioral Design Patterns

These patterns deal with **communication and responsibility** between objects — how they interact and distribute work.

| Pattern | Problem it solves | One-liner |
|---------|-------------------|-----------|
| [Strategy](strategy) | Swap one of several algorithms at runtime. | Pluggable algorithm. |
| [Observer](observer) | Many objects must react when one changes. | Publish/subscribe. |
| [Command](command) | Need undo/queue/log for actions. | Request as an object. |
| [State](state) | Behavior depends on an internal mode. | Object changes behavior with state. |
| [Template Method](template-method) | Steps vary but the skeleton is fixed. | Fixed algorithm, overridable steps. |
| [Chain of Responsibility](chain-of-responsibility) | One of several handlers should process a request. | Pass down the chain. |
| [Iterator](iterator) | Traverse a collection without exposing internals. | Uniform traversal. |
| [Mediator](mediator) | Many objects reference each other (spaghetti). | Central hub for interaction. |
| [Memento](memento) | Save/restore state (undo). | Snapshot and rollback. |
| [Visitor](visitor) | Add operations to a class tree without editing it. | Operation as a visitor. |

### Strategy vs State (common interview question)
Both delegate behavior to a swappable object. The difference is intent:
- **Strategy**: the client picks the algorithm; strategies don't know about each other.
- **State**: the object transitions between states, and states often decide the next state.
