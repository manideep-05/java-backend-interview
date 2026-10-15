# Snake and Ladders — LLD

A "model a board game" warmup. Simple rules, but interviewers watch whether you model entities cleanly and keep the game loop readable instead of one big method.

---

## 1. Requirements

**Functional**
- A board of N cells (usually 100) with **snakes** (head → lower tail) and **ladders** (bottom → higher top).
- 2+ players take turns rolling a die, advancing; land on a snake head → slide down; on a ladder bottom → climb up.
- First to reach the last cell exactly (or ≥, define it) wins.

**Non-functional**
- Mostly single-threaded (turn-based). Deterministic given a die sequence (inject the die for testability).

---

## 2. Why these choices

- Model snakes and ladders as a single **"jump" abstraction** (from → to): a snake is a jump down, a ladder is a jump up. One map `cell → destination` handles both — avoids duplicate logic.
- Inject the **Dice** (a seam) so tests can feed a fixed sequence instead of randomness.
- A thin **Game** orchestrates the turn loop; players and board are dumb data.

---

## 3. Critical flow

turn: current player rolls → newPos = pos + roll → if newPos > last, skip/stay (define) → apply any jump at newPos → update player → check win → next player.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Boundary | Roll overshoots the last cell | Define: stay put, or need exact landing. |
| Chaining | Ladder top is a snake head | Decide if jumps chain (usually not). |
| Setup | Snake/ladder on cell 1 or last | Validate board config. |
| Input | Invalid die value | Die yields 1..6 only. |
| Win | Exact-landing rule | Make it explicit. |

---

## 5. Entities, relationships, responsibilities

- **Board**: size + a `jumps` map (cell → destination for snakes/ladders). *Responsibility:* resolve a landing cell.
- **Jump**: from/to (snake = to<from, ladder = to>from). (Modeled as a map entry.)
- **Dice**: `roll()` (injectable seam).
- **Player**: name + current position.
- **Game**: the turn loop + win detection.

---

## 6. Class/interface design

```
Game
  ├── Board { size; Map<cell,dest> jumps; move(from, roll) }
  ├── Dice (interface) -> RandomDice / FixedDice (test seam)
  └── List<Player>
```

### normal vs production
- `normal/` — Board with a jumps map, injectable Dice, a clean turn loop in Game; single-threaded.
- `production/` — the realistic "production" angle for a board game is an **online multiplayer server**: `playTurn` is synchronized on the game and enforces the **turn invariant** (only the current player can move; concurrent move attempts from other players are rejected) — the same shape as the Chess production version.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you model snakes and ladders without duplicating logic?**
Unify them as a jump: a map from a cell to its destination. Snake = destination lower, ladder = destination higher. `move()` just checks "is there a jump at this cell?" — one code path for both.

**Q: How do you make the game testable despite the dice randomness?**
Inject the `Dice` as an interface. Tests pass a `FixedDice` with a scripted sequence so outcomes are deterministic; production uses `RandomDice`. (Strategy/DI — a classic "how would you test this?" follow-up.)

**Q: Overshooting the final cell?**
State the rule explicitly: either the player stays put until they roll the exact number, or any roll ≥ needed wins. Both are valid; the point is you called it out.

**Follow-ups**
- *"Multiple dice / variable board?"* — Parameterize dice count and board size; the model doesn't change.
- *"Online multiplayer concurrency?"* — Serialize turns per game and enforce whose-turn-it-is (production version).
- *"Should jumps chain?"* — Define it; usually a single jump per landing to avoid infinite loops.

**Traps**
- Separate, duplicated handling for snakes vs ladders instead of one jump map.
- Hard-coding `Math.random()` so the game can't be tested.
- Not defining the overshoot/exact-landing rule.
- A God `play()` method doing board math, win checks, and I/O all at once.
