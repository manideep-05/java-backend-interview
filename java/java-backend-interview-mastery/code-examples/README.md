# code-examples — runnable demonstrations

A self-contained **Maven** project (Java 21) that *proves* the pure-Java claims made in the chapter READMEs. Kept dependency-light (JUnit 5 only, no Spring/DB) so it builds and tests run anywhere without external services.

> Spring, JPA, and SQL behaviors are demonstrated inline in the chapter READMEs (they need a running container/DB). This module focuses on the pure-Java claims that can be verified deterministically: concurrency, collections, and core object semantics.

## What's here
| Class / test | Chapter | Demonstrates |
|---|---|---|
| `concurrency/CounterDemo` | [06 CON-01](../06-java-concurrency/) | `volatile count++` loses updates; `AtomicLong` doesn't. `main` prints the undercount. |
| `concurrency/DocumentPublisher` | [06 §4 / COL-03](../06-java-concurrency/) | atomic `Set.add` → "publish exactly once per id" under concurrency. |
| `core/Period` | [01 CJ-05](../01-core-java/) | a correct immutable value type (validation, equals/hashCode). |
| `CounterDemoTest` | 06 | atomic counter is exact; broken counter never exceeds expected. |
| `DocumentPublisherTest` | 06 | side effect runs exactly once per id across 16 racing threads. |
| `CoreJavaSemanticsTest` | 01 | Integer-cache `==` trap, String pool identity, immutable key in a `HashMap`. |

## Build & test
```bash
# from this directory
mvn test            # compile + run all tests
mvn -q compile      # just compile
```
Run the lost-update demonstration directly:
```bash
mvn -q compile exec:java -Dexec.mainClass=com.interview.mastery.concurrency.CounterDemo
# or, after `mvn package`, run the compiled class on the classpath
```

## Notes
- Targets Java 21 via `maven.compiler.release=21`. A newer JDK can build it (`--release 21`); the Maven in this environment runs on JDK 21.
- Tests assert *invariants* (e.g., the broken counter is `<= expected`) rather than requiring the race to always manifest, so they are not flaky on machines where the race happens not to appear.
