# Inventory Management — LLD

Warehouse/stock system. The signals are an atomic **stock decrement** (anti-oversell) and an **Observer** for low-stock/reorder alerts.

---

## 1. Requirements

**Functional**
- Track stock per SKU across one or more warehouses.
- Add stock (restock), reserve/deduct stock (on order), release (on cancel).
- Reorder alert when stock falls below a threshold.

**Non-functional**
- Concurrent order processing must not **oversell**.
- Reorder notifications shouldn't block the stock update path.

---

## 2. Why these patterns

- **Observer** for reorder alerts: when stock crosses below threshold, notify subscribers (procurement, dashboards) without the inventory knowing who they are.
- **Strategy** (optional) for reorder policy (fixed quantity vs economic order quantity).

---

## 3. Critical flow

reserve(sku, qty): atomically check availability and deduct; if the new level is below the reorder threshold, fire a low-stock event to observers.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Concurrency | Two orders for the last units | Atomic decrement (CAS); only valid deductions commit. |
| Underflow | Deduct more than available | Reject; never go negative. |
| Threshold | Crossing the reorder line | Fire alert once per crossing (avoid alert storms). |
| Restock | Concurrent restock + deduct | Atomic add/subtract. |
| Multi-warehouse | Which warehouse to deduct from? | A routing policy (Strategy). |

---

## 5. Entities, relationships, responsibilities

- **Product / SKU**.
- **StockItem**: quantity + reorder threshold for one SKU. *Responsibility:* stock math.
- **InventoryObserver** (interface): `onLowStock(sku, qty)`. → Observer.
- **Inventory** (subject): holds stock items; reserve/restock; notifies observers.

---

## 6. Class/interface design

```
Inventory (subject)
   ├── Map<sku, StockItem (qty, threshold)>
   └── List<InventoryObserver>   OBSERVER
reserve(sku, qty): atomic deduct + maybe fire low-stock event
```

### normal vs production
- `normal/` — quantities as plain ints; reserve is check-then-subtract; observers notified synchronously.
- `production/` — quantity is an atomic value with a CAS reserve loop (anti-oversell / no-negative); observer list is concurrent; alerts are decoupled so a slow subscriber doesn't block stock updates.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you prevent overselling under concurrent orders?** *(core)*
Atomic decrement with a guard: a CAS loop that only commits `qty - n` if `qty >= n` and nobody changed it in between. Two orders for the last unit: one commits, the other sees insufficient stock and is rejected. Never read-then-write without atomicity.

**Q: How do you avoid alert storms at the threshold?**
Fire the low-stock event only on the *crossing* (when it goes from ≥ threshold to < threshold), not on every deduction below it. Track whether an alert is already outstanding until a restock clears it.

**Q: Why Observer for reorder alerts?**
The inventory shouldn't know about procurement systems, dashboards, or email. It just publishes a low-stock event; interested parties subscribe. Decoupling via Observer keeps the stock code focused (SRP).

**Follow-ups**
- *"Multi-warehouse allocation?"* — A routing Strategy decides which warehouse(s) fulfill an order (nearest, most stock, split).
- *"Reservations vs hard deductions?"* — Two-phase: reserve (soft hold with TTL) then commit on payment, like the booking problems.
- *"Distributed stock across services?"* — Atomic DB decrement (`UPDATE ... SET qty=qty-? WHERE qty>=?`) or an event-sourced ledger.

**Traps**
- Non-atomic decrement → oversell / negative stock.
- Alert storms (notifying on every deduction below threshold).
- Slow synchronous observers blocking the stock update path.
- Floating/again-recomputed stock instead of a single authoritative counter.
