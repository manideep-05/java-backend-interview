# My Notes — Tic-Tac-Toe

> Personal space. Smallest game problem — nail the efficient win check + extensibility.

## Problem in one line (my words)
_..._

## The ONE concept to focus on here
_...incremental O(N) win check around the last move; enum board..._

## The key concurrency insight (normal → production)
_...online turn integrity via game lock..._

## How I'd generalize (say this)
_...N x N and K-in-a-row with no class changes — count runs in 4 directions..._

## How I'd drive this in an interview
1. Clarify: _..._
2. Core flow: _..._
3. Entities: _..._
4. Then harden: _..._

## Edge cases I must remember to call out
- _...draw detection, out-of-turn, occupied cell..._

## Where I got stuck / what to revisit
_..._

- [ ] I can code the normal version from scratch
- [ ] I can write the incremental win check without a full scan
- [ ] I can generalize to N x N / K-in-a-row on the spot
