# Food Delivery — LLD

Think DoorDash/Swiggy core. Combines an **order lifecycle (State)**, **agent assignment** (like ride matching), and pluggable fee/assignment **Strategy**.

---

## 1. Requirements

**Functional**
- Customer places an order from a restaurant (items).
- Restaurant accepts/prepares; a **delivery agent** is assigned; agent picks up and delivers.
- Order lifecycle: PLACED → ACCEPTED → PREPARING → READY → ASSIGNED → PICKED_UP → DELIVERED (plus CANCELLED).

**Non-functional**
- One agent handles one active delivery at a time → assignment must be race-free.
- Pluggable assignment (nearest/least-busy) and delivery-fee calculation.

---

## 2. Why these patterns

- **State** for the order lifecycle (legal transitions only).
- **Strategy** for agent assignment and fee calculation.
- A **Facade** (`OrderService`) orchestrates placement → assignment → delivery.

---

## 3. Critical flow

place order → restaurant accepts → when READY, atomically assign a free agent (mark agent BUSY, order ASSIGNED) → PICKED_UP → DELIVERED → agent FREE again.

The "pick a free agent and mark busy" is the critical section (same shape as ride matching).

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Concurrency | Two READY orders grab the same agent | Atomic agent claim (CAS FREE→BUSY). |
| None | No free agents | Order waits in a READY/UNASSIGNED queue; retry on agent free. |
| Cancel | Customer cancels mid-flow | Only legal before PICKED_UP; free the agent. |
| Invalid state | Deliver before pickup | State rejects. |
| Restaurant | Restaurant rejects order | PLACED → CANCELLED. |

---

## 5. Entities, relationships, responsibilities

- **Order** → items, restaurant, customer, `OrderState`.
- **OrderState** (interface/enum): the lifecycle. → State.
- **DeliveryAgent** (id, location, status FREE/BUSY).
- **AssignmentStrategy** (interface): pick an agent. → Strategy.
- **OrderService** (Facade): placement + transitions + assignment.

---

## 6. Class/interface design

```
OrderService (Facade)
   ├── AssignmentStrategy (NearestAgent)   STRATEGY
   └── Order --> OrderState                STATE
DeliveryAgent { status }  (atomic claim)
```

### normal vs production
- `normal/` — order transitions via guarded enum; assignment scans free agents, picks one, marks busy; single-threaded.
- `production/` — agent status is atomic (CAS FREE→BUSY) so two orders can't share an agent; order state transitions are guarded; agent freed on terminal/cancel.

---

## Interview Questions, Follow-ups & Traps

**Q: How is agent assignment different from ride matching?**
It's the same core: pick a free resource and claim it atomically. The difference is the richer order lifecycle before and after assignment (restaurant accept, prepare, pickup).

**Q: What if no agent is free when an order is READY?**
Queue the order as unassigned; when an agent transitions to FREE, pull the next waiting order (producer/consumer). Don't block the restaurant flow.

**Q: Why State for the order?**
Many phases with strict legal transitions (can't deliver before pickup, can't cancel after pickup). State (or enum-with-guards) centralizes those rules instead of scattering `if (status == ...)` checks.

**Follow-ups**
- *"Batch deliveries (one agent, multiple nearby orders)?"* — Relax one-agent-one-order; the agent holds a small route; assignment groups nearby orders.
- *"ETA / live tracking?"* — Separate concern; emit location events (Observer) consumers subscribe to.
- *"Surge delivery fee?"* — A fee Strategy with a demand multiplier.

**Traps**
- Non-atomic agent claim (one agent, two orders).
- Illegal order transitions (deliver before pickup).
- Not freeing the agent on cancel/deliver (agent stuck BUSY).
- Blocking the restaurant when no agent is free instead of queueing.
