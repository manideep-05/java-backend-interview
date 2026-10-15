# My Notes — Snake and Ladders

> Personal space. A warmup problem — the win here is clean modeling + testability.

## Problem in one line (my words)
_..._

## The ONE concept to focus on here
_...unify snakes+ladders as one jump map; inject the dice for testability..._

## The key concurrency insight (normal → production)
_...online multiplayer turn integrity via a game lock + whose-turn check..._

## How I'd keep it testable (say this)
_...inject Dice interface; FixedDice for deterministic tests..._

## How I'd drive this in an interview
1. Clarify: _...overshoot rule? chaining jumps?..._
2. Core flow: _..._
3. Entities: _..._
4. Then harden: _..._

## Edge cases I must remember to call out
- _..._

## Where I got stuck / what to revisit
_..._

- [ ] I can code the normal version from scratch
- [ ] I can justify the single jump-map modeling
- [ ] I can explain the testable-dice seam
