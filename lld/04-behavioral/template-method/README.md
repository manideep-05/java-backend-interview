# Template Method Pattern

> Define the skeleton of an algorithm in a base method, deferring some steps to subclasses.

## What
A base class defines a `final` template method that calls a fixed sequence of steps. Some steps are implemented in the base; others are abstract hooks subclasses fill in.

## Why it exists
When several algorithms share the same overall structure but differ in a few steps, duplicating the whole structure in each is error-prone (a fix to the shared part must be repeated). Template Method keeps the invariant structure in one place and lets subclasses vary only the parts that differ.

## Template Method vs Strategy
- Template Method uses **inheritance** — the skeleton is fixed in a base class, steps overridden by subclasses.
- Strategy uses **composition** — the whole algorithm is swapped at runtime.

## Where it's used in real apps
- `java.util.AbstractList`, `InputStream.read()`, servlet `service()` → `doGet()/doPost()`.
- Frameworks with "lifecycle" hooks you override.
- Data-processing pipelines with fixed stages (read → process → write).

## The exact problem (see `without/`)
`CsvProcessor` and `JsonProcessor` each duplicate the full open → parse → save → close sequence. A change to "close" logic must be fixed in both.

## How it's applied (see `with/`)
`DataProcessor.process()` is a final template fixing the sequence; subclasses override only `parse()` (and optionally a hook).

---

## Interview Questions & Traps

**Q: Template Method vs Strategy?** *(trap)*
Both vary part of an algorithm. Template Method uses **inheritance** — the skeleton is fixed in a base class, subclasses override steps; chosen at compile time. Strategy uses **composition** — the whole algorithm is swapped, often at runtime.

**Q: What is a "hook" method?**
An optional step with a default (often empty) implementation that subclasses *may* override. Contrast with abstract steps subclasses *must* implement. The `afterSave()` hook in `with/` is an example.

**Q: Why make the template method `final`?**
To lock the algorithm's structure so subclasses can vary only the designated steps, not the overall sequence.

**Follow-ups**
- *"Where in the JDK?"* — `AbstractList`, `InputStream`, servlet `service()` → `doGet/doPost`, `AbstractMap`.
- *"How does this relate to the Hollywood Principle?"* — "Don't call us, we'll call you": the base class calls the subclass's steps, not the other way around.
- *"Downside?"* — Inheritance coupling; a change to the base can ripple to all subclasses. Strategy avoids this via composition.

**Traps to avoid**
- Not making the template method `final`, letting subclasses break the skeleton.
- Too many abstract steps — subclasses become hard to write; prefer sensible hooks.
- Reaching for inheritance when composition (Strategy) would be more flexible.
