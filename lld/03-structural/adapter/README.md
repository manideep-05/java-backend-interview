# Adapter Pattern

> Convert the interface of a class into another interface clients expect. Lets classes with incompatible interfaces work together.

## What
A wrapper that translates calls from the interface your code expects into the interface an existing/third-party class actually provides.

## Why it exists
You can't (or shouldn't) edit third-party or legacy code, yet it doesn't match the interface your app is built around. An adapter bridges the two without changing either side.

## Where it's used in real apps
- `java.util.Arrays.asList()`, `InputStreamReader` (bytes → chars).
- Wrapping a third-party payment SDK to fit your own `PaymentGateway` interface.
- Spring's `HandlerAdapter`.

## The exact problem (see `without/`)
Your app speaks `MediaPlayer.play(file)`, but you've got a third-party `AdvancedAudioLib` whose method is `playMp3(path)`. The signatures don't line up, so the app can't call it directly.

## How it's applied (see `with/`)
`AudioAdapter` implements your `MediaPlayer` interface and internally delegates to the third-party `AdvancedAudioLib`, translating the call. Your app depends only on `MediaPlayer`.

---

## Interview Questions & Traps

**Q: Object adapter vs class adapter?**
- **Object adapter** (used in `with/`): holds the adaptee via composition. Flexible — can adapt subclasses too, and Java-friendly (no multiple inheritance needed). Preferred.
- **Class adapter**: inherits from the adaptee. In Java this needs the adaptee to be a class and only works via single inheritance, so it's limited.

**Q: Adapter vs Decorator vs Proxy?** *(classic trap)*
All wrap an object. Adapter **changes the interface** to make two things fit. Decorator keeps the interface and **adds behavior**. Proxy keeps the interface and **controls access**.

**Q: Adapter vs Facade?**
Adapter converts one interface to a specific expected interface. Facade invents a new simplified interface over a whole subsystem; it's not matching a pre-existing target.

**Follow-ups**
- *"JDK examples?"* — `InputStreamReader` (bytes → chars), `Arrays.asList()`, `java.io` wrappers.
- *"Two-way adapter?"* — An adapter implementing both interfaces so either side can call the other.
- *"Where would you place the adapter in a layered app?"* — At the boundary (anti-corruption layer) so third-party types don't leak into your domain.

**Traps to avoid**
- Letting the adaptee's types leak through the adapter into client code.
- Putting business logic in the adapter — it should only translate.
- Confusing it with Decorator (adapter changes the interface, decorator doesn't).
