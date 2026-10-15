# E-commerce Checkout & Payment — LLD

A broad problem; the senior signal is how you handle **order state**, **payment idempotency**, and **stock decrement** under concurrency.

---

## 1. Requirements

**Functional**
- Cart: add/remove items with quantities.
- Checkout: create an **order** from the cart, apply pricing/discount, take **payment**, decrement stock, confirm.
- Order lifecycle: CREATED → PAID → CONFIRMED (or FAILED/CANCELLED).

**Non-functional**
- Payment must be **idempotent** (network retries must not double-charge).
- Stock decrement must not **oversell** under concurrent checkouts.
- Payment provider is pluggable.

---

## 2. Why these patterns

- **State** for the order lifecycle (what's legal depends on the order's state).
- **Strategy** for payment method (card / wallet / UPI) and for pricing/discount.
- A thin **Facade** (`CheckoutService`) orchestrates the steps.

---

## 3. Critical flow

cart → createOrder (price it) → reserve/decrement stock → charge payment (idempotent) → mark PAID → CONFIRMED. On any failure, compensate (release stock, don't confirm).

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Idempotency | Client retries the charge | Idempotency key → return the original result, don't re-charge. |
| Concurrency | Two orders for the last unit | Atomic stock decrement; one fails gracefully. |
| Partial | Paid but stock reservation failed (or vice versa) | Reserve stock BEFORE charging; on charge failure, release stock. |
| Invalid state | Pay an already-paid order | State rejects. |
| Empty | Checkout an empty cart | Reject. |
| Price change | Price changed between cart and checkout | Re-price at checkout; show the user. |

---

## 5. Entities, relationships, responsibilities

- **Cart** → **CartItem** (product + qty).
- **Order** → **OrderItem**; has an `OrderState`.
- **OrderState** (interface): CREATED / PAID / CONFIRMED / CANCELLED. → **State**.
- **PaymentStrategy** (interface): charge(amount, idempotencyKey). → **Strategy**.
- **Inventory**: atomic stock reserve/release.
- **CheckoutService** (Facade): orchestrates the flow + compensation.

---

## 6. Class/interface design

```
CheckoutService (Facade)
   ├── Inventory (atomic reserve/release)
   ├── PaymentStrategy (Card/Wallet)   STRATEGY
   └── Order --> OrderState            STATE
```

### normal vs production
- `normal/` — straight-line checkout; stock decrement is a simple subtract; order state transitions via State pattern.
- `production/` — stock reserve is atomic (CAS) and done **before** charging; payment carries an **idempotency key** cached so retries return the same result; compensation releases stock on failure.

---

## Interview Questions, Follow-ups & Traps

**Q: Why reserve stock before charging payment?** *(ordering trap)*
If you charge first and then can't fulfill (out of stock), you've taken money for nothing and must refund — bad. Reserve stock first; only charge once you can fulfill. If the charge fails, release the reservation.

**Q: How do you make payment idempotent?**
The client sends an idempotency key per checkout attempt. The payment layer records (key → result); a retry with the same key returns the stored result instead of charging again. Essential because networks retry.

**Q: Prevent overselling the last item under concurrency?**
Atomic stock decrement (CAS loop or DB conditional update `SET qty=qty-1 WHERE qty>0`). Two concurrent checkouts for the last unit: one succeeds, the other is rejected and never charged.

**Follow-ups**
- *"Distributed transaction across payment + inventory?"* — You can't 2PC easily across services; use a **saga** with compensating actions (release stock, refund) and make steps idempotent.
- *"Discounts/coupons?"* — A pricing Strategy / chain of pricing rules applied at checkout.
- *"Order timeout if payment never completes?"* — Expire CREATED orders and release reserved stock.

**Traps**
- Charging before confirming stock (refund mess).
- Non-idempotent payment (double charge on retry).
- Non-atomic stock decrement (oversell).
- Letting the order jump states illegally (confirm an unpaid order).
