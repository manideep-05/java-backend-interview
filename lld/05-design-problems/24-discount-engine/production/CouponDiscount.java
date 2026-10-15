package production;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single-use coupon code with ATOMIC redemption tracking.
 *
 * usedCodes is a concurrent set. redeem() does `usedCodes.add(code)` and
 * only applies the discount if add() returns true (i.e. the code wasn't
 * already redeemed). This is an atomic check-then-act, so two concurrent
 * checkouts can NEVER both redeem the same one-time coupon. Same idea as
 * payment idempotency.
 */
public final class CouponDiscount implements Discount {
    private final String code;
    private final long offCents;
    // Shared across checkouts: which codes have already been consumed.
    private final Set<String> usedCodes = ConcurrentHashMap.newKeySet();

    public CouponDiscount(String code, long offCents) {
        this.code = code;
        this.offCents = offCents;
    }

    @Override
    public boolean isApplicable(Cart cart) {
        return true; // eligibility checked at redeem time
    }

    @Override
    public long computeDiscount(long currentTotalCents, Cart cart) {
        // Atomic one-time redemption: add() returns false if already used.
        if (!usedCodes.add(code)) {
            return 0; // already redeemed by someone -> no discount
        }
        return Math.min(offCents, currentTotalCents);
    }

    @Override
    public String name() { return "coupon " + code; }
}
