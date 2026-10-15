# Memento Pattern

> Capture and externalize an object's internal state so it can be restored later, without violating encapsulation.

## What
Three roles:
- **Originator**: the object whose state we want to save/restore.
- **Memento**: an opaque snapshot of that state.
- **Caretaker**: keeps mementos (e.g. an undo stack) but never inspects their contents.

## Why it exists
Implementing undo by exposing all of an object's fields breaks encapsulation and couples the undo code to internals. Memento lets the object produce an opaque snapshot and restore from it, keeping its internals private.

## Where it's used in real apps
- Undo/redo in editors and IDEs.
- Save points in games; database savepoints/transactions.
- `java.io.Serializable` snapshots, form state restoration.

## The exact problem (see `without/`)
Undo is implemented by the caretaker reaching into the editor's fields to save and restore them — fragile and encapsulation-breaking.

## How it's applied (see `with/`)
`TextEditor` creates a `Memento` of its own state and restores from one. A `History` caretaker just pushes/pops mementos without knowing what's inside.

---

## Interview Questions & Traps

**Q: What are the three roles?**
Originator (owns the state, creates/restores mementos), Memento (the opaque snapshot), Caretaker (stores mementos, e.g. an undo stack, but never inspects them).

**Q: How does Memento preserve encapsulation?** *(key point)*
Only the originator can read a memento's contents (e.g. via a package-private accessor or an inner class). The caretaker treats it as a black box, so internal state never leaks.

**Q: Memento vs Command for undo?**
Memento restores a saved snapshot (state-based undo). Command's `undo()` reverses an operation (operation-based undo). They're often combined: a command stores a memento to undo complex changes.

**Follow-ups**
- *"Memory concern with many snapshots?"* — Full snapshots are costly; use incremental/diff mementos, cap history size, or snapshot only on meaningful checkpoints.
- *"Real-world examples?"* — Editor undo/redo, game save points, DB savepoints, transaction rollback.
- *"How to snapshot a large object cheaply?"* — Copy-on-write or store deltas rather than full copies.

**Traps to avoid**
- Exposing the memento's internals to the caretaker (breaks encapsulation — the whole point).
- Unbounded history causing memory bloat.
- Shallow snapshots of mutable state that then mutates underneath you.
