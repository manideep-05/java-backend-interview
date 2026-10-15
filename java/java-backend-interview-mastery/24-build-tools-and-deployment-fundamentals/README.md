# 24 — Build Tools & Deployment Fundamentals

> **Priority: P2.** Enough to build, package, deploy, and operate a Java service confidently — Maven resolution, JAR vs WAR, Docker, config, graceful shutdown, basic CI/CD. Not a DevOps deep-dive.

**Prerequisites:** [10 Spring Boot internals](../10-spring-boot-internals/).
**Related:** [26 Observability](../26-observability-and-reliability/), [25 Security](../25-backend-security/).

---

## A. Concept & intuition
A build tool turns source + dependencies into a reproducible artifact; deployment runs that artifact reliably across environments. The goal is **one immutable artifact** promoted from dev → staging → prod, configured per environment via externalized config — not rebuilding per environment.

## B. Core concepts

### Maven
- **Lifecycle:** `validate → compile → test → package → verify → install → deploy`.
- **Dependency scopes:** `compile` (default), `provided` (at compile, supplied at runtime), `runtime`, `test`.
- **Transitive dependencies & mediation:** Maven resolves transitive deps and, on version conflicts, uses **nearest-wins** (shortest path in the tree); use `mvn dependency:tree` to debug conflicts and `<dependencyManagement>`/BOM to pin versions. Spring Boot's BOM pins a consistent set ([10](../10-spring-boot-internals/)).

### JAR vs WAR
- **Executable (fat) JAR:** Spring Boot's default — the app + embedded server + dependencies, run with `java -jar`. Simple, container-friendly, same artifact everywhere.
- **WAR:** deployed into an external servlet container (Tomcat/JBoss). For legacy/standardized app-server environments.

### Docker
- An image is layered; put rarely-changing layers (dependencies) first and your JAR last to maximize cache reuse and speed builds.
- Use a slim/distroless JRE base, run as a **non-root** user, set resource limits, and prefer **multi-stage builds** (build with the JDK, run with a JRE) to shrink images.
- The JVM in a container should respect cgroup limits (modern JDKs do by default) so heap sizing matches the container memory.

### Environment config & secrets
Externalize config (env vars/config server), inject secrets at runtime ([25](../25-backend-security/)), and keep the artifact identical across environments. Profiles select env-specific settings ([10](../10-spring-boot-internals/)).

### Graceful shutdown
On SIGTERM, stop accepting new requests, finish in-flight ones within a timeout, close pools/connections, then exit — so deploys/scaling don't drop requests. Spring Boot supports graceful shutdown (`server.shutdown=graceful` + a timeout).

### Health checks & deploy safety
Liveness/readiness ([26](../26-observability-and-reliability/)) gate traffic. Safe-release techniques: rolling, blue-green, canary — deploy to a subset, watch error/latency, then proceed or roll back. Keep migrations backward-compatible so old and new versions run together during a rollout.

### CI/CD
Pipeline: build → test → scan (CVE/static analysis) → package → deploy to staging → smoke test → promote. Fail fast on test/scan failures; keep the pipeline reproducible.

## D. Trade-offs

| Choice | Option A | Option B | Guidance |
|---|---|---|---|
| Packaging | fat JAR | WAR | fat JAR default; WAR for app servers |
| Image | single-stage | multi-stage | multi-stage: smaller, JDK-build/JRE-run |
| Config | per-env builds | one artifact + external config | one artifact always |
| Release | big-bang | rolling/canary | canary/rolling for safety |
| Dep conflict | ignore | `dependency:tree` + BOM | pin versions |

## E. Interview Q&A

