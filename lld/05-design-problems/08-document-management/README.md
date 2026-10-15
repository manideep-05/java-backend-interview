# Document Management System — LLD

A content system (folders + documents, versioning, permissions). The signals are **Composite** for the folder tree and **Memento** for version history.

---

## 1. Requirements

**Functional**
- Folders contain documents and sub-folders (a tree).
- Documents have content and **versions** (save → new version; restore an old one).
- Permissions (read/write) per user (often simplified).
- Operations like total size, search by name.

**Non-functional**
- Concurrent edits to the same document must not corrupt version history.

---

## 2. Why these patterns

- **Composite**: treat a document (leaf) and a folder (container) uniformly via a common `Node` interface (getSize, getName) — recursion over the tree.
- **Memento**: capture a document's content as an opaque version snapshot so you can restore without exposing internals.

---

## 3. Critical flow

edit document → `save()` captures the current content as a new version (Memento pushed to history) → `restore(v)` sets content from a stored version. Folder operations (size, listing) recurse via Composite.

---

## 4. Edge cases & how to spot them

| Source | Edge | Handling |
|--------|------|----------|
| Versioning | Restore a non-existent version | Validate the version index. |
| Memory | Unbounded version history | Cap history or store diffs. |
| Concurrency | Two users save the same doc | Guard the save/version step. |
| Tree | Deep nesting / cycles | Prevent adding a folder into itself. |
| Permissions | Write without permission | Enforce before mutating. |

---

## 5. Entities, relationships, responsibilities

- **Node** (interface): `getName`, `getSize`. → Composite component.
- **Document** (leaf): content + version history (Memento caretaker).
- **Folder** (composite): children (Nodes); recurses for size/listing.
- **DocumentMemento**: opaque content snapshot.
- (PermissionService): access checks (kept simple here).

---

## 6. Class/interface design

```
Node (getName, getSize)
 ├── Document (leaf) ── history: List<Memento>   MEMENTO
 └── Folder (composite) ── children: List<Node>  COMPOSITE
```

### normal vs production
- `normal/` — Composite tree; Document keeps a version list; save/restore via Memento; single-threaded.
- `production/` — a document's `save`/`restore` are guarded (per-document lock) so concurrent edits don't corrupt the version list; folder reads use concurrent structures.

---

## Interview Questions, Follow-ups & Traps

**Q: Why Composite for folders/documents?**
So clients compute size, list, or search over a document or an entire subtree with the same call — no `instanceof` branching. A folder's operation recurses into children, which may be folders themselves. (Same idea as the File System problem.)

**Q: How do you version documents without exposing internals?**
Memento: the document creates an opaque snapshot of its content; a history list (caretaker) stores snapshots; restore sets content from one. The caretaker never reads the snapshot's contents — encapsulation preserved.

**Q: Version history memory concern?**
Full-content snapshots are heavy. Store diffs/deltas between versions, cap the history length, or snapshot only on explicit "commit." Trade storage for restore cost.

**Follow-ups**
- *"Concurrent edits (two users on one doc)?"* — Guard save/version per document; or optimistic versioning (reject a save against a stale base version — like a CAS on version number).
- *"Permissions model?"* — ACL per node, checked before mutations; inheritance down the folder tree.
- *"Search at scale?"* — An inverted index, not a tree walk.

**Traps**
- Type-checking (document vs folder) in clients instead of Composite.
- Exposing version internals to the caretaker (breaks Memento encapsulation).
- Unbounded version history (memory blowup).
- Allowing a folder to contain itself (cycle).
