# Cursor Pagination (Java / Spring Boot)

> Concept: [pagination-filtering-and-sorting](../01-networking-and-api-design/pagination-filtering-and-sorting.md) · [indexing-and-query-optimization](../03-databases-and-data-modeling/indexing-and-query-optimization.md)

**HLD point:** offset pagination (`OFFSET 10000`) scans+discards rows → slow at depth + unstable under writes. **Cursor (keyset) pagination** seeks via an indexed sort key → constant cost at any depth + stable.

**Where the concern lives:** app builds the keyset query; the **DB index on the sort key** makes it fast.

```java
// Cursor encodes the last row's (createdAt, id). Query uses WHERE on the indexed key.
@Repository
interface DocumentRepo extends Repository<Document, Long> {
    @Query("""
        SELECT d FROM Document d
        WHERE d.ownerId = :owner
          AND (d.createdAt < :ts OR (d.createdAt = :ts AND d.id < :id))  -- keyset predicate
        ORDER BY d.createdAt DESC, d.id DESC
        """)
    List<Document> pageAfter(@Param("owner") Long owner,
                             @Param("ts") Instant ts, @Param("id") Long id,
                             Pageable limit); // PageRequest.of(0, limit)
}
```

```java
@Service
class DocumentListService {
    private final DocumentRepo repo;

    public PageResult list(Long owner, String cursor, int limit) {
        Instant ts; Long id;
        if (cursor == null) { ts = Instant.MAX; id = Long.MAX_VALUE; } // first page
        else { var c = Cursor.decode(cursor); ts = c.ts(); id = c.id(); }

        List<Document> rows = repo.pageAfter(owner, ts, id, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        if (hasMore) rows = rows.subList(0, limit);
        String next = hasMore
            ? Cursor.encode(rows.get(rows.size()-1).getCreatedAt(), rows.get(rows.size()-1).getId())
            : null;
        return new PageResult(rows, next, hasMore);
    }
}
```

Backing index (critical): `CREATE INDEX ON documents (owner_id, created_at DESC, id DESC);` — serves the filter + sort + tiebreaker in one index seek.

**Key points to say:**
- **Offset is scan+discard** → page 10,000 is slow and can skip/dupe rows under concurrent writes. **Cursor seeks** on an indexed key → constant cost + stable (see [pagination-filtering-and-sorting](../01-networking-and-api-design/pagination-filtering-and-sorting.md)).
- Always include a **unique tiebreaker** (`id`) in the sort so the order is deterministic and the cursor is unambiguous.
- Return `hasMore` + `nextCursor`; **avoid exact total counts** on large filtered sets (`COUNT(*)` is itself expensive).
- The index must match the filter + sort + tiebreaker (composite-prefix rule — see [indexing-and-query-optimization](../03-databases-and-data-modeling/indexing-and-query-optimization.md)).

**Caveat:** production needs opaque/signed cursors (don't leak internal state), a max `limit`, and the matching index actually created.

**Maps to your resume:** your document-list endpoints at scale (and the 60M-row tables) — cursor pagination + the right composite index is how those stay fast as data grows.
