# Fundamentals — the warm-up round

Most SDE2 LLD interviews open with 5–15 minutes of OOP and concurrency fundamentals *before* the design question. This is where the interviewer decides whether you're "solid" — get these wrong and the rest of the loop is uphill.

Two primers, each with runnable, heavily-commented Java:

| Topic | What it covers | Why it matters |
|-------|----------------|----------------|
| [OOP](oop) | composition vs inheritance, interface vs abstract class, equals/hashCode, immutability, Comparable vs Comparator, enums with behavior | The vocabulary every LLD answer is built from. |
| [Concurrency](concurrency) | race conditions, synchronized/ReentrantLock/ReadWriteLock, volatile, atomics/CAS, ExecutorService, BlockingQueue, deadlock | The "now make it thread-safe" follow-up that separates SDE1 from SDE2. |

> These are the building blocks for the `production/` solutions in `05-design-problems/`. When a problem's production version uses a `ReentrantLock` or a CAS loop, this section is where you learn *why* and *when*.

## How to run
```bash
javac oop/*.java && java oop.Main           # (per demo class)
javac concurrency/*.java && java concurrency.Main
```
