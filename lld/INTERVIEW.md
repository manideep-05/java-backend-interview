# SDE2 Interview Guide — SOLID & Design Patterns

A cross-cutting cheat sheet. Each topic folder also has its own **Interview Questions & Traps** section; this page covers the questions that span topics and how to handle an LLD round.

---

## How to approach a "which pattern?" question

Interviewers rarely ask "define X." They describe a messy design and ask how you'd improve it, or they ask you to design something (parking lot, rate limiter, elevator). A reliable sequence:

1. **Name the pain, not the pattern.** Say what's wrong first: "this class has three reasons to change," "this switch grows with every new type." Patterns are the cure; lead with the disease.
2. **Pick the smallest fix.** Don't reach for Abstract Factory when a single Factory Method or even a plain constructor is enough. Over-engineering is a red flag.
3. **State the trade-off.** Every pattern adds indirection. Call it out: "this adds a class per type, but it removes the switch and makes it open/closed."
4. **Mention the real-world analogue.** "This is how `Comparator` works with `Collections.sort`." Shows you've seen it in practice, not just in a book.

---

## The comparison questions interviewers love (and the trap in each)

| Question | Short answer | The trap |
|----------|-------------|----------|
| **Strategy vs State** | Both delegate to a swappable object. Strategy = client picks the algorithm, strategies are independent. State = object transitions between states, states know each other. | Saying "they're the same." They have identical structure but opposite intent. |
| **Factory Method vs Abstract Factory** | Factory Method creates **one** product via subclass override. Abstract Factory creates a **family** of related products via a factory object. | Calling a simple static helper that returns objects a "Factory pattern." That's a *simple/static factory*, not the GoF Factory Method. |
| **Decorator vs Proxy** | Same interface in both. Decorator **adds behavior**; Proxy **controls access** (lazy, cache, auth). | Both wrap an object with the same interface, so people mix them up. Intent is the discriminator. |
| **Decorator vs Adapter** | Decorator keeps the interface and adds behavior. Adapter **changes** the interface to make two things fit. | Thinking "wrapping = adapter." Adapter changes the interface; decorator doesn't. |
| **Bridge vs Strategy** | Structurally similar (composition). Bridge separates an abstraction from its implementation along two dimensions, decided up front. Strategy swaps an algorithm, often at runtime. | Bridge is structural (how things are composed); Strategy is behavioral (what algorithm runs). |
| **Adapter vs Facade** | Adapter makes one incompatible interface fit an expected one. Facade simplifies a whole subsystem behind one easy interface. | Facade can wrap many classes; Adapter typically wraps one and matches a specific target interface. |
| **Template Method vs Strategy** | Both vary part of an algorithm. Template Method uses **inheritance** (fixed skeleton in a base class). Strategy uses **composition** (swap the whole algorithm). | Template Method can't change the skeleton at runtime; Strategy can swap entirely. |
| **Composite vs Decorator** | Both are recursive tree-ish structures. Composite = part-whole hierarchy, treat leaf and group uniformly. Decorator = one child, add behavior. | A decorator wraps exactly one component; a composite holds many children. |

---

## SOLID: the questions that come up every time

- **"Give a real example of an SRP violation you've seen."** Have a concrete story (a God service/controller). Generic answers sound memorized.
- **"Doesn't SRP mean one method per class?"** No — one *reason to change*, which can be several cohesive methods. This is the #1 SRP misconception.
- **"How does OCP relate to the other principles?"** OCP is often *achieved* via DIP (depend on an abstraction) and polymorphism; Strategy/Factory are common vehicles.
- **"Is `Square extends Rectangle` always wrong?"** It violates LSP *if* Rectangle's contract lets width and height vary independently. The lesson is about contracts, not geometry.
- **"Isn't DIP just dependency injection?"** DIP is the principle (depend on abstractions). DI is one technique to satisfy it. Don't conflate them.

---

## General traps across all design patterns

- **Pattern-itis / over-engineering.** Applying a pattern where a plain class or function is clearer. Interviewers probe this deliberately. The correct answer is sometimes "no pattern needed."
- **Reciting structure, not intent.** Knowing the UML but not *when* to use it. Always tie the pattern to the problem it removes.
- **Ignoring trade-offs.** Every pattern adds indirection/complexity. Naming the cost signals maturity.
- **Thread safety.** Singletons, lazy init, shared Flyweights, and Observer notification can all be asked about under concurrency.
- **Confusing GoF Factory Method with a "simple factory."** A static method returning subtypes is the simple factory idiom, not the GoF pattern.
- **Memory leaks via listeners.** Observer: forgetting to unregister observers is a classic leak (and a follow-up question).

---

## A quick LLD walkthrough script

When asked to design something low-level:

1. **Clarify requirements & scope** (functional + a couple of non-functional like concurrency, scale).
2. **Identify the core entities** and their responsibilities (this is SRP in action).
3. **Find the axes of change** — where will new types/behaviors be added? Those are your pattern candidates (OCP, Strategy, Factory).
4. **Sketch interfaces first**, concretes second (DIP, ISP).
5. **Call out patterns by name** as you introduce them, with the trade-off.
6. **Mention concurrency & edge cases** proactively.

Keep it iterative and think out loud — the interviewer is grading your reasoning, not just the final diagram.
