# Chess Game — LLD

A pure object-modeling problem (little concurrency). The signal is a clean piece hierarchy with per-piece move rules via **Strategy/polymorphism**, plus **Factory** for setup.

---

## 1. Requirements

**Functional**
- 8×8 board; standard pieces (pawn, rook, knight, bishop, queen, king).
- Validate a move for the piece type; execute legal moves; capture.
- Alternate turns; detect check/checkmate (often scoped out or simplified).

**Non-functional**
- Mostly single-threaded (turn-based). Concurrency only matters for an online server coordinating two players' moves on one game.

---

## 2. Why these patterns

- **Strategy / polymorphism**: each piece defines its own `isValidMove` (or a `MovementStrategy`). Adding a variant piece = a new class, no change to the board (OCP).
- **Factory**: build the initial board / create pieces from type.
- **State** (optional): game state (ACTIVE / CHECK / CHECKMATE / DRAW).

---

## 3. Critical flow

`move(from, to)`:
1. There's a piece at `from` and it belongs to the player to move.
2. The piece's movement rule permits from→to (and path is clear for sliding pieces).
3. `to` isn't occupied by own piece.
4. (Full rules) the move doesn't leave own king in check.
5. Apply: move piece, capture if needed, toggle turn.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Bounds | Move off the board | Reject coordinates outside 0..7. |
| Turn | Moving opponent's piece / out of turn | Reject. |
| Path | Sliding piece jumps over a piece | Check the path is clear (not for knight). |
| Capture | Land on own piece | Illegal; on enemy piece = capture. |
| Special | Castling, en passant, promotion | Call out as extensions; often scoped out. |
| Check | Move exposes own king | Full rules reject; simplified version may skip. |

**How to find edges:** walk a move for each piece type and ask about bounds, turn, path, and capture.

---

## 5. Entities, relationships, responsibilities

- **Board**: 8×8 grid of cells; get/set piece; apply move.
- **Piece** (abstract) → Pawn/Rook/Knight/Bishop/Queen/King; each knows its color and `isValidMove(board, from, to)`. *Responsibility:* its own movement rule.
- **Position** (row, col): value object.
- **PieceFactory**: create the starting position.
- **Game**: whose turn, move loop, (state).

---

## 6. Class/interface design

```
Game
  ├── Board (8x8 of Piece?)
  ├── turn (Color)
  └── move(from, to): validate via piece polymorphism, then apply
Piece (abstract): isValidMove(board, from, to)  <- polymorphic rule
  Pawn / Rook / Knight / Bishop / Queen / King
```

### normal vs production
- `normal/` — board + piece hierarchy with per-piece `isValidMove`; `Game.move` validates (bounds, turn, rule, own-piece) and applies; turn toggles.
- `production/` — for an online server, `move` is **synchronized on the game** so two players' concurrent move submissions are serialized and the turn invariant can't be violated (both can't move in the "same" turn).

---

## Interview Questions, Follow-ups & Traps

**Q: How do you model per-piece movement cleanly?**
Polymorphism: an abstract `Piece.isValidMove(board, from, to)` overridden per type. The board/game never switches on piece type — adding a new piece is a new subclass (OCP). This is the Strategy idea expressed through the class hierarchy.

**Q: How do sliding pieces (rook/bishop/queen) check their path?**
Step one square at a time from `from` toward `to` along the line; if any intermediate square is occupied, the move is blocked. Knights skip this (they jump).

**Q: How would you detect check/checkmate?**
Check = the king's square is attacked by any enemy piece (reuse `isValidMove` from every enemy piece to the king's square). Checkmate = in check AND no legal move removes the check. It's expensive but conceptually simple; usually mention it rather than fully code it.

**Follow-ups**
- *"Castling / en passant / promotion?"* — Special-case rules; promotion swaps the pawn via the factory. Flag them as add-ons.
- *"Undo moves?"* — Keep a move history (Command/Memento) and reverse.
- *"Online two-player concurrency?"* — Serialize moves per game (one lock) and enforce the turn invariant; reject out-of-turn moves.

**Traps**
- A giant `switch(pieceType)` in the board instead of polymorphism.
- Forgetting path-blocking for sliding pieces.
- Ignoring turn order / moving the opponent's piece.
- Over-scoping: trying to implement full check/checkmate/castling when the interviewer wanted the core model.
