# Composite Pattern

> Compose objects into tree structures to represent part-whole hierarchies. Lets clients treat individual objects and compositions **uniformly**.

## What
A single interface for both "leaf" objects and "containers" of objects. A container forwards operations to its children, which may themselves be containers — recursion all the way down.

## Why it exists
Without it, client code must constantly ask "is this a single item or a group?" and branch accordingly. Composite erases that distinction: you call `getSize()` on a file or a whole folder the same way.

## Where it's used in real apps
- File systems (file vs directory).
- UI component trees (a panel contains buttons and other panels).
- `java.awt.Component`/`Container`, DOM nodes, org charts.

## The exact problem (see `without/`)
Computing total size requires the client to check whether each node is a file or a folder and recurse manually, with type checks scattered around.

## How it's applied (see `with/`)
`FileSystemNode` has `getSize()`. `File` (leaf) returns its own size; `Folder` (composite) sums its children. The client calls `getSize()` on anything and never type-checks.

---

## Interview Questions & Traps

**Q: The transparency vs safety trade-off?** *(key question)*
- **Transparent**: declare child-management methods (`add`/`remove`) on the common component interface so leaves and composites look identical — but leaves must then handle/ignore `add()` (potential LSP issue).
- **Safe**: put `add`/`remove` only on the composite — type-safe, but clients must know whether they hold a leaf or a composite (less uniform).
GoF leans transparent; many codebases prefer safe. Name the trade-off.

**Q: Where's Composite in the JDK/real apps?**
`java.awt.Container`/`Component`, Swing component trees, the DOM, file systems, org charts, menu hierarchies.

**Q: How does it relate to recursion?**
A composite's operation typically recurses into children, which may themselves be composites — the recursion is encapsulated in the composite, not the client.

**Follow-ups**
- *"How would you avoid recomputing (e.g. total size) each call?"* — Cache the aggregate and invalidate on child changes.
- *"Composite vs Decorator?"* — Composite has many children (part-whole); Decorator wraps one component to add behavior.
- *"How to traverse without recursion?"* — Use an explicit stack/queue (DFS/BFS), or expose an Iterator.

**Traps to avoid**
- Forcing `add`/`remove` onto leaves and throwing from them (transparency's cost).
- Deep trees causing stack overflow with naive recursion.
- Ignoring the cost of repeated aggregate computations on large trees.
