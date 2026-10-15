# Metrics and Tracing (Java / Spring Boot + Micrometer / OpenTelemetry)

> Concept: [logs-metrics-and-traces](../09-observability-and-operations/logs-metrics-and-traces.md) · [distributed-tracing](../09-observability-and-operations/distributed-tracing.md) · [alerting-and-dashboards](../09-observability-and-operations/alerting-and-dashboards.md)

**HLD point:** you can't diagnose what you can't see. Emit **metrics** (aggregate health, for alerting), **traces** (cross-service latency localization), and **structured logs with a correlation id** — so you diagnose from signals, not guesswork.

**Where the concern lives:** app emits (Micrometer/OTel); infra collects (Prometheus/TSDB, a tracing backend, log aggregation).

```java
// Metrics via Micrometer: RED (Rate, Errors, Duration). Timers give you p99, not just average.
@Service
class DocumentService {
    private final MeterRegistry meter;

    public Document upload(UploadCmd cmd) {
        Timer.Sample sample = Timer.start(meter);
        try {
            Document d = doUpload(cmd);
            meter.counter("doc.upload", "result", "success").increment();
            return d;
        } catch (RuntimeException e) {
            meter.counter("doc.upload", "result", "error").increment(); // error rate (symptom alert)
            throw e;
        } finally {
            sample.stop(Timer.builder("doc.upload.latency")
                .publishPercentiles(0.5, 0.95, 0.99)   // SLO on p99, NOT average
                .register(meter));
        }
    }
}
```

```java
// Tracing via Micrometer Tracing / OpenTelemetry: one trace id across services.
@Observed(name = "doc.process")   // creates a span; context propagates to downstream calls
public void process(Long docId) {
    virusScan(docId);   // child span
    ocr(docId);         // child span
}
// Trace id + span context auto-propagate over HTTP/messaging headers so you can see the
// whole request path and WHICH span is slow/failing (localize cross-service latency).
```

```java
// Structured logging with a correlation id (ties logs<->traces for one request).
// Spring Boot auto-populates MDC [traceId, spanId] when tracing is enabled.
log.info("document processed docId={} status={}", docId, status);
// -> {"ts":..., "traceId":"abc123", "spanId":"def", "msg":"document processed docId=42 status=READY"}
// NEVER log secrets/PII.
```

```yaml
# Expose metrics + enable tracing (Prometheus scrape, OTLP export)
management:
  endpoints.web.exposure.include: health,prometheus,metrics
  tracing.sampling.probability: 0.1     # sample 10% of traces (tracing all is expensive)
  metrics.distribution.percentiles-histogram.http.server.requests: true
```

**Key points to say:**
- **Three pillars, three questions:** metrics = *is something wrong & how much* (aggregate, cheap, for alerting); traces = *where* (which span/service); logs = *why* (exact error). Debug flow: metrics alert → trace localizes → logs explain (tied by the **correlation/trace id**).
- **SLO on p99, not average** — averages hide the tail that fan-out amplifies (see [logs-metrics-and-traces](../09-observability-and-operations/logs-metrics-and-traces.md)).
- **Sample traces** (10% here) — tracing everything is expensive; tail-based sampling keeps the slow/error traces (see [distributed-tracing](../09-observability-and-operations/distributed-tracing.md)).
- **Alert on symptoms** (error rate, p99, SLO burn), not causes (CPU) — causes go on dashboards (see [alerting-and-dashboards](../09-observability-and-operations/alerting-and-dashboards.md)).
- **Never log secrets/PII**; use structured logs so they're queryable.

**Caveat:** production needs cardinality control on metric labels (avoid high-cardinality tags), log sampling/retention (cost), tail-based trace sampling, and PII redaction.

**Maps to your resume:** your "exceptions → DB error table → email alerts" is basic observability; this is the upgrade path — structured logs + metrics + traces + symptom/burn-rate alerting (which avoids the alert fatigue of per-exception emails). Great to mention as "how I'd evolve it."
