# Command Pattern

> Encapsulate a request as an object, letting you parameterize clients with different requests, queue or log them, and support undo.

## What
Wrap "what to do" into a command object with an `execute()` (and optionally `undo()`). An invoker triggers commands without knowing their concrete work.

## Why it exists
When the thing issuing a request (a button, a menu) is hard-wired to a specific action, you can't easily queue, log, undo, or reassign actions. Turning the request into an object decouples invoker from receiver and unlocks undo/redo, macros, and queuing.

## Where it's used in real apps
- `Runnable` submitted to an `ExecutorService` (a command on a queue).
- Undo/redo in editors; GUI actions (`javax.swing.Action`).
- Task queues, transactional operations, macro recording.

## The exact problem (see `without/`)
A `RemoteControl` has `if (button == "light_on") light.on()...`. It's welded to specific devices and offers no undo.

## How it's applied (see `with/`)
Each action is a `Command` (`LightOnCommand`, `LightOffCommand`) holding its receiver. The remote stores commands and can execute and undo them generically.

---

## Interview Questions & Traps

**Q: What capabilities does turning a request into an object unlock?**
Undo/redo (via `undo()`), queuing/scheduling, logging and replay, macros (composite commands), and decoupling the invoker from the receiver.

**Q: How do you implement redo?**
Keep two stacks: undo and redo. Executing or redoing pushes onto undo; undoing pops from undo, runs `undo()`, and pushes onto redo. A new command clears the redo stack.

**Q: How does Command relate to `Runnable`/`ExecutorService`?**
A `Runnable` submitted to an executor is essentially a command placed on a queue — the invoker (executor) runs it without knowing what it does.

**Follow-ups**
- *"How do you make undo robust for non-trivial actions?"* — Store the pre-state (often with Memento) so `undo()` can restore it, rather than computing an inverse.
- *"Macro command?"* — A command containing a list of commands, executing/undoing them in order (Composite + Command).
- *"Where do parameters live?"* — Captured in the command object at construction, so execution is self-contained.

**Traps to avoid**
- `undo()` that recomputes an inverse incorrectly for irreversible ops — snapshot state instead.
- Fat commands containing business logic that belongs in the receiver.
- Forgetting to clear the redo stack when a new command executes.
