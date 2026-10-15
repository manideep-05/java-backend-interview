# Observer Pattern

> Define a one-to-many dependency so that when one object changes state, all its dependents are notified automatically.

## What
A **subject** maintains a list of **observers** and notifies them whenever its state changes. Observers subscribe/unsubscribe freely.

## Why it exists
Without it, the subject must know and directly call every interested party — tight coupling, and adding a new listener means editing the subject. Observer inverts this: the subject only knows the `Observer` interface.

## Where it's used in real apps
- GUI event listeners, `java.util.Observable` (legacy), `PropertyChangeListener`.
- Reactive streams (`Flow.Publisher`/`Subscriber`), message buses.
- A stock ticker notifying dashboards; model→view updates.

## The exact problem (see `without/`)
`WeatherStation` hard-codes calls to each display (`phoneDisplay.update(...); tvDisplay.update(...)`). Adding a display edits the station.

## How it's applied (see `with/`)
`WeatherStation` keeps a list of `Observer`s and calls `notifyObservers()`. Displays register themselves. Adding a display requires no change to the station.

---

## Interview Questions & Traps

**Q: What's the biggest real-world pitfall with Observer?** *(key trap)*
**Memory leaks** (lapsed listener): if an observer never unregisters, the subject keeps a strong reference to it, preventing garbage collection. Fix: always unregister, or hold observers via `WeakReference`.

**Q: Push vs pull model?**
Push: the subject sends the changed data to observers (`update(data)`). Pull: the subject just signals "something changed" and observers query it for what they need. Push is simpler; pull decouples observers from the exact data shape.

**Q: Why is `java.util.Observable` deprecated?**
It's a class (not an interface, so it eats your single inheritance), not thread-safe, and clunky. Modern code uses `PropertyChangeListener`, listener interfaces, or reactive streams (`Flow.Publisher`/`Subscriber`).

**Follow-ups**
- *"Concurrency concerns?"* — Notifying while the observer list is being modified causes `ConcurrentModificationException`; copy the list before iterating or use a concurrent collection.
- *"Order of notification?"* — Not guaranteed; don't let observers depend on each other's ordering.
- *"What if an observer throws during notify?"* — It can break the whole notification loop; isolate/catch per observer if robustness matters.

**Traps to avoid**
- Forgetting to unregister observers (the classic leak).
- Observers triggering further updates causing cascades/infinite loops.
- Assuming synchronous, ordered delivery.
