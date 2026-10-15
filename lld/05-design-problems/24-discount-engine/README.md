# Discount / Coupon Engine — LLD

A rules-driven pricing problem. Heavy on **Strategy** and **Chain of Responsibility**, and a great test of extensibility (new discount types without touching the engine) — OCP in action.

---

## 1. Requirements

**Functional**
- Apply discounts to a cart: percentage off, flat amount off, buy-X-get-Y, coupon codes.
- Discounts may **stack** (apply in sequence) or be **exclusive** (best-one-wins).
- Rules/eligibility: min cart value, specific categories, one-time coupon use.

**Non-functional**
- New discount types must be addable without editing the engine (OCP).
- Deterministic, order-defined stacking.
- Money as integer cents; results reconcile exactly.

---

## 2. Why these patterns

- **Strategy**: each discount is a `Discount` with `apply(cart) -> amountOff`. Add a type = add a class.
- **Chain of Responsibility / pipeline**: the engine runs discounts in order, each reducing the running total (stacking), or picks the best (exclusive).
- **Specification** (eligibility): a predicate deciding whether a discount applies to this cart.

---

## 3. Critical flow

price(cart): start with subtotal → for each applicable discount in order, compute its reduction and subtract (stacking mode) → OR evaluate all and keep the largest (exclusive mode) → never go below zero → return final total + breakdown.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Order | Stacking order changes the result | Define a deterministic order/priority. |
| Floor | Discount exceeds total | Clamp final to ≥ 0. |
| Eligibility | Coupon below min cart value | Skip (not applicable). |
| Exclusivity | Two coupons, only best allowed | Exclusive mode picks max. |
| Reuse | One-time coupon applied twice | Mark used (idempotency). |
| Money | Rounding on percentages | Integer cents, defined rounding. |

---

## 5. Entities, relationships, responsibilities

- **Cart / LineItem**: items, quantities, subtotal.
- **Discount** (interface): `isApplicable(cart)` + `computeDiscount(currentTotal, cart)`. → Strategy + Specification.
- **PercentageDiscount / FlatDiscount / ...**: concrete rules.
- **DiscountEngine**: runs the pipeline (stack or best-of), clamps, returns a breakdown. *Responsibility:* orchestration + money math.

---

## 6. Class/interface design

```
DiscountEngine
   ├── List<Discount> (ordered)        STRATEGY instances
   └── apply(cart): stack or best-of, clamp >= 0
Discount (interface): isApplicable(cart), computeDiscount(total, cart)
   PercentageDiscount / FlatDiscount / ...
```

### normal vs production
- `normal/` — Discount interface + a couple of concrete discounts; engine stacks them in order with a floor at zero; integer cents.
- `production/` — engine is thread-safe for shared config (discounts list is immutable/`CopyOnWriteArrayList`); one-time **coupon usage tracked atomically** (a used-set with `add()` returning false on reuse) so a coupon can't be redeemed twice under concurrent checkouts.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you add a new discount type without touching the engine?**
Each discount implements the `Discount` interface; the engine iterates the abstraction. A new type is a new class registered into the list — zero engine edits. Textbook Strategy + OCP.

**Q: Stacking vs exclusive discounts — how do you model both?**
Stacking: run discounts in a defined order, each reducing the running total (a pipeline / Chain of Responsibility). Exclusive: evaluate each independently against the subtotal and keep the maximum. Make the mode explicit; order matters for stacking, so define a priority.

**Q: A coupon must be single-use — how under concurrency?**
Track redeemed coupon codes in a concurrent set; redemption does `usedCodes.add(code)` and only proceeds if it returns true (the code wasn't already present). That's an atomic check-then-act, so two concurrent checkouts can't both redeem the same one-time coupon. (Same idempotency idea as your payment work.)

**Follow-ups**
- *"Eligibility rules (min value, category)?"* — A Specification predicate per discount; `isApplicable(cart)` gates it.
- *"How to avoid negative totals / rounding drift?"* — Clamp at zero; integer cents with a defined rounding rule for percentages.
- *"Audit which discounts applied?"* — Return a breakdown (list of applied discount + amount), not just the final number.

**Traps**
- `if (type == PERCENT) ... else if (type == FLAT) ...` in the engine instead of Strategy.
- Floating-point money.
- Letting a discount push the total negative.
- Non-atomic one-time-coupon check (double redemption).
