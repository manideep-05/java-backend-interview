# Tic-Tac-Toe — LLD

The smallest "model a game" problem. Interviewers use it to check clean board modeling, win-detection logic, and extensibility (generalize to N×N / K-in-a-row).

---

## 1. Requirements

**Functional**
- 3×3 board (generalize to N×N), two players (X and O) alternating.
- Place a mark on an empty cell; reject invalid/occupied cells.
- Detect **win** (row/column/diagonal) and **draw** (board full, no winner).

**Non-functional**
- Mostly single-threaded. Online version → turn integrity.
- Win check should be efficient (O(N) per move, not O(N²) rescans).

---

## 2. Why these choices

- Model the board as an N×N grid of an enum (`EMPTY/X/O`) — type-safe, no magic chars.
- **Incremental win check**: after a move at (r,c), only check that row, that column, and (if on a diagonal) the diagonals — O(N), not a full-board scan.
- A thin **Game** owns turn alternation + state (IN_PROGRESS/WON/DRAW).

---

## 3. Critical flow

move(player, r, c): validate it's player's turn and (r,c) is empty → place mark → incremental win check around (r,c) → update game state → switch turn.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Invalid | Occupied / out-of-bounds cell | Reject the move. |
| Turn | Wrong player moves | Reject. |
| Terminal | Move after game over | Reject. |
| Draw | Board full, no line | Detect via move count == N*N. |
| Efficiency | Win detection cost | Incremental O(N) check, not O(N²). |

---

## 5. Entities, relationships, responsibilities

- **Mark** (enum): EMPTY / X / O.
- **Board**: the grid; place a mark; the incremental win check. *Responsibility:* board state + win logic.
- **Player**: name + mark.
- **Game**: turn alternation, status, orchestration.
- **GameStatus** (enum): IN_PROGRESS / X_WON / O_WON / DRAW.

---

## 6. Class/interface design

```
Game
  ├── Board (N x N of Mark; place(); checkWin(r,c))
  ├── Player[2]
  └── status (GameStatus)
```

### normal vs production
- `normal/` — Board with incremental win detection, Game with turn alternation; single-threaded.
- `production/` — online two-player server: `move` is synchronized on the game and enforces the turn invariant (reject out-of-turn / post-game moves) — same shape as Chess/Snake&Ladders production.

---

## Interview Questions, Follow-ups & Traps

**Q: How do you detect a win efficiently?**
Don't rescan the whole board each move. After placing at (r,c), check only: row r, column c, the main diagonal (if r==c), and the anti-diagonal (if r+c==N-1). That's O(N) per move. For large boards this matters.

**Q: How do you generalize to N×N or K-in-a-row (Gomoku)?**
Parameterize the board size and the required run length. The incremental check becomes "count consecutive same marks in each of the 4 directions through (r,c); win if any reaches K." The class structure doesn't change — a sign of a good model.

**Q: How do you represent cells — chars or an enum?**
An enum (`EMPTY/X/O`). Type-safe, self-documenting, no "is it ' ' or '-' for empty?" bugs. Interviewers notice `char`-based boards as a smell.

**Follow-ups**
- *"Draw detection?"* — Track the move count; a full board with no win is a draw. Don't scan for "any empty cell" each time.
- *"Undo moves?"* — Keep a move history (Command/Memento) and reverse.
- *"Online concurrency?"* — Serialize moves per game + whose-turn check (production version).

**Traps**
- Full-board O(N²) win scan every move.
- Char-based board instead of an enum.
- Forgetting to detect draws (infinite loop on a full board).
- Allowing moves after the game is decided.
