# Splitwise — LLD

Tests domain modeling (balances, splits) and a clean **Strategy** for how an expense is divided. The "simplify debts" extension is a nice algorithm follow-up.

---

## 1. Requirements

**Functional**
- Users and groups. Add an **expense**: who paid, total amount, and how it's split (equal / exact / percentage).
- Track pairwise **balances** (who owes whom).
- Show a user's balances; settle up.

**Non-functional**
- Concurrent expense additions should keep balances consistent.
- Money must not drift — use integer cents and make splits sum exactly to the total.

---

## 2. Why Strategy

The *way* an expense is split varies (equal, exact amounts, percentages). A `SplitStrategy` turns "how do we divide this?" into pluggable classes — add a new split type without touching the expense/balance code (OCP).

---

## 3. Critical flow

Add expense → strategy computes each participant's share → update the balance sheet: payer is owed, each participant owes their share. Balances are stored as a map `(from, to) → amount` (or a net balance per user pair).

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Money | Equal split doesn't divide evenly ($10/3) | Distribute the remainder cents deterministically (first N people get +1 cent). |
| Validation | Exact/percent shares don't sum to total/100 | Reject the expense. |
| Self | Payer is also a participant | They owe themselves nothing; net it out. |
| Concurrency | Two expenses update the same pair's balance | Atomic balance update. |
| Settle | Settlement larger than owed | Reject or cap. |

---

## 5. Entities, relationships, responsibilities

- **User**, **Group** (a set of users).
- **Expense**: payer, amount, participants, a `SplitStrategy`.
- **Split**: a participant + their share (output of the strategy).
- **SplitStrategy** (interface): equal / exact / percentage. → **Strategy**.
- **BalanceSheet**: the ledger of who owes whom. *Responsibility:* balance math only.
- **ExpenseService** (Facade): validate + apply an expense to the balance sheet.

---

## 6. Class/interface design

```
ExpenseService
   ├── SplitStrategy (Equal / Exact / Percentage)   STRATEGY
   └── BalanceSheet { Map<(from,to), amountCents> }
Expense { payer, amountCents, participants, strategy }
```

### normal vs production
- `normal/` — balances in a nested map; add-expense computes shares and updates balances; shows remainder-cent distribution and validation.
- `production/` — balance updates are atomic (lock the balance sheet or use atomic accumulators keyed per pair) so concurrent expenses can't lose updates; validation stays strict.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you store balances?**
A directed map: `balance[A][B]` = how much A owes B (keep one signed value per unordered pair, or net on read). Avoid storing every transaction and recomputing — keep running net balances for O(1) reads.

**Q: $10 split 3 ways — where do the extra cents go?** *(classic trap)*
3 × $3.33 = $9.99, one cent short. Distribute the remainder deterministically: the first `remainder` participants pay one extra cent. Never use floating-point dollars; use integer cents so totals reconcile exactly.

**Q: Simplify debts (A→B, B→C ⇒ A→C)?**
Compute each person's net balance (sum of what they owe minus what they're owed), then greedily match the biggest creditor with the biggest debtor until all nets are zero. Minimizes the number of transactions. This is the standard "minimize cash flow" follow-up.

**Follow-ups**
- *"Concurrent expenses to the same pair?"* — Guard the balance update (lock or atomic per-pair accumulator) so two additions don't clobber each other.
- *"Multi-currency?"* — Store currency per balance; convert at settle time; don't mix.
- *"Audit/history?"* — Keep the expense log separately from the running balances (event sourcing-ish).

**Traps**
- Floating-point money (rounding drift) — use integer cents.
- Not validating that exact/percentage splits sum correctly.
- Losing remainder cents on uneven splits.
- Recomputing balances from all history on every read instead of keeping a running net.
