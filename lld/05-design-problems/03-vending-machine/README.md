# Vending Machine — LLD

The textbook **State pattern** problem. Interviewers use it to see if you can model a lifecycle cleanly instead of with nested `if`s.

---

## 1. Requirements

**Functional**
- Select a product; insert money; dispense product + change; cancel for a refund.
- Track inventory per slot and available change.
- Reject when: product out of stock, insufficient money, invalid selection.

**Non-functional**
- Single physical machine, but concurrent signals possible (coin sensor + buttons). Keep state transitions consistent.

---

## 2. Why State pattern

The machine behaves differently depending on where it is in the flow:
- In **Idle**: you can select a product.
- In **HasMoney**: you can insert more, or dispense when enough.
- In **Dispensing**: inserting money is rejected.

Modeling this with `if (state == X && ...)` in every method produces duplicated, fragile branching. **State** gives each phase its own class that defines what's legal and what the next state is.

---

## 3. Critical flow

`Idle → select product → ReadyState → insert coins → (enough?) → Dispense → change returned → Idle`.
Cancel at any pre-dispense state → refund inserted money → Idle.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Stock | Selected product sold out | Reject selection in Idle. |
| Money | Not enough inserted | Stay in HasMoney; wait or allow cancel. |
| Change | Can't make exact change | Reject before dispensing; refund. |
| Invalid op | Insert money while dispensing | State rejects it. |
| Cancel | Refund mid-flow | Each pre-dispense state supports cancel. |
| Concurrency | Coin + button at once | Guard the transition (production). |

---

## 5. Entities, relationships, responsibilities

- **VendingMachine** (context): holds current `State`, inventory, inserted amount. Delegates operations to the state.
- **State** (interface): `selectProduct`, `insertMoney`, `dispense`, `cancel`. Each concrete state implements legal ops and transitions.
- **IdleState / HasMoneyState / DispensingState**: the lifecycle.
- **Inventory**: product → (count, price). *Responsibility:* stock bookkeeping.
- **Product**: value object.

---

## 6. Class/interface design

```
VendingMachine (context)
   ├── State current  --> Idle / HasMoney / Dispensing   (STATE pattern)
   ├── Inventory
   └── insertedAmount
```

### normal vs production
- `normal/` — classic State pattern, single-threaded. States drive transitions.
- `production/` — operations are **synchronized on the machine** so a coin event and a button press can't interleave into an inconsistent state; inventory decrement is guarded; `volatile`/guarded current-state reference.

---

## Interview Questions, Follow-ups & Traps

**Q: Why State over a big enum + switch?**
State localizes each phase's behavior and transitions in one class, so adding a phase (e.g. "Maintenance") doesn't mean editing every method's switch. It directly serves OCP.

**Q: Who decides the next state — the state or the machine?**
Typically the current state decides and calls `machine.setState(next)`, keeping transition rules cohesive per state (shown here). A central transition table is the alternative; mention the trade-off.

**Q: State vs Strategy here?**
Strategy would swap an interchangeable algorithm chosen by the client. State models the machine *moving through* a lifecycle where states know their successors — that's the right fit.

**Follow-ups**
- *"Make change optimally."* — Greedy by denomination (works for canonical coin systems); mention it can fail for arbitrary denominations (then DP).
- *"Concurrent coin + button."* — Serialize operations on the machine (one lock) so transitions are atomic.
- *"Persist state across power cycles?"* — Externalize current state + inserted amount (Memento-ish snapshot).

**Traps**
- Letting the machine (context) keep giant switches anyway (half-applied State).
- Allowing illegal ops (insert money while dispensing) because states don't reject them.
- Dispensing before confirming change can be made.
- Not refunding on cancel.
