# Visitor Pattern

> Represent an operation to be performed on the elements of an object structure. Lets you define a new operation without changing the classes of the elements.

## What
Move an operation out of the element classes into a separate **visitor** object. Each element has an `accept(visitor)` method that calls back the right `visit` method (double dispatch).

## Why it exists
When you have a stable set of element classes but keep adding new *operations* over them (export, calculate, validate...), putting each operation into every element class bloats them and violates the Single Responsibility Principle. Visitor collects one operation in one class, so adding an operation = adding a visitor, with no change to the elements.

## Trade-off (important interview point)
Visitor makes adding **operations** easy but adding **element types** hard (every visitor must gain a new `visit` method). Use it when the element hierarchy is stable and operations change often.

## Where it's used in real apps
- Compilers/ASTs (type-check visitor, codegen visitor, pretty-print visitor).
- Document object models exporting to multiple formats.
- `java.nio.file.FileVisitor`.

## The exact problem (see `without/`)
Each shape has `area()`, and now also needs `exportXml()`, soon `exportJson()`... every new operation edits every shape class.

## How it's applied (see `with/`)
Shapes accept a `ShapeVisitor`. An `AreaVisitor` and `XmlExportVisitor` each hold one operation. New operations are new visitor classes; shapes are untouched.

---

## Interview Questions & Traps

**Q: What is double dispatch and why does Visitor need it?** *(key question)*
A method call resolved by *two* runtime types: the element's type (via `accept`) and the visitor's type (via the overloaded `visit`). Java has single dispatch, so `element.accept(visitor)` calls back `visitor.visit(this)` — `this` is the concrete element type, selecting the right overload.

**Q: What's Visitor's fundamental trade-off?** *(the trap)*
Adding a new **operation** is easy (one new visitor). Adding a new **element type** is hard (every visitor needs a new `visit` method). Use Visitor only when the element hierarchy is stable and operations change often. If elements change frequently, Visitor is the wrong choice.

**Q: Why not just add methods to the element classes?**
That bloats elements with unrelated operations (violating SRP) and forces editing every element for each new operation. Visitor collects one operation in one class.

**Follow-ups**
- *"Real-world examples?"* — Compiler/AST passes (type-check, codegen, pretty-print), `java.nio.file.FileVisitor`, document export to many formats.
- *"How does Visitor break encapsulation?"* — Elements must expose enough state for visitors to work, which can leak internals.
- *"Alternatives in modern languages?"* — Pattern matching / sealed types + switch can replace Visitor more concisely.

**Traps to avoid**
- Using Visitor when element types change often (every change touches every visitor).
- Forgetting double dispatch — a single `visit(Shape)` won't resolve concrete types.
- Over-exposing element internals to satisfy visitors.
