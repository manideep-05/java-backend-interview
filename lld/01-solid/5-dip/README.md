# Dependency Inversion Principle (DIP)

> High-level modules should not depend on low-level modules. **Both should depend on abstractions.** Abstractions should not depend on details; details should depend on abstractions.

## What
Your business logic (high-level) should not reach out and `new` a concrete database/email/gateway (low-level). Instead, both talk through an interface, and the concrete choice is injected from outside.

## Why it exists
When high-level code hard-codes a concrete dependency:
- You can't swap implementations (MySQL → Postgres, SMTP → SendGrid).
- You can't unit-test the business logic without the real dependency (real DB, real network).
- A change in the low-level detail ripples up into stable business code.

## DIP vs Dependency Injection
- **DIP** is the *principle*: depend on abstractions.
- **Dependency Injection (DI)** is a common *technique* to achieve it: pass the concrete dependency in (via constructor) rather than creating it inside.

## Where it's used in real apps
- Spring's `@Autowired` beans, Repository interfaces with swappable implementations.
- Any code that takes an interface in its constructor so tests can pass a mock/fake.

## The exact problem (see `without/`)
`NotificationService` directly creates `new EmailSender()`. It is welded to email and to that concrete class — untestable and unswappable.

## How it's applied (see `with/`)
Define a `MessageSender` abstraction. `NotificationService` depends on it and receives the concrete sender via its constructor. Swapping to SMS, or injecting a fake in tests, needs no change to `NotificationService`.

---

## Interview Questions & Traps

**Q: State DIP.**
High-level modules and low-level modules should both depend on abstractions, not on each other. Abstractions shouldn't depend on details; details depend on abstractions.

**Q: Is DIP the same as Dependency Injection?** *(classic trap)*
No. DIP is the *principle* (depend on abstractions). DI is a *technique* (inject the dependency, usually via constructor) that helps you achieve DIP. You can do DI without truly inverting dependencies, and you can satisfy DIP via other means (service locator, factories).

**Q: What's the "inversion" in Dependency Inversion?**
Traditionally high-level code depends on concrete low-level code. DIP inverts the dependency arrow so both point at an abstraction that the high-level module owns/defines.

**Follow-ups**
- *"Who should own the abstraction?"* — Ideally the high-level module defines the interface it needs; the low-level module implements it. This is the real inversion.
- *"How does DIP enable testing?"* — Inject a mock/fake implementation of the abstraction; no real DB/network needed.
- *"DIP vs OCP?"* — DIP (depend on abstractions) is often what makes OCP (extend without modifying) possible.

**Traps to avoid**
- `new`-ing a concrete dependency inside a high-level class (hard-wired dependency) — the exact smell DIP removes.
- Creating an interface but still letting the high-level module construct the concrete type itself.
- Over-abstracting: not every dependency needs an interface (e.g. stable value types like `String`). Abstract across *boundaries that change or need swapping*.
