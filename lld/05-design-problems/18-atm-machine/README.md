# ATM Machine — LLD

Another **State pattern** classic, with a money/account twist that invites a concurrency follow-up.

---

## 1. Requirements

**Functional**
- Insert card → enter PIN → select operation (balance / withdraw / deposit) → eject card.
- Withdraw: check balance, check ATM cash, dispense, debit account.
- Authenticate PIN (limited attempts).

**Non-functional**
- An account may be hit from multiple ATMs/channels at once → debits must be atomic (no overdraft via race).
- Keep the hardware flow (card, keypad, dispenser) decoupled from banking logic.

---

## 2. Why State pattern

The ATM is a lifecycle: `Idle → CardInserted → Authenticated → (operation) → Idle`. What's legal depends on the state (you can't withdraw before authenticating). State classes encode legal operations + transitions.

---

## 3. Critical flows

1. **Withdraw**: authenticated → request amount → verify account balance AND atm cash → debit account → dispense → idle.
2. **Auth**: wrong PIN increments attempts; too many → retain/eject card.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Money | Withdraw > balance | Reject. |
| Hardware | ATM out of cash / can't make denomination | Reject before debit. |
| Security | 3 wrong PINs | Lock/eject. |
| Concurrency | Two withdrawals on one account | Atomic debit (lock/CAS) — no overdraft. |
| Partial failure | Debited but dispense jams | Must compensate/reverse — mention transactions. |
| Invalid op | Withdraw before auth | State rejects. |

---

## 5. Entities, relationships, responsibilities

- **ATM** (context): current `ATMState`, inserted card, cash on hand. Delegates to state.
- **ATMState** (interface): `insertCard`, `enterPin`, `withdraw`, `ejectCard`.
- **IdleState / HasCardState / AuthenticatedState**: lifecycle.
- **Account**: balance; `debit` / `credit`. *The money source of truth.*
- **BankService**: authenticate, fetch account. *Responsibility:* banking, separate from hardware.

---

## 6. Class/interface design

```
ATM (context) --> ATMState (Idle/HasCard/Authenticated)   STATE
   ├── BankService (auth, accounts)
   └── cashOnHand
Account { balance; debit(); credit(); }
```

### normal vs production
- `normal/` — State pattern, single-threaded; `account.debit()` is a simple check-then-subtract.
- `production/` — `Account.debit()` is atomic (synchronized / CAS loop) so concurrent withdrawals can't overdraft; the withdraw operation verifies-and-debits as one critical section; ATM cash is guarded.

---

## Interview Questions, Follow-ups & Traps

**Q: Where's the overdraft race?** *(the key follow-up)*
`if (balance >= amount) balance -= amount` is check-then-act. Two concurrent withdrawals can both pass the check on the same balance and both debit → overdraft. Fix: make verify+debit atomic (lock the account or CAS the balance).

**Q: Debited but the dispenser jams — now what?**
That's a partial-failure/atomicity problem. In a real system the debit + dispense are a transaction; if dispense fails you reverse (credit back) or hold the funds and reconcile. Say you'd use a transactional/saga approach.

**Q: Why separate BankService from ATM?**
SRP + DIP: hardware (card/keypad/cash) changes for different reasons than banking rules; the ATM depends on a `BankService` abstraction so it's testable with a fake.

**Follow-ups**
- *"Multiple ATMs on one account."* — The account is shared state; its debit must be atomic across all callers (DB row lock / optimistic version in reality).
- *"PIN lockout policy?"* — Track attempts on the card/session; lock after N.
- *"Denomination dispensing?"* — Greedy by note value; reject if exact amount can't be formed.

**Traps**
- Non-atomic debit (overdraft race).
- Putting banking logic inside ATM states (God object).
- Dispensing before confirming both balance and cash availability.
- Ignoring the debit/dispense partial-failure case.
