# My Notes — Pub/Sub Message Queue

> Personal space. Fill this in after solving the problem. This one maps directly to real messaging work (RabbitMQ/SQS, outbox, DLQ) — use that to your advantage in the interview.

## Problem in one line (my words)
_..._

## The patterns I used (and why)
_...Observer for fan-out; producer/consumer per subscriber because..._

## The ONE concept to focus on here
_...per-subscriber queue+worker = failure/lag isolation..._

## The key concurrency insight (normal → production)
_...sync fan-out blocks publisher -> async per-subscriber inbox; which structures and WHY..._

## At-least-once + idempotency (say this in the interview)
_...why exactly-once is really at-least-once + idempotent consumers (dedupe by id)..._

## Retry + DLQ (tie to my real experience)
_...N retries with backoff, then dead-letter; where I've built this..._

## How I'd drive this in an interview
1. Clarify: _..._
2. Core flow: _..._
3. Entities: _..._
4. Then harden: _..._

## Edge cases I must remember to call out
- _..._

## Where I got stuck / what to revisit
_..._

- [ ] I can derive the entities from requirements unprompted
- [ ] I can code the normal version from scratch
- [ ] I can explain async delivery + retry/DLQ + idempotency
- [ ] I can connect it to my RabbitMQ/outbox experience out loud
