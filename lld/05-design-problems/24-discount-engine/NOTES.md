# My Notes — Discount / Coupon Engine

> Personal space. A pure OCP/Strategy showcase — great for talking about extensibility.

## Problem in one line (my words)
_..._

## The patterns I used (and why)
_...Strategy per discount; Specification for eligibility; pipeline/CoR for stacking..._

## The ONE concept to focus on here
_...add a discount type without editing the engine (OCP)..._

## The key concurrency insight (normal → production)
_...single-use coupon via atomic set.add() check-then-act; stateless engine..._

## Stacking vs exclusive (be ready to model both)
_..._

## How I'd drive this in an interview
1. Clarify: _...stacking? exclusivity? eligibility rules?..._
2. Core flow: _..._
3. Entities: _..._
4. Then harden: _..._

## Edge cases I must remember to call out
- _...floor at zero, rounding, one-time coupon, order of stacking..._

## Where I got stuck / what to revisit
_..._

- [ ] I can code the engine + a couple of discounts from scratch
- [ ] I can add a new discount type live without touching the engine
- [ ] I can explain atomic one-time coupon redemption
