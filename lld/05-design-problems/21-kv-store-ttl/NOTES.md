# My Notes — KV Store with TTL

> Personal space. Maps to my Redis/caching work — lean on that.

## Problem in one line (my words)
_..._

## The ONE concept to focus on here
_...expiry strategy: lazy vs active vs both..._

## The key concurrency insight (normal → production)
_...the get/expiry race and why conditional remove(key, entry) fixes it..._

## Expiry strategy I'd propose and why
_...lazy + sampling sweeper (Redis-style)..._

## How I'd drive this in an interview
1. Clarify: _..._
2. Core flow: _..._
3. Entities: _..._
4. Then harden: _..._

## Edge cases I must remember to call out
- _..._

## Where I got stuck / what to revisit
_..._

- [ ] I can code the lazy-expiry version from scratch
- [ ] I can explain the conditional-remove race fix
- [ ] I can extend it to a bounded LRU+TTL cache
- [ ] I can connect it to Redis behavior out loud
