# Decorator Pattern

> Attach additional responsibilities to an object **dynamically**. A flexible alternative to subclassing for extending behavior.

## What
Wrap an object in another object that adds behavior, both sharing the same interface. You can stack wrappers to combine features.

## Why it exists
Adding every feature combination via subclassing explodes: `CoffeeWithMilk`, `CoffeeWithMilkAndSugar`, `CoffeeWithSugar`... Decorators let you compose features at runtime instead of pre-declaring every combination.

## Where it's used in real apps
- Java I/O: `new BufferedReader(new InputStreamReader(new FileInputStream(f)))` — each wraps and adds behavior.
- `Collections.unmodifiableList()`, `synchronizedList()`.
- Adding caching/logging/compression around a service.

## The exact problem (see `without/`)
A `Coffee` hierarchy needs a subclass for every add-on combination. Costs and descriptions are duplicated and the class count explodes.

## How it's applied (see `with/`)
`Beverage` is the component. `MilkDecorator` and `SugarDecorator` wrap any `Beverage`, adding to its cost/description. Combine freely at runtime.

---

## Interview Questions & Traps

**Q: Why Decorator over subclassing?**
Subclassing to cover every feature combination explodes (2^n classes). Decorators compose features at runtime, so N features need N decorator classes, not 2^N subclasses.

**Q: Decorator vs Proxy?** *(trap)*
Same interface in both. Decorator **adds behavior/responsibilities**; Proxy **controls access** (lazy load, cache, auth) without changing the core behavior. Intent is the discriminator.

**Q: What's the canonical JDK example?**
`java.io` streams: `new BufferedReader(new InputStreamReader(new FileInputStream(f)))` — each wraps the previous and adds behavior. Also `Collections.unmodifiableList`/`synchronizedList`.

**Follow-ups**
- *"Does order of decorators matter?"* — Yes. Compression-then-encryption differs from encryption-then-compression; stack order changes the result.
- *"Downside of Decorator?"* — Many small similar objects; debugging a deep stack of wrappers is harder.
- *"Decorator vs Composite?"* — Composite manages many children (part-whole); a decorator wraps exactly one component to augment it.

**Traps to avoid**
- Decorators that break the component's contract (should remain substitutable — LSP).
- Confusing with Proxy (access control) or Adapter (interface change).
- Overusing it where a couple of constructor params would be simpler.
