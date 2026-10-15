# 29 — Behavioral & Leadership (STAR)

> **Priority: P0 for the loop.** Almost every SDE-2 interview has a behavioral round, and strong technical candidates lose offers here by being vague. This chapter gives you the framework and **pre-built stories drawn from your actual experience** so you walk in with concrete, quantified answers and can defend the decisions behind them.

This is not generic advice padding — the stories below are seeded from your resume (EIL + JobFinder) and shaped into reusable STAR narratives. Edit the exact numbers to match what you can defend.

---

## A. The STAR framework
Answer every behavioral question as **S**ituation → **T**ask → **A**ction → **R**esult, then be ready for the follow-up (**why** you chose that action, what you'd do differently).

- **Situation (10–15s):** context, scale, stakes. Give enough that the problem sounds hard.
- **Task (5–10s):** your specific responsibility. Use "I", not "we", for your part.
- **Action (bulk):** what *you* did, the options you weighed, the trade-off you made. This is where SDE-2 is judged — decisions, not activity.
- **Result (10–15s):** quantified outcome + what you learned. Numbers make it credible.

**The SDE-2 differentiator:** interviewers want *ownership, judgment under ambiguity, and impact beyond writing code* — driving a decision, handling disagreement, owning a failure, mentoring, measuring results. Keep "we" for team context and switch to "I" for your contribution.

---

## B. What SDE-2 behavioral rounds actually probe
| Competency | Typical prompt | Your strongest story |
|---|---|---|
| Ownership / impact | "Most impactful project?" | API latency 90% reduction (B1) |
| Handling ambiguity | "A problem with no clear solution?" | Cross-system sync design (B3) |
| Technical judgment / trade-offs | "A tough technical decision?" | Outbox vs 2PC for notifications (B2) |
| Dealing with failure | "A time you failed / a bug you caused?" | (B5 — prepare honestly) |
| Conflict / influence | "Disagreed with someone?" | pushing a design change (B6) |
| Mentoring / leadership | "Helped someone grow?" | mentored 3 trainees (B4) |
| Scope / prioritization | "Too much to do, how did you choose?" | 6 portals + 4 services (B7) |

---

## C. Pre-built stories (STAR) — from your experience

> Adjust numbers to what you can defend under questioning. Interviewers *will* ask "how did you measure that?"

### B1 — API latency: 4–5s → 300–500ms on 60M-row Oracle tables (impact + judgment)
- **S:** 20+ business-critical operations on Oracle tables with 60M+ rows were taking 4–5 seconds, hurting user-facing portals.
- **T:** I owned bringing these under acceptable latency without changing the data model under everyone.
- **A:** I didn't guess — I pulled execution plans, found full table scans and N+1 query patterns, added/reordered indexes to match the query predicates (equality-then-range), eliminated inefficient joins, and introduced materialized views for frequently-reused static data. I verified each change against production-scale data with `EXPLAIN`, not a dev table.
- **R:** ~90% reduction, from 4–5s to 300–500ms across 20+ operations. Learned to always diagnose from the plan before touching indexes.
- **Likely follow-ups:** "Why materialized views and not a cache?" (static, reused, consistency window acceptable; cache adds invalidation complexity — ch.20). "How did you know an index would help?" (plan showed a seq scan on a selective predicate; verified estimated vs actual rows — ch.16 IDX-07). "Downside of your indexes?" (write cost; I only indexed real query patterns). "What was the N+1 and how'd you fix it?" (ch.12 JPA-05).

### B2 — Notification platform: outbox + retries + DLQ (trade-off + reliability)
- **S:** Notifications were sent inline and unreliable; we needed a centralized platform handling ~15K/day across many apps.
- **T:** Design it so a DB change and the notification can't diverge, and so transient failures don't lose or duplicate messages.
- **A:** I built it on Node/Express/RabbitMQ using the **outbox pattern** — the business change and the outbox record commit in one local transaction, a relay publishes, with 5 retries then routing to a dead-letter queue. I made consumers **idempotent** (dedup key) so at-least-once redelivery never double-sends. I chose outbox over a distributed/2PC approach because 2PC is operationally heavy and doesn't scale; eventual consistency was acceptable here. Packaged a reusable Java client so 13 apps could adopt it.
- **R:** ~15K notifications/day reliably; one platform replaced scattered ad-hoc sending; adopted by 13 applications.
- **Likely follow-ups:** "Why outbox not just publish after commit?" (the dual-write gap — a crash between commit and publish loses the message; ch.19 MSG-05). "At-least-once means duplicates — how handled?" (idempotent consumer + dedup key; ch.19 MSG-03, XT-03). "What goes in the DLQ and who watches it?" (poison messages after 5 retries; alerting). "Exactly-once?" (not deliverable — effectively-once via dedup).

### B3 — Cross-system sync EPDDMS ↔ VDMS/EDMS, 10 days → under 2 days (ambiguity + correctness)
- **S:** Document transfer and review between Hexagon EPDDMS and VDMS/EDMS took ~10 days and was error-prone.
- **T:** Build a fault-tolerant real-time sync with high data accuracy, across systems I didn't fully control.
- **A:** I designed integration pipelines with retry mechanisms, input validation, structured exception handling, and **idempotent** handling so re-runs and retries couldn't corrupt or duplicate data. I treated every external response as untrusted and built for partial failure rather than assuming the happy path.
- **R:** ~99% data accuracy in real-time sync; transfer+review time dropped from ~10 days to under 2 days.
- **Likely follow-ups:** "How did idempotency actually work?" (dedup on a business key / unique constraint so retries are safe — ch.17 DBTX-09). "What happened on partial failure mid-sync?" (per-item handling + retry + DLQ-style recovery — ch.18 API-04). "How did you measure 99% accuracy?" (reconciliation checks between source and target).

### B4 — Mentoring 3 trainees (leadership without authority)
- **S:** Three trainees joined to work on a Java/Spring Boot document information-extraction service.
- **T:** Get them productive and raise code quality without just doing the work for them.
- **A:** I paired on the first features, set up review conventions, and taught the "why" (transaction boundaries, N+1, exception handling) rather than handing answers — the same first-principles approach I use myself. I let them own pieces and reviewed with specific, teachable feedback.
- **R:** They shipped independently on the service; I scaled my impact beyond my own commits.
- **Likely follow-ups:** "A trainee who struggled — what did you do?" (prepare a specific, honest example). "How do you give feedback that lands?" (specific, kind, tied to a principle).

### B5 — A failure / bug you owned (prepare this honestly — don't dodge)
- Pick a real one. A good shape: a change that caused a production issue (e.g., a retry/idempotency gap that caused duplicate sends, or an index change that regressed writes), **how you detected it, contained it, root-caused it, fixed it, and the guardrail you added so it can't recur** (ch.22 workflow). Interviewers value candid ownership + the prevention step far more than "I've never failed."
- **Why this matters:** dodging this question reads as low self-awareness. Having one ready signals maturity.

### B6 — Disagreement / influencing a decision (conflict)
- Shape: you believed a design was wrong (e.g., inline sending vs outbox, or synchronous vs async processing, or an N+1-prone approach). You **made the case with data/trade-offs, listened to the counter-view, and either persuaded or disagreed-and-committed.** End with the outcome and what you learned about communicating technical decisions.

### B7 — Prioritization across 6 portals + 4 services (scope under load)
- **S:** You owned backend work across 6 enterprise portals and 4 supporting services simultaneously.
- **A:** How you triaged by business impact and risk, communicated trade-offs to stakeholders, and avoided dropping critical work — plus the CI/CD improvement (30–45min → 5min) that reduced your own cycle time so you could ship more safely.
- **R:** 10+ workflow modules delivered; 60% less manual effort, 50% faster turnaround; deployment time cut ~85%.

---

## D. Delivery tips (how to say it)
- **Lead with the result sometimes** for impact questions ("I cut API latency ~90%; here's how"), then STAR the detail.
- **Quantify everything** you can defend. Vague = weak.
- **Own your part:** "I decided… I weighed… I verified…" The interviewer is scoring *you*, not the team.
- **Name the trade-off,** not just the choice: "I chose outbox over 2PC because…". This connects behavioral to your technical depth.
- **Have the follow-up ready:** every story needs a "why that way?" and a "what would you do differently?".
- **Keep it ~2–3 minutes;** stop and let them probe.

## E. Common pitfalls
- Rambling with no structure (use STAR).
- All "we", no "I" — invisible contribution.
- No numbers / no measurable result.
- Claiming no failures or no disagreements (reads as inexperienced or evasive).
- Blaming others in the conflict/failure story.
- Technical story with no decision — just listing tasks.

## F. Prep checklist
- [ ] 6–8 stories written in STAR, each with 2 follow-ups rehearsed.
- [ ] At least one honest failure story with a prevention step.
- [ ] At least one conflict/disagreement story ending in a decision.
- [ ] A mentoring story (you have one — use it).
- [ ] Every story has a defensible number.
- [ ] "Why this company / why SDE-2 now" prepared.
- [ ] Questions to ask the interviewer (team, on-call, growth) — asking good questions is scored.

## G. Rapid-fire bank (map each to a story above)
1. Most impactful thing you built? → B1 / B2
2. Hardest technical decision? → B2 (outbox vs 2PC)
3. A problem with no clear answer? → B3
4. A time you failed? → B5
5. Disagreed with a teammate/manager? → B6
6. Helped someone grow? → B4
7. Too much work — how did you prioritize? → B7
8. A production incident you handled? → B5 / B1 (debugging — ch.22)
9. Something you'd do differently? → have one per major story
10. Why leave your current role / why SDE-2? → growth, scope, systems ownership (be positive, not negative about current employer)

> Pair this chapter with mock reps: say each story out loud, timed, and have someone throw the follow-ups. Technical correctness (rest of this repo) + crisp behavioral delivery is the combination that converts an SDE-2 loop.