### BLD-01 (P2) — How does Maven resolve a version conflict between transitive dependencies?
**Strong answer:** Maven builds the dependency tree and, when the same artifact appears at different versions, uses **nearest-wins** — the version closest to the project root (shortest path) is selected, with declaration order breaking ties at equal depth. I debug conflicts with `mvn dependency:tree`, and I pin versions explicitly via `<dependencyManagement>` or a BOM (like Spring Boot's) to avoid surprising transitive upgrades.

<a id="bld-02"></a>
### BLD-02 (P2) — JAR vs WAR; why does Spring Boot default to an executable JAR?
**Strong answer:** A fat/executable JAR bundles the app, its dependencies, and an embedded server so it runs with `java -jar` — the same immutable artifact in every environment, which suits containers and 12-factor deployment. A WAR needs an external servlet container and suits legacy/standardized app-server setups. I default to the executable JAR for simplicity and portability.

### BLD-03 (P2) — How do you build an efficient, secure Docker image for a Spring Boot app?
**Strong answer:** Use a multi-stage build (JDK to build, slim JRE/distroless to run), order layers so dependencies are cached before the app layer, run as a non-root user, set memory/CPU limits (modern JVMs respect cgroups for heap sizing), and avoid baking secrets into the image. This gives small, reproducible, safer images with fast rebuilds.

### BLD-04 (P2) — Why run one artifact across environments with external config?
**Strong answer:** Rebuilding per environment risks drift and "works in staging, breaks in prod" bugs. Promoting a single tested artifact and varying only externalized config (env vars/secret manager, profiles) guarantees what you tested is what you run, and keeps secrets out of the build.

### BLD-05 (P2) — What is graceful shutdown and why does it matter for deploys?
**Strong answer:** On SIGTERM the app stops accepting new requests, lets in-flight requests finish within a timeout, and closes resources before exiting, so rolling deploys and autoscaling don't drop live requests or corrupt in-flight work. Spring Boot enables it with `server.shutdown=graceful` and a configurable grace period, and I pair it with readiness probes so the LB stops routing before shutdown.

### BLD-06 (P2) — How do you release safely?
**Strong answer:** Rolling or canary deploys to a subset of instances while watching error rate and latency (SLOs), with automated rollback on regression, plus backward-compatible database migrations so the old and new versions can run simultaneously during the rollout. CI/CD runs tests and security scans before promotion.

## F. Production relevance
Build/deploy discipline prevents a class of outages: dependency-conflict bugs (pin with BOM, inspect `dependency:tree`), environment drift (one artifact + external config), dropped requests on deploy (graceful shutdown + readiness), oversized/insecure images (multi-stage, non-root, distroless), and risky releases (canary + backward-compatible migrations + rollback). Even for a backend-focused role, being able to reason about how your JAR becomes a running, observable, safely-deployable service rounds out the picture.

## G. Revision

### 10 key takeaways
1. One immutable artifact promoted across environments; config externalized.
2. Maven: nearest-wins conflict resolution; pin with BOM; debug with `dependency:tree`.
3. Executable fat JAR is the Boot default; WAR for app servers.
4. Multi-stage Docker builds: JDK build, JRE run; non-root; layer caching.
5. JVM respects container cgroup limits (modern JDKs).
6. Secrets injected at runtime, never in images.
7. Graceful shutdown: drain in-flight, then exit.
8. Liveness/readiness gate traffic during deploys.
9. Canary/rolling + backward-compatible migrations + rollback.
10. CI/CD: build → test → scan → package → deploy → promote.

### 10 rapid-fire
1. Maven conflict rule? (nearest-wins) 2. Inspect deps? (dependency:tree) 3. Boot default package? (executable JAR) 4. Smaller image? (multi-stage) 5. Run container as? (non-root) 6. Config per env? (external) 7. Drain on SIGTERM? (graceful shutdown) 8. Remove from LB? (readiness) 9. Safe release? (canary/rolling) 10. Pin versions? (BOM/dependencyManagement)

### 5 tricky
1. Why did a transitive dep upgrade unexpectedly? (nearest-wins) 2. Why rebuild-per-env causes bugs? (drift) 3. Why did deploys drop requests? (no graceful shutdown) 4. Why did the JVM OOM in a container with plenty of host RAM? (cgroup limit) 5. Why keep migrations backward-compatible during rollout?

### 5 exercises
1. Resolve a version conflict using `dependency:tree` + a BOM.
2. Write a multi-stage Dockerfile (JDK build → JRE run, non-root).
3. Enable graceful shutdown and verify in-flight requests complete.
4. Externalize config/secrets and run the same JAR in two "environments."
5. Sketch a canary release with rollback criteria and a backward-compatible migration.

### One-page revision sheet
> **One immutable artifact + external config** across envs. Maven: lifecycle, scopes, **nearest-wins** conflict → `dependency:tree` + BOM to pin. **Executable fat JAR** default (WAR for app servers). Docker: **multi-stage** (JDK build/JRE run), layer caching, **non-root**, cgroup-aware JVM, no secrets in image. **Graceful shutdown** (drain in-flight) + readiness gating. Release: canary/rolling + backward-compatible migrations + rollback. CI/CD: build→test→scan→package→deploy→promote.

### Prerequisites to revisit
- Config precedence/actuator (ch.10); health checks (ch.26); secrets (ch.25).
