# Common Interviewer Pushbacks

> Related: [00-foundations/interview-communication-framework](../00-foundations/interview-communication-framework.md) · [architecture-defense-questions](./architecture-defense-questions.md)

Interviewers deliberately push back to test whether you reason, adapt, and hold your ground when right. This is a playbook of the common pushbacks and how to respond — the response matters more than being "right."

---

## The golden rule

Pushback is a *test of reasoning*, not necessarily a sign you're wrong. Don't cave reflexively ("you're right, let me change it") and don't dig in stubbornly. Restate the assumption your choice depends on, acknowledge the alternative, and either defend with a reason or adapt gracefully. **Revising well is a positive signal.**

---

## The pushback playbook (pushback → response)

**"I disagree with that choice."**
→ "I chose X assuming [requirement]. The alternative is Y, which trades [X] for [Y]. If the requirement is actually [Z], I'd switch to Y. Which is it?" (Surface the assumption; let them clarify.)

**"That won't scale."**
→ "At the current estimated scale it's sufficient; let me name the first bottleneck and when I'd evolve: [X] fails at [metric], at which point I'd add [Y]." (Show the evolution path, don't just defend.)

**"Isn't that over-engineered?"**
→ "You're right to question it — let me justify each component by the bottleneck it solves, and remove any that isn't justified at this scale." (Welcome the challenge; simplify.) Often the correct response is to *remove* complexity.

**"What if traffic grows 10×?"**
→ Re-estimate the pressured dimension, name the first bottleneck + matching fix, then the next bottleneck (see [scaling-followups](./scaling-followups.md)). Never "scale horizontally" alone.

**"I want stronger consistency here."**
→ "Then I'd read from the primary (not replicas) for that path, or use a quorum read, accepting higher latency and lower availability under partition. Here's the specific change..." (Name the exact change + its cost.)

**"What if [component] goes down?"**
→ Describe graceful degradation, not just failure: "If Redis is down, cache reads fall back to the DB behind a concurrency limiter + tight timeouts — slower but serving." (See [graceful-degradation](../06-distributed-systems/graceful-degradation.md), [redis-design-and-failure-modes](../04-caching/redis-design-and-failure-modes.md).)

**"You said exactly-once — is that really possible?"**
→ "Not exactly-once *delivery* — that's a myth over an unreliable network. I mean effectively-once: at-least-once delivery + idempotent processing (+ reconciliation for external effects)." (Correct your own imprecise claim honestly.) (See [exactly-once-processing](../06-distributed-systems/exactly-once-processing.md).)

**"Why not just use [trendy tech]?"**
→ "I'd use it if [its specific benefit] is a requirement here; otherwise it adds [cost] for no benefit. At this scale, [simpler choice] is sufficient." (Resist cargo-culting; justify by requirement.)

**"Your design has a bottleneck right there."**
→ "Good catch — you're right. I'd fix it by [specific mechanism], which trades [cost]." (Acknowledge, fix, name the trade-off — don't get defensive.)

**"I don't think that's how [technology] works."**
→ If unsure: "I may be misremembering the detail — the principle I'm relying on is [X]; let me reason from that." (Honest; reason from principles rather than bluffing. See "I don't know" below.)

**Silence / "...okay." (they're waiting for more)**
→ Proactively go deeper or offer a direction: "Should I detail the failure handling here, or move to how this evolves at scale?" (Drive the conversation.)

---

## Special situations

**You genuinely don't know a technology:**
→ Be honest + reason from first principles: "I haven't used [X] in production, but it's a [category] with [properties]; based on that it fits because [reason]. I'd verify the specifics." Bluffing is worse than honest reasoning. (See [interview-communication-framework](../00-foundations/interview-communication-framework.md).)

**You realize mid-interview your design is wrong:**
→ Say so and revise: "Given that, my earlier choice doesn't hold — let me revise." Graceful revision is a *strong* signal (it's the job).

**Time is running out and they keep pushing:**
→ "I'll note that as a risk to go deeper on; let me make sure I've summarized the core design first." (Manage time; close well.)

**They keep escalating scale/consistency:**
→ Keep applying the pattern (re-estimate → first bottleneck → fix → next bottleneck; or name the exact consistency change + cost). They're testing how far your reasoning goes, not expecting a planet-scale design on the spot.

---

## What pushbacks are really testing

- **Do you reason or recite?** (Can you defend with a requirement-grounded reason?)
- **Do you adapt or stubbornly dig in?** (Can you revise gracefully?)
- **Do you know the limits of your own claims?** (Will you correct "exactly-once"?)
- **Do you avoid cargo-culting?** (Will you resist trendy tech without a reason?)
- **Can you handle not knowing something?** (Honesty + first-principles over bluffing.)

---

## Exercise

Have someone throw 8 pushbacks at a design you did (mix: "won't scale," "over-engineered," "component down," "stronger consistency," "why not [trendy tech]," "that's a bottleneck," "exactly-once?," "I disagree"). Respond to each with the playbook pattern. Note any where you caved reflexively or dug in stubbornly — those are the ones to drill.

**Next:** [common-design-mistakes](./common-design-mistakes.md)
