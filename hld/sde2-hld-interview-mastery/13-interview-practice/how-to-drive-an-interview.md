# How to Drive an HLD Interview

> Prerequisites: [00-foundations/interview-communication-framework](../00-foundations/interview-communication-framework.md)
> Related: [common-interviewer-pushbacks](./common-interviewer-pushbacks.md) · [mock-interviews](./mock-interviews.md)

The framework chapter taught the 11-step flow. This chapter is about *driving* — owning the conversation, managing time, and reading the interviewer — so the structure becomes natural, not robotic.

---

## The mindset: you lead, they steer

An HLD interview is a conversation you drive, with the interviewer occasionally steering. If you wait to be asked questions, you look passive and run out of time. You proactively: clarify, estimate, propose, and then say "let me go deeper on X, or would you prefer Y?" — giving them the wheel at decision points.

## The time budget (45 min) — a guide, not a cage

| Phase | ~Minutes | Signal of trouble |
|---|---|---|
| Clarify + scope + assumptions | 5 | Still clarifying at minute 12 |
| Estimate | 4 | No numbers by minute 10 |
| API + data model | 7 | — |
| Architecture + data flow | 11 | — |
| Bottlenecks + scaling | 8 | Haven't drawn anything by minute 20 |
| Consistency + failures | 5 | Never reached failures |
| Security/observability + summary | 3–5 | No summary |

Leave ~2 min buffer. **By minute ~30 you should be discussing bottlenecks/failures**, not still drawing the happy path. If you're behind, compress: state assumptions faster, pick the obvious design, move on.

## Reading and following the interviewer

- **They go quiet when they're satisfied** → move to the next phase.
- **They dwell on an area / ask a pointed question** → that's where the signal is; go deeper there (follow them).
- **They say "let's assume X"** → take it, don't re-litigate.
- **They push back** → they want to see you reason/adapt, not cave or dig in (see [common-interviewer-pushbacks](./common-interviewer-pushbacks.md)).
- **They look confused** → you're unclear; restate and check in.

## Habits that make you look senior

1. **Narrate reasoning, not just conclusions** ("I'll start simple with X because [requirement]; I'd add Y when [metric]").
2. **State a trade-off for every real decision** (gain + cost + alternative).
3. **Default to simple; justify every addition with a number** (resist over-engineering).
4. **Think aloud during silence** ("let me reason about consistency here...").
5. **Check in at decision points** ("want me to go deeper on the queue or move to failures?").
6. **Revise gracefully when challenged** (a positive signal — see [architecture-defense-questions](./architecture-defense-questions.md)).

## Running behind — how to recover

- Skip ahead to the architecture with stated assumptions ("I'll assume these numbers and come back if needed").
- Pick the obvious choice and name the alternative in one sentence instead of debating it.
- Prioritize: a complete simple design + failure handling beats an elaborate half-design.

## The close

Spend the last ~3 minutes on a **summary**: restate the design in 4–5 sentences, name the key trade-offs, and the top risks/next steps ("the main risks are the celebrity fan-out and the DLQ strategy; next I'd detail the reconciliation job"). Invite follow-ups. A crisp close leaves a strong final impression.

## Practice

Record yourself doing a full 45-minute design (use any problem from [12-system-design-problems](../12-system-design-problems/)). Afterward, check: Did you clarify in ~5 min? Estimate with shown math? Reach failures by ~30? Summarize at the end? Narrate trade-offs? Score with [INTERVIEW-CHECKLIST](../INTERVIEW-CHECKLIST.md).

---

**Next:** [requirement-clarification-drills](./requirement-clarification-drills.md)
