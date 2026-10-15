# SOLID Principles

SOLID is five object-oriented design principles that, together, make software **easier to change, test, and extend**. They were popularized by Robert C. Martin ("Uncle Bob").

The common thread: **manage dependencies and reasons-to-change**. Code that violates SOLID tends to be rigid (hard to change), fragile (breaks in surprising places), and immobile (hard to reuse).

| Letter | Principle | Core idea |
|--------|-----------|-----------|
| **S** | [Single Responsibility](1-srp) | One class, one reason to change. |
| **O** | [Open/Closed](2-ocp) | Extend behavior without editing existing code. |
| **L** | [Liskov Substitution](3-lsp) | A subclass must honor the contract of its parent. |
| **I** | [Interface Segregation](4-isp) | Many small interfaces beat one fat interface. |
| **D** | [Dependency Inversion](5-dip) | High-level code depends on abstractions, not details. |

### How they relate
- **SRP** tells you *where to draw boundaries* between classes.
- **OCP** and **LSP** tell you *how to use inheritance/polymorphism* safely across those boundaries.
- **ISP** and **DIP** tell you *how to shape the interfaces* that connect them.

Learn them in order S → O → L → I → D.
