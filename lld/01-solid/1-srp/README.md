# Single Responsibility Principle (SRP)

> A class should have **only one reason to change**.

## What
A "responsibility" is a reason to change. If a class does report generation **and** database persistence **and** email sending, it has three reasons to change — three different stakeholders (a product manager, a DBA, an ops engineer) can each force you to edit it.

## Why it exists
When unrelated responsibilities live in one class:
- A change for one reason risks breaking the others (fragility).
- The class is hard to test (you can't test formatting without a database).
- It can't be reused (you can't reuse the formatter without dragging email code along).

## Where it's used in real apps
- Separating **Controller → Service → Repository** layers in Spring/Node apps.
- Keeping DTOs (data) separate from services (behavior).
- Splitting a "God class" `UserManager` into `UserValidator`, `UserRepository`, `UserNotifier`.

## The exact problem (see `without/`)
`Employee` computes pay, saves itself to a DB, and formats a report. Three teams, one class. A change to the report format forces a recompile/redeploy of payroll logic.

## How it's applied (see `with/`)
Split by reason-to-change:
- `Employee` — holds data only.
- `PayCalculator` — business rule for pay.
- `EmployeeRepository` — persistence.
- `EmployeeReportFormatter` — presentation.

Each class now has exactly one reason to change.

---

## Interview Questions & Traps

**Q: Define SRP in one sentence.**
A class should have only one reason to change — one responsibility owned by one actor/stakeholder.

**Q: Does SRP mean "one method per class"?** *(the #1 trap)*
No. It means one *reason to change*. A class can have many cohesive methods that all serve the same responsibility. SRP is about cohesion, not method count.

**Q: How do you identify a responsibility?**
Ask "who requests changes to this?" If a report change comes from the design team and a persistence change from the DBA, those are two responsibilities. Responsibilities map to actors/stakeholders.

**Follow-ups**
- *"Isn't splitting into many classes just more complexity?"* — There's a balance. Over-splitting ("nano-classes") hurts readability too. Split along genuine axes of change, not arbitrarily.
- *"How does SRP relate to the other SOLID principles?"* — It tells you *where to draw class boundaries*; the others tell you how to connect them. SRP violations often cause OCP violations too.
- *"How does SRP help testing?"* — Each responsibility can be unit-tested in isolation (test the formatter without a DB).

**Traps to avoid**
- Confusing SRP with "do one thing" at the method level — SRP is a *class/module*-level principle.
- Creating anemic classes with no behavior just to "follow SRP"; cohesion matters.
- Hidden responsibilities: logging, validation, and persistence sneaking into a domain class.
