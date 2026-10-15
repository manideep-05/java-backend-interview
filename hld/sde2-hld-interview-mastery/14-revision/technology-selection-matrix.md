# Technology Selection Matrix

> When to choose each technology and why. The architectural principle matters more than the specific product; AWS examples are illustrative, not prescriptive. Choose by requirement, not popularity.

## Backend
| Tech | Choose when | Avoid when |
|---|---|---|
| Java/Spring Boot | Enterprise, strong typing, mature ecosystem, team expertise | Ultra-lightweight scripts |
| Node.js | I/O-bound, high-concurrency, JS team, lightweight services | CPU-bound heavy computation |

## Relational databases
| Tech | Choose when | Notes |
|---|---|---|
| PostgreSQL | Default: rich features (JSONB, full-text, extensions), strong SQL | Great general-purpose default |
| MySQL | Mature, widely supported, read-heavy web | Fewer advanced features than PG |
| Oracle | Existing enterprise/licensing, specific features | Cost; avoid for greenfield unless required |

**All relational:** relationships + transactions + evolving queries; scale vertical → replicas → shard.

## NoSQL
| Tech | Choose when | Avoid when |
|---|---|---|
| DynamoDB (KV/wide-column) | Massive scale, known access patterns, managed, predictable latency | Ad-hoc queries, complex transactions |
| MongoDB (document) | Self-contained varied objects, flexible schema, whole-doc access | Highly relational data, cross-doc transactions |
| Cassandra (wide-column) | Write-heavy, append/time/event data, known partition access, huge scale | Ad-hoc queries, strong multi-row txns |

**Rule:** NoSQL for a specific access pattern at scale (know it first); not for "flexibility" when you have relationships.

## Caching
| Tech | Choose when |
|---|---|
| Redis (distributed) | Shared cache across instances, atomic ops (rate limit/locks/leaderboards), larger data |
| Caffeine (local) | Tiny, hot, change-tolerant data per instance; front hot keys; no cross-instance consistency needed |

Layer both (local L1 + Redis L2) for hot shared data; always define invalidation + Redis-down fallback.

## Messaging
| Tech | Choose when | Avoid when |
|---|---|---|
| Kafka | Retained/replayable log, multiple independent consumers, high throughput, event streaming/sourcing | Simple one-off task distribution (overkill) |
| RabbitMQ | Rich routing, priorities, per-message ack, task/work queues (no replay needed) | Extreme throughput + replay (use Kafka) |
| Amazon SQS | Managed simple queue, decoupling, at-least-once | Need replay/ordering across partitions |
| SNS | Pub/sub fan-out to many subscribers | Need durable replay (pair with SQS or Kafka) |

**All messaging:** at-least-once default → idempotent consumers + DLQ.

## Object storage / search / LB / compute
| Category | Choose | When |
|---|---|---|
| Object storage | S3-like | Blobs/files (never in a DB); pre-signed URLs; tiering for cost |
| Search | Elasticsearch/OpenSearch | Full-text/relevance/fuzzy/facets at scale (fed via CDC) |
| Search (small) | DB full-text (Postgres tsvector) | Modest full-text, no extra system, strong consistency |
| L4 load balancer | — | Raw throughput, TLS passthrough, non-HTTP |
| L7 load balancer | — | HTTP path/host routing, TLS termination (common default) |
| API gateway | — | Multiple services needing centralized authn/rate-limit/routing (not a single service) |

## Compute / orchestration / CDN
| Category | Choose | When |
|---|---|---|
| VMs | — | Full control, legacy, special needs |
| Containers (K8s) | — | Steady/varied workloads at scale, portability (ops cost) |
| Managed containers (Fargate/Cloud Run) | — | Steady services, avoid K8s ops |
| Serverless (Lambda) | — | Spiky/event-driven/low-volume short tasks (watch cold starts + DB connection fan-out) |
| Docker/K8s | — | Orchestration, service discovery, autoscaling at scale |
| CDN (CloudFront-like) | — | Static/media/cacheable content, global low latency, origin offload |

## Observability / identity / deployment
| Category | Choose | When |
|---|---|---|
| Metrics (TSDB/Prometheus) | — | Aggregate health, alerting; watch cardinality |
| Logs (ELK/search engine) | — | Full-text log search; buffer with Kafka; tier retention |
| Tracing (OpenTelemetry) | — | Cross-service latency/failure localization (microservices) |
| JWT | — | Stateless auth, horizontal scale (short TTL for revocation) |
| OAuth2 | — | Delegated access to resources |
| OIDC | — | Authentication/SSO ("Login with X") |
| Rolling / blue-green / canary | — | Routine / instant-cutover / risky changes |

## The meta-rule
Pick the **simplest technology that meets the requirement**, justify by a requirement/number, and know the one trade-off that would make you switch. AWS examples illustrate principles — the reasoning transfers to any cloud.

---
**Next:** [common-bottlenecks-and-solutions](./common-bottlenecks-and-solutions.md)
