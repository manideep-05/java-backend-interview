# Iterator Pattern

> Provide a way to access the elements of a collection sequentially without exposing its underlying representation.

## What
An iterator object knows how to walk a collection (`hasNext()` / `next()`), so clients traverse uniformly regardless of whether the collection is an array, a tree, or a linked list.

## Why it exists
If clients loop using the collection's internal structure (array index, node pointers), they're coupled to that structure — change it and every loop breaks. An iterator hides the structure and offers one traversal API.

## Where it's used in real apps
- `java.util.Iterator`, the enhanced for-loop (anything implementing `Iterable`).
- Database cursors, paginated API result walkers.
- Tree/graph traversals exposed as iterators.

## The exact problem (see `without/`)
A custom `BookShelf` exposes its internal array. Clients must know it's an array and loop by index; switching to a list would break them.

## How it's applied (see `with/`)
`BookShelf` implements `Iterable<Book>` and returns an `Iterator`. Clients use `hasNext()/next()` (or a for-each) and never touch the internal storage.

---

## Interview Questions & Traps

**Q: What is a fail-fast iterator?**
A JDK iterator that throws `ConcurrentModificationException` if the collection is structurally modified during iteration (except via the iterator's own `remove()`). It uses a modification-count check. Fail-safe iterators (e.g. on `CopyOnWriteArrayList`) iterate a snapshot and don't throw.

**Q: External vs internal iterator?**
External (`hasNext()/next()`): the client controls traversal. Internal (`forEach(consumer)`, streams): the collection controls traversal and the client supplies the operation.

**Q: Why `Iterable` and not just `Iterator`?**
`Iterable` is a factory for fresh `Iterator`s, enabling the enhanced for-loop and multiple independent traversals.

**Follow-ups**
- *"How do you remove safely during iteration?"* — Use `iterator.remove()`, not `collection.remove()`.
- *"Lazy/infinite iterators?"* — `next()` can compute on demand, enabling infinite sequences (like streams).
- *"Iterator vs Stream?"* — Streams add functional, lazy, parallelizable pipelines on top of the traversal idea.

**Traps to avoid**
- Modifying a collection directly while iterating (CME).
- Returning the internal collection instead of an iterator (leaks representation).
- Stateful iterators shared across threads.
