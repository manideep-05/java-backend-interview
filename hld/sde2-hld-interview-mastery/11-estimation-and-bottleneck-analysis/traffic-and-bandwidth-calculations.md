# Traffic and Bandwidth Calculations

> Prerequisites: [estimation-cheat-sheet](./estimation-cheat-sheet.md)
> Related: [00-foundations/back-of-the-envelope-estimation](../00-foundations/back-of-the-envelope-estimation.md)

Converting users to request rates and request rates to bandwidth — the first numbers you compute in almost every design.

---

## A. Formulas

- **Average RPS** = `(DAU × actions/user/day) / 86,400` ≈ `12 × (millions of actions/day)`.
- **Peak RPS** = `average RPS × peak factor` (2–10×; higher for consumer apps with evening peaks, flash sales).
- **Read/Write split** = apply the read:write ratio to peak RPS.
- **Egress bandwidth** = `RPS × average response size`.
- **Ingress bandwidth** = `write RPS × average request/upload size`.

Peak factor guidance (heuristic): internal/B2B ~2–3×; consumer apps ~3–5×; flash-sale/viral ~10×+. State your assumed factor.

---

## B. Worked example — DMS

1M DAU, 20 actions/day, 5× peak, 90% reads, 1KB responses, 2MB uploads at 10% of actions:
- Average RPS = 20M/86,400 ≈ 231 (or 12×20 = 240).
- Peak RPS = 240 × 5 = **~1,200**.
- Peak read RPS = 1,080; peak write RPS = **~120**.
- Egress (reads, 1KB each) = 1,080 × 1KB ≈ **~1 MB/s**.
- Ingress (uploads): 10% of writes are 2MB uploads ≈ 12/s × 2MB = **~24 MB/s** (notable — justifies pre-signed-URL direct-to-object-storage so this doesn't flow through the app; see [object-storage-and-file-uploads](../10-storage-and-content-delivery/object-storage-and-file-uploads.md)).

**Implication:** modest RPS (keep it simple), but upload *ingress* bandwidth is the notable number → offload uploads to object storage.

---

## C. Pitfalls

- **Confusing users with RPS:** 1M DAU ≠ 1M RPS — convert via actions/day (see [back-of-the-envelope-estimation](../00-foundations/back-of-the-envelope-estimation.md)).
- **Designing for average, not peak:** apply the peak factor.
- **Ignoring upload/download bandwidth:** large payloads make bandwidth, not RPS, the pressure (route big files around the app).

---

## D. Interview Q&A

- Q: "2M DAU, 50 actions/day, 4× peak. Peak RPS?" → *100M/day ≈ 1,160 avg; ×4 = ~4,640 peak.*
- Q: "What dominates bandwidth here — RPS or payload size?" → *Depends: small JSON responses → RPS; large file transfers → payload size. For file-heavy systems, offload transfers to object storage/CDN so they don't hit the app.*

**Common wrong answers:** users as RPS; average not peak; ignoring payload-driven bandwidth.

---

## E. Exercise

A video app: 10M DAU, 5 video views/day, each view streams ~50MB. Compute average/peak view RPS (3×) and peak egress bandwidth. State why this demands a CDN.
*(Answer: 50M views/day ≈ 580 avg view RPS; ×3 = ~1,740 peak; egress ≈ 1,740 × 50MB = ~87 GB/s — enormous → serve via CDN from the edge, never from the origin.)*

---

## F. Revision summary

**Key takeaways**
- Average RPS from users×actions/86,400 (or 12×millions/day); peak = ×peak factor; split by read:write.
- Bandwidth = RPS × payload; large payloads make bandwidth the pressure → offload to object storage/CDN.
- Convert users→RPS; design for peak; don't ignore payload-driven bandwidth.

**Rapid-fire**
1. Peak RPS = ? → average × peak factor.
2. Egress bandwidth = ? → RPS × response size.
3. 1M DAU = 1M RPS? → No (convert via actions/day).
4. Large-payload system — what dominates? → Bandwidth (offload to CDN/object storage).
5. Typical consumer peak factor? → ~3–5× (heuristic).

**Next:** [storage-and-retention-calculations](./storage-and-retention-calculations.md)
