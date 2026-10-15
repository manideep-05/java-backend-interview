# 12 — Spring Data JPA & Hibernate

> **Priority: P0.** The persistence context, the N+1 problem, flush-vs-commit, and locking strategy are where SDE-2 interviews separate people who "use `save()`" from people who understand what SQL Hibernate emits and when. Almost every "the endpoint is slow / the data is wrong" scenario traces back here.

**Prerequisites:** [13 Spring Transactions](../13-spring-transactions/), [17 DB transactions & isolation](../17-database-transactions-and-isolation/), [15 SQL](../15-sql-and-relational-databases/).

---

## Table of contents
1. [ORM: what it is and why](#1-orm)
2. [Entity lifecycle & the persistence context](#2-lifecycle)
3. [Dirty checking, flush vs commit](#3-flush-vs-commit)
4. [persist vs merge vs save](#4-persist-merge-save)
5. [Fetching: lazy vs eager](#5-fetching)
6. [The N+1 problem and its fixes](#6-n1)
7. [JPQL vs native, projections, pagination](#7-queries)
8. [Locking: optimistic vs pessimistic](#8-locking)
9. [Relationships, cascade, orphanRemoval](#9-relationships)
10. [equals/hashCode for entities](#10-equals)
11. [Open Session in View](#11-osiv)
12. [Batching](#12-batching)
13. [Trade-offs](#d-trade-offs)
14. [Interview Q&A](#e-interview-qa)
15. [Production relevance](#f-production-relevance)
16. [Revision](#g-revision)

Dependencies for examples: `spring-boot-starter-data-jpa`, a JDBC driver (`org.postgresql:postgresql`), and for visibility set `spring.jpa.show-sql=true` plus `logging.level.org.hibernate.SQL=DEBUG` and `org.hibernate.orm.jdbc.bind=TRACE` to see bound parameters.

---

## 1. ORM

### A. Concept & intuition
JPA is a **specification**; Hibernate is the default **implementation** in Spring Boot. Object-Relational Mapping bridges the "impedance mismatch" between object graphs (references, inheritance, collections) and relational tables (rows, foreign keys, joins). Before ORM you wrote JDBC by hand: SQL strings, `ResultSet` row-to-object mapping, manual connection/statement handling. ORM automates mapping, change tracking, and SQL generation.

**The trade-off it introduces:** convenience at the cost of a *leaky abstraction*. If you don't know what SQL Hibernate generates, you get N+1 queries, surprise lazy-loading exceptions, and accidental full-table loads. The whole chapter is about keeping the abstraction honest.

---

## 2. Lifecycle

### Entity states
| State | Meaning | In persistence context? | Tracked for changes? |
|---|---|---|---|
| **Transient (new)** | just `new`-ed, no DB identity | no | no |
| **Managed (persistent)** | associated with an open `EntityManager` | yes | **yes** |
| **Detached** | was managed, context closed/evicted | no | no |
| **Removed** | marked for delete, pending flush | yes | — |

### The persistence context (first-level cache)
The `EntityManager` holds a **persistence context**: an identity map of managed entities for the current transaction. Within one transaction, loading the same entity by id twice returns the **same instance** (not two) and the second load hits no DB — that's the first-level cache. It is **mandatory** and per-transaction (not shared across requests). The *second-level cache* and *query cache* are separate, optional, shared caches you opt into — don't conflate them.

---

## 3. Flush vs commit

### JPA-03 — the top distinction
- **Flush**: Hibernate synchronizes the persistence context to the DB by **emitting SQL** (INSERT/UPDATE/DELETE) over the current connection — but the transaction is **not yet committed**; changes are not durable and other transactions may not see them (depending on isolation).
- **Commit**: the DB transaction commits; changes become durable and visible per isolation rules. Commit triggers a flush first if needed.

**When does a flush happen?** Default `FlushMode.AUTO`: before a query that might be affected by pending changes, and at commit. You can force `entityManager.flush()`. **Why it matters:** a constraint violation surfaces at *flush* time, which may be far from the line that made the change — surprising if you don't know flush timing. Also, in a `readOnly` transaction Hibernate skips the auto-flush ([13 §5](../13-spring-transactions/)).

```java
// Dirty checking: no explicit save() needed for a managed entity
@Transactional
public void renameUser(Long id, String name) {
    User u = userRepo.findById(id).orElseThrow(); // managed
    u.setName(name);                              // just mutate the managed entity
    // no save() call: at commit, Hibernate dirty-checks and emits UPDATE
}
```
This surprises people: a `SELECT` then a field change produces an `UPDATE` with **no `save()` call**, because the entity is managed and Hibernate compares its snapshot at flush.

---

## 4. persist vs merge vs save
- **`persist(e)`**: makes a *transient* entity managed; expects it to be new. Returns void; the passed instance becomes managed.
- **`merge(e)`**: copies the state of a *detached* entity into a managed instance and **returns that managed instance** — the argument stays detached. Using the argument afterwards is a classic bug.
- **Spring Data `save(e)`**: convenience — internally `persist` if the entity is new (no id / version), else `merge`. For a *new* entity with an assigned id it may do a redundant `SELECT` before insert (merge path) — a known gotcha; implement `Persistable` or use `persist` to avoid it.

```java
// BROKEN: using the detached argument after merge
User detached = ...;            // from a previous tx
userRepo.save(detached);        // returns the managed copy...
detached.setName("x");          // ...but you mutated the DETACHED one -> not persisted
// FIXED:
User managed = userRepo.save(detached);
managed.setName("x");           // mutate the returned managed instance
```

---

## 5. Fetching

- **LAZY**: the association is a proxy; the SQL runs only when you first touch it — *inside an open persistence context*. `@OneToMany`/`@ManyToMany` default LAZY; `@ManyToOne`/`@OneToOne` default **EAGER** (a frequent surprise).
- **EAGER**: loaded immediately with the owner, often via a join or extra query — can pull far more than you need and causes "cartesian product" blowups with multiple eager collections.

**LazyInitializationException**: touching a LAZY association after the persistence context closed (e.g., in the controller/serializer after the `@Transactional` service returned). The right fix is to fetch what you need *within* the transaction (fetch join / entity graph / projection), **not** to enable Open Session in View (see §11).

---

## 6. N+1

### JPA-05 — how it arises
```java
// Entities
@Entity class Author { @Id Long id; String name;
  @OneToMany(mappedBy="author") List<Book> books; } // LAZY
@Entity class Book { @Id Long id; String title; @ManyToOne Author author; }

// BROKEN: 1 query for authors + N queries for each author's books
@Transactional(readOnly = true)
public void report() {
    List<Author> authors = authorRepo.findAll();   // SELECT * FROM author        (1)
    for (Author a : authors) {
        int count = a.getBooks().size();            // SELECT * FROM book WHERE author_id=?  (N)
    }
}
```
Hibernate emits **1 + N** queries: one to load authors, then one per author when the LAZY `books` is first accessed. With 1,000 authors that's 1,001 round trips — the endpoint is slow even though each query is fast ([XT-02](../27-cross-topic-interview-scenarios/)).

**SQL you'd see in logs:**
```
select * from author;
select * from book where author_id = 1;
select * from book where author_id = 2;
... (one per author)
```

### JPA-06 — the fixes (and their trade-offs)
```java
// Fix 1: JPQL fetch join — one query with a join
@Query("select distinct a from Author a join fetch a.books")
List<Author> findAllWithBooks();   // 1 query; 'distinct' dedups the cartesian rows
```
```java
// Fix 2: entity graph — declarative, reusable
@EntityGraph(attributePaths = "books")
@Query("select a from Author a")
List<Author> findAllGraph();
```
```java
// Fix 3: projection / DTO — fetch only needed columns (often best for read endpoints)
interface AuthorBookCount { Long getId(); String getName(); long getBookCount(); }
@Query("select a.id as id, a.name as name, count(b) as bookCount " +
       "from Author a left join a.books b group by a.id, a.name")
List<AuthorBookCount> findCounts();
```
Trade-offs: **fetch join** is one query but a join + `distinct` can be heavy and **can't be combined with pagination safely** (Hibernate paginates in memory and warns — a trap). **Entity graph** is cleaner but same pagination caveat for collections. **Projection/DTO** avoids loading entities at all and is usually best for read models, but it's read-only (not managed). For paginating a collection, fetch IDs first (`@BatchSize` or two queries) rather than `join fetch`.

**`@BatchSize` / `hibernate.default_batch_fetch_size`**: turns N lazy loads into `WHERE id IN (?, ?, …)` batches — a good blanket mitigation.

---

## 7. Queries
- **JPQL**: object-oriented, portable, type-checked against the model.
- **Native SQL**: full DB power (window functions, hints) but ties you to a dialect; map to projections.
- **Pagination**: `Pageable` + `Page<T>` adds `LIMIT/OFFSET` and a `COUNT`. **Keyset pagination** beats OFFSET for deep pages ([16](../16-database-indexing-and-query-optimization/)). Never `join fetch` a collection with `Pageable` (in-memory pagination).

---

## 8. Locking

### JPA-08 — optimistic vs pessimistic
- **Optimistic** (`@Version`): no DB lock held. Each row has a version column; an UPDATE includes `WHERE id=? AND version=?` and bumps the version. If the version moved (someone else updated), 0 rows match → `OptimisticLockException`. You retry/merge or return 409. Best when **conflicts are rare**; cheap, no held locks, scales.
- **Pessimistic** (`@Lock(PESSIMISTIC_WRITE)` → `SELECT … FOR UPDATE`): acquires a DB row lock up front so others block/wait. Best when **conflicts are frequent** or you must serialize (e.g., stock decrement), but it holds locks (contention, deadlock risk, connection-pool pressure).

```java
// Optimistic
@Entity class Document { @Id Long id; @Version long version; String body; }
// On concurrent edits, the second commit throws OptimisticLockException -> handle:
@Transactional
public Document edit(Long id, String body) {
    Document d = repo.findById(id).orElseThrow();
    d.setBody(body);
    return d; // flush at commit: UPDATE ... WHERE id=? AND version=?; 0 rows -> OptimisticLockException
}
```
```java
// Pessimistic
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select d from Document d where d.id = :id")
Document findForUpdate(@Param("id") Long id); // emits SELECT ... FOR UPDATE
```
Decision rule to say out loud: "Start optimistic — it's cheaper and scales; move to pessimistic only when measured conflict rates make retries wasteful or when correctness requires serialization." (Cross-ref [17 §locking](../17-database-transactions-and-isolation/), [CON-10](../06-java-concurrency/).)

---

## 9. Relationships, cascade, orphanRemoval
- **Owning side** owns the foreign key; `mappedBy` marks the inverse. Updating only the inverse side doesn't persist the FK.
- **Cascade**: `PERSIST`, `MERGE`, `REMOVE`, `ALL` propagate operations to associations. `CascadeType.REMOVE` + a large collection = many deletes; be deliberate.
- **orphanRemoval=true**: removing a child from the collection deletes it. Different from cascade REMOVE (which only propagates an explicit remove of the parent).
- Prefer `Set` over `List` for `@ManyToMany` to avoid delete-all-then-reinsert behavior.

---

## 10. equals/hashCode for entities
Entities with generated IDs are tricky: the ID is `null` before persist, so an id-based `hashCode` changes after insert — which **breaks `HashSet` membership** if you added the entity while transient. Guidance: either use a **business/natural key** for equals/hashCode, or make `hashCode` a constant and `equals` compare the (possibly-null) id carefully. Never use all fields (mutable + lazy associations trigger loads). (Cross-ref [01 equals/hashCode](../01-core-java/).)

---

## 11. OSIV
**Open Session in View** keeps the persistence context open until the view/response renders, so lazy loads in the controller/serializer don't throw. Spring Boot enables it by **default** (`spring.jpa.open-in-view=true`). It hides N+1 bugs and holds the DB connection longer (pool pressure). Interview-strong stance: "I disable OSIV and fetch exactly what the response needs in the service layer (DTO projections / fetch joins), so lazy-loading surprises surface in tests, not production." It's a trade-off (convenience vs connection hold + hidden N+1), so state why.

---

## 12. Batching
Default: Hibernate issues one INSERT/UPDATE per entity. For bulk work enable JDBC batching: `spring.jpa.properties.hibernate.jdbc.batch_size=50`, `order_inserts=true`, `order_updates=true`, and flush/clear the context every N to bound memory. For truly large loads, a native bulk `INSERT … SELECT` or `COPY` (Postgres) beats entity-by-entity.

---

## D. Trade-offs

| Approach | Pros | Cons | When | Poor choice when |
|---|---|---|---|---|
| Lazy + fetch join | fix N+1, one query | join+distinct heavy; breaks collection pagination | load a graph you'll fully use | paginated collections |
| Entity graph | declarative, reusable | same pagination caveat | reusable fetch plans | ad-hoc one-offs |
| DTO projection | minimal columns, no managed overhead | read-only, extra query def | read endpoints | you need to mutate |
| `@BatchSize` | blanket N+1 mitigation | still multiple queries (batched) | many small lazy loads | single-graph fetch |
| Optimistic lock | no held locks, scales | retries on conflict | low contention | hot rows |
| Pessimistic lock | serializes, correct | held locks, deadlock, pool pressure | high contention/critical | read-mostly |
| OSIV on | no lazy exceptions | hides N+1, holds connection | prototypes | production performance |

---

## E. Interview Q&A

### JPA-01 (P1) — JPA vs Hibernate vs Spring Data JPA?
**Strong answer:** JPA is the specification (annotations, `EntityManager` API); Hibernate is the implementation Spring Boot uses by default; Spring Data JPA is a layer on top that generates repository implementations (derived queries, `Page`, etc.). They stack: Spring Data → JPA API → Hibernate → JDBC.

### JPA-02 (P1) — Explain entity states and the persistence context.
**Strong answer:** Transient (new, no identity), Managed (tracked by an open `EntityManager`, dirty-checked), Detached (context closed), Removed (pending delete). The persistence context is a per-transaction identity map / first-level cache: same id → same instance, repeated loads hit no DB. It's mandatory and not shared across requests, unlike the optional second-level cache.

<a id="jpa-03"></a>
### JPA-03 (P0) — Difference between flush and commit?
**Strong answer:** Flush emits the pending SQL to the DB over the current connection but does not commit — not durable, visibility depends on isolation. Commit finalizes the transaction (durable, visible), performing a flush first. Default AUTO flush happens before affected queries and at commit, so a constraint violation can surface at flush time, which may be a different line than where I changed the data. In readOnly transactions Hibernate skips the auto-flush.
**Follow-up:** Why did my `UPDATE` run without `save()`? (dirty checking of a managed entity at flush.)

### JPA-04 (P1) — persist vs merge, and what's wrong with using `save()` naively?
**Strong answer:** `persist` makes a new transient entity managed; `merge` copies a detached entity's state into a managed instance and returns *that* — so I must use the return value, not the argument. Spring Data `save` chooses persist-or-merge by newness; for an entity with an assigned id it may do a redundant SELECT before insert (merge path), which I avoid with `Persistable`/`persist` on hot insert paths.

<a id="jpa-05"></a>
### JPA-05 (P0) — How does Hibernate produce the N+1 problem?
**Strong answer:** When I load a list of parents and then access a LAZY association in a loop, Hibernate runs one query for the parents and one additional query each time it initializes a child collection — 1 + N queries. It's invisible until you read the SQL logs; the endpoint is slow despite each query being fast. I detect it by counting queries (SQL logging / datasource-proxy / Hypersistence) and fix with a fetch join, entity graph, DTO projection, or `@BatchSize`, choosing based on whether I need managed entities and whether I'm paginating.
**Weak answer:** "Use eager fetching." Eager often makes it worse (cartesian products, over-fetching) and doesn't help the loop case cleanly.

<a id="jpa-06"></a>
### JPA-06 (P1) — Compare the N+1 fixes.
**Strong answer:** Fetch join = one query but join + `distinct`, and it can't paginate a collection safely (in-memory pagination warning). Entity graph = declarative/reusable, same pagination caveat. DTO projection = fetch only needed columns, no entity overhead, best for read models but read-only. `@BatchSize`/`default_batch_fetch_size` = turns N loads into IN-list batches, a good global mitigation. For paginating collections I fetch IDs first then load details.

### JPA-07 (P1) — What is LazyInitializationException and the right fix?
**Strong answer:** It's touching a LAZY association after the persistence context closed (e.g., during JSON serialization in the controller). The right fix is to fetch what the response needs inside the transaction via fetch join/entity graph/projection — not to turn on Open Session in View, which hides N+1 and holds the connection longer. I disable OSIV so these surface in tests.

<a id="jpa-08"></a>
### JPA-08 (P0) — Optimistic vs pessimistic locking: when each?
**Strong answer:** Optimistic (`@Version`) holds no DB lock; the UPDATE's `WHERE version=?` detects a concurrent change and throws `OptimisticLockException`, which I handle by retry/merge or 409 — ideal when conflicts are rare because it scales without blocking. Pessimistic (`SELECT … FOR UPDATE`) locks the row so others wait — for frequent conflicts or must-serialize operations like stock decrement, at the cost of held locks, deadlock risk, and connection-pool pressure. I start optimistic and escalate to pessimistic only when measured conflict rates justify it.
**Follow-up:** How does optimistic locking interact with retries and idempotency? (retry must be safe/idempotent — ch.17/18.)

### JPA-09 (P1) — Why are entity `equals`/`hashCode` tricky?
**Strong answer:** Generated IDs are null before persist, so id-based hashCode changes after insert, breaking `HashSet` membership for entities added while transient; all-fields equals triggers lazy loads and uses mutable state. I use a stable business key when one exists, or a careful id-based equals with a constant/consistent hashCode, and I never include lazy associations.

### JPA-10 (P2) — How do you do efficient bulk inserts?
**Strong answer:** Enable `hibernate.jdbc.batch_size`, order inserts/updates, and flush+clear every N to bound memory; for very large loads use native bulk SQL (`INSERT … SELECT`) or Postgres `COPY`, because entity-by-entity persistence with its dirty-checking overhead doesn't scale to millions of rows.

---

## F. Production relevance
This chapter is the root cause of the two most common backend incidents: **slow endpoints** (N+1, OSIV holding connections, missing projections pulling whole graphs) and **wrong/lost data** (lost updates without `@Version`, using the detached merge argument, cascade/orphan surprises, flush-timing constraint errors). Reading the generated SQL is the core skill — turn on SQL logging in dev, count queries in tests, prefer DTO projections for read endpoints, keep transactions short, and pick a deliberate locking strategy. "I can tell you exactly what SQL this repository method emits" is a strong SDE-2 signal.

---

## G. Revision

### 10 key takeaways
1. JPA=spec, Hibernate=impl, Spring Data=repo layer on top.
2. Managed entities are dirty-checked; mutate without `save()`.
3. Flush = emit SQL; commit = make durable. Flush ≠ commit.
4. `merge` returns the managed copy; use the return value.
5. `@ManyToOne`/`@OneToOne` default EAGER; collections default LAZY.
6. N+1 = 1 parent query + N child queries; read the SQL logs.
7. Fix N+1 with fetch join/entity graph/projection/`@BatchSize`; mind pagination.
8. Optimistic for rare conflicts, pessimistic for frequent/critical.
9. Disable OSIV; fetch in the service layer.
10. Batch bulk writes; flush+clear to bound memory.

### 10 rapid-fire
1. Default fetch for @OneToMany? (LAZY) 2. For @ManyToOne? (EAGER) 3. First-level cache scope? (per transaction) 4. Who owns the FK? (owning side) 5. OSIV default in Boot? (on) 6. Lock annotation for a version column? (@Version) 7. SQL for pessimistic write lock? (SELECT…FOR UPDATE) 8. Can you paginate a collection fetch join? (not safely) 9. Does a managed entity need save() to update? (no) 10. merge returns? (the managed instance)

### 5 tricky
1. Why did my UPDATE fire without save()? 2. Why is a fetch join + Pageable dangerous? 3. Why did adding a transient entity to a HashSet then persisting break contains()? 4. Why did eager fetching make it slower? 5. Why did a constraint error appear on a line that didn't touch that column? (flush timing)

### 5 exercises
1. Reproduce N+1 with SQL logging on; fix three ways; count queries each time.
2. Trigger `OptimisticLockException` with two concurrent edits; add retry handling.
3. Convert a read endpoint from entity loading to a DTO projection; compare queries.
4. Turn OSIV off; fix the resulting LazyInitializationException properly.
5. Bulk-insert 100k rows with batching + flush/clear; measure vs naive save loop.

### One-page revision sheet
> States: transient/managed/detached/removed. Persistence context = per-tx 1st-level cache (identity map), mandatory; 2nd-level/query cache optional & shared. **Flush=emit SQL (not durable); commit=durable.** Managed entities dirty-checked → UPDATE without save(). `merge` returns managed copy. @ManyToOne EAGER, collections LAZY. **N+1 = 1 + N queries**; fix: fetch join (no collection pagination), entity graph, DTO projection (read model), @BatchSize. Optimistic(@Version, no lock, retry on conflict) vs pessimistic(SELECT FOR UPDATE, held lock). Disable OSIV; fetch in service. Batch writes + flush/clear. Entity equals/hashCode: business key, never all fields.

### Prerequisites to revisit
- Transaction boundaries & thread-binding (ch.13).
- Isolation & locking at the DB level (ch.17).
- SQL joins & pagination (ch.15, 16); equals/hashCode (ch.01).
