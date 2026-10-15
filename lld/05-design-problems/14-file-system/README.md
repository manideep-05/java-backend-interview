# File System — LLD

The canonical **Composite** problem: files and directories form a tree, and clients operate on either uniformly. Path operations and concurrency are the follow-ups.

---

## 1. Requirements

**Functional**
- Directories contain files and sub-directories (a tree).
- Operations: create file/dir, delete, list, compute total size, find by path.
- A path like `/a/b/c.txt` resolves to a node.

**Non-functional**
- Concurrent create/delete/lookup must keep the tree consistent.

---

## 2. Why Composite

A `FileSystemNode` abstraction with `getName`/`getSize` lets a file (leaf) and a directory (composite) be treated the same. A directory's `getSize` recurses over children; a client never checks "is this a file or a directory?". (Same shape as the Document Management tree.)

---

## 3. Critical flow

- `mkdir(path)` / `touch(path)`: walk/resolve the parent, then add a child.
- `getSize(path)`: resolve the node; recurse if it's a directory.
- `delete(path)`: resolve the parent, remove the child.

Path resolution = split on `/`, walk the tree directory by directory.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Path | Non-existent path | Return null / throw "not found". |
| Duplicate | Create an existing name | Reject or overwrite (define). |
| Type | `cd` into a file | Reject (not a directory). |
| Boundary | Root path `/` | Handle specially. |
| Concurrency | Create + delete same dir | Guard per-directory mutations. |

---

## 5. Entities, relationships, responsibilities

- **FileSystemNode** (interface): `getName`, `getSize`. → Composite component.
- **File** (leaf): a name + size (content length).
- **Directory** (composite): named children keyed by name; recurses for size; resolves paths.
- **FileSystem** (Facade): path-based API over the root directory.

---

## 6. Class/interface design

```
FileSystemNode (getName, getSize)
 ├── File (leaf)
 └── Directory (composite) ── Map<name, FileSystemNode>   COMPOSITE
FileSystem (Facade): path ops over root
```

### normal vs production
- `normal/` — Composite tree; Directory children in a map; path resolution by walking; single-threaded.
- `production/` — directory children in a `ConcurrentHashMap`; creates use atomic `putIfAbsent` so two threads can't both create the same name; recursion reads are consistent.

---

## Interview Questions, Follow-ups & Traps

**Q: Why Composite here?**
Files and directories share `getSize`/`getName`; a directory computes size by recursing over children (which may be directories). Composite erases the leaf-vs-container distinction for clients — no `instanceof`.

**Q: How do you resolve a path like `/a/b/c`?**
Split on `/`, start at root, and walk: at each segment, look up the child in the current directory; fail if missing or if a non-final segment isn't a directory. O(path length).

**Q: Concurrency on the tree?**
Guard per-directory mutations. A `ConcurrentHashMap` of children makes create/delete/lookup on one directory thread-safe, and `putIfAbsent` makes "create if not exists" atomic so two threads can't both create `/a/x`. Different directories don't contend.

**Follow-ups**
- *"Symbolic links / hard links?"* — A node can be referenced from multiple parents (hard link) or a special link node pointing to a path (symlink); watch for cycles.
- *"Compute size efficiently for huge trees?"* — Cache directory sizes and invalidate up the chain on change.
- *"Permissions / metadata?"* — Attach an attributes object per node.

**Traps**
- `instanceof` branching instead of Composite polymorphism.
- Not handling path resolution failures (missing segment, file-as-directory).
- Non-atomic "create if absent" (duplicate creation race).
- Naive full-tree size recomputation at scale.
