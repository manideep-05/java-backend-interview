# My Notes — Concurrency Fundamentals

> Personal space. Fill this in after you study the topic.

## The tools and when I'd reach for each
- synchronized → _..._
- ReentrantLock → _..._
- ReadWriteLock → _..._
- volatile → _..._
- atomic / CAS → _..._
- ExecutorService → _..._
- BlockingQueue → _..._

## The one mental model that ties it together
_...e.g. "find the shared mutable state + the compound action, then pick the cheapest primitive that makes it atomic"..._

## Traps I must not fall into
- volatile makes count++ atomic? → _..._
- forgetting unlock in finally → _..._
- lock ordering / deadlock → _..._
- unbounded queue → _..._

## Where I got stuck / what to revisit
_..._

- [ ] I can explain each primitive and when to use it
- [ ] I can spot the race in a given snippet
- [ ] I can pick the right fix and justify it
