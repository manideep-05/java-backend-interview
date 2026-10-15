# Chain of Responsibility Pattern

> Pass a request along a chain of handlers. Each handler decides either to process the request or to pass it to the next handler.

## What
A linked series of handler objects. A request enters the chain; each handler either handles it or forwards it. The sender doesn't know which handler will act.

## Why it exists
A single method with a long `if/else` deciding "who handles this?" is rigid: reordering or inserting a step means rewriting it. A chain lets you add, remove, or reorder handlers independently, each with a single responsibility.

## Where it's used in real apps
- Servlet filters / middleware pipelines (Express, ASP.NET).
- Logging levels (DEBUG → INFO → ERROR handlers).
- Approval workflows (team lead → manager → director by amount).

## The exact problem (see `without/`)
An `ExpenseApprover` has nested `if/else` on amount to pick an approver. Adding a tier or changing thresholds rewrites the method.

## How it's applied (see `with/`)
Each `Approver` handles up to its limit or passes to the next. The chain is assembled by linking handlers; adding a tier means adding a handler and linking it.

---

## Interview Questions & Traps

**Q: What happens if no handler handles the request?**
Design decision: either silently drop it, reach a default/fallback handler, or throw. Always state what the end-of-chain behavior is — interviewers probe this.

**Q: Must exactly one handler process the request?**
No. Variants exist: the first capable handler stops the chain, OR every handler gets a chance (like a filter pipeline where each may act and still pass along). Clarify which semantics you mean.

**Q: Where's this in real frameworks?**
Servlet filters, Spring Security filter chain, middleware (Express/ASP.NET), logging level handlers.

**Follow-ups**
- *"CoR vs Decorator?"* — Both are chains of wrappers. CoR: a handler may *stop* the chain (handle and return). Decorator: every wrapper always delegates and augments.
- *"How do you build the chain?"* — Link handlers manually (as in `with/`), or configure/order them via a list/DI.
- *"Performance concern?"* — A long chain adds per-request overhead; order handlers by likelihood/cost.

**Traps to avoid**
- No fallback for an unhandled request.
- Hidden ordering dependencies between handlers.
- Infinite loops if a handler forwards back into the chain.
