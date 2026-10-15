package production;

/** Immutable product value object; price in cents (integer money). */
public final class Product {
    private final String code;
    private final int priceCents;

    public Product(String code, int priceCents) {
        this.code = code;
        this.priceCents = priceCents;
    }

    public String getCode() {
        return code;
    }

    public int getPriceCents() {
        return priceCents;
    }
}
