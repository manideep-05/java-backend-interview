package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotent card payment.
 *
 * seen: idempotencyKey -> result. putIfAbsent is atomic, so even if two
 * threads retry the same key simultaneously, the charge "effect" is
 * recorded once and both callers observe the same result. No double charge.
 */
public class CardPayment implements PaymentStrategy {
    private final Map<String, Boolean> seen = new ConcurrentHashMap<>();

    @Override
    public boolean charge(long amountCents, String idempotencyKey) {
        // If we've already processed this key, return the stored result.
        Boolean prior = seen.get(idempotencyKey);
        if (prior != null) {
            System.out.println("Idempotent replay for key " + idempotencyKey + " -> " + prior);
            return prior;
        }
        // First time: perform the charge, then record the result atomically.
        boolean result = true; // pretend the gateway succeeded
        Boolean raced = seen.putIfAbsent(idempotencyKey, result);
        if (raced != null) {
            // Another thread recorded it first; honor that result.
            return raced;
        }
        System.out.println("Charged card: " + amountCents + " cents (key " + idempotencyKey + ")");
        return result;
    }
}
