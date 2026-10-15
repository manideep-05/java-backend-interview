# Message Producer & Consumer (Java / Spring Boot + RabbitMQ)

> Concept: [queues-vs-pub-sub-vs-event-streams](../05-messaging-and-event-driven-systems/queues-vs-pub-sub-vs-event-streams.md) · [rabbitmq-internals-and-routing](../05-messaging-and-event-driven-systems/rabbitmq-internals-and-routing.md) · [message-delivery-semantics](../05-messaging-and-event-driven-systems/message-delivery-semantics.md)

**HLD point:** decouple slow/heavy work from the request path via a durable queue; producers publish, competing consumers process. Delivery is **at-least-once** → consumers must be **idempotent** and **ack after processing**.

**Where the concern lives:** the **broker** (RabbitMQ) does routing/delivery/durability; the app produces/consumes idempotently.

```java
// Topology: a durable topic exchange routing to a durable queue (survives broker restart).
@Configuration
class MessagingConfig {
    @Bean TopicExchange notifyExchange() { return new TopicExchange("notify.exchange", true, false); }
    @Bean Queue emailQueue() { return QueueBuilder.durable("notify.email")
            .withArgument("x-dead-letter-exchange", "notify.dlx")   // DLQ wiring (see DLQ example)
            .build(); }
    @Bean Binding bind() { return BindingBuilder.bind(emailQueue())
            .to(notifyExchange()).with("notify.email.*"); }
}
```

```java
// Producer: publish with a routing key. (Pair with the outbox for atomic DB+publish — see outbox example.)
@Service
class NotificationPublisher {
    private final RabbitTemplate rabbit;
    void publishEmail(NotificationEvent evt) {
        rabbit.convertAndSend("notify.exchange", "notify.email.sent", evt);
    }
}
```

```java
// Consumer: MANUAL ack after successful processing; idempotent via dedup store.
@Component
class EmailConsumer {
    private final EmailProvider provider;
    private final SentRepo sent; // unique (notificationId) -> dedup

    @RabbitListener(queues = "notify.email", ackMode = "MANUAL")
    public void onMessage(NotificationEvent evt, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            // Idempotency: atomic claim BEFORE the side effect (unique constraint).
            if (!sent.claim(evt.notificationId())) {   // already sent -> skip (redelivery)
                channel.basicAck(tag, false);
                return;
            }
            provider.send(evt);                        // the side effect (external)
            channel.basicAck(tag, false);              // ack AFTER success -> at-least-once
        } catch (TransientException e) {
            channel.basicNack(tag, false, false);      // -> DLX after retry policy (poison handling)
        }
    }
}
```

**Key points to say:**
- **Ack after processing** (manual ack) → at-least-once; a crash before ack redelivers (no lost message), so consumers are **idempotent** (dedup on `notificationId`). This is "at-least-once + idempotency = effectively-once."
- **Durable** exchange/queue + **persistent** messages so a broker restart doesn't lose work; publisher confirms for can't-lose events.
- **Competing consumers** scale throughput; **prefetch** (QoS) gives fair dispatch + backpressure.
- Pair the publish with the **transactional outbox** so the DB write and the publish are atomic (avoid dual-write; see [transactional-outbox-example](./transactional-outbox-example.md)).
- Route failures to a **DLQ** via a dead-letter exchange (see [dead-letter-handling-example](./dead-letter-handling-example.md)).

**Caveat:** production needs publisher confirms, prefetch tuning, a retry policy before DLX, and the idempotency store sized/TTL'd.

**Maps directly to your resume:** this *is* your Node.js/RabbitMQ notification platform (outbox + idempotent consumers + DLQ), expressed in Spring/Java. In an interview, say "I built exactly this in production."
