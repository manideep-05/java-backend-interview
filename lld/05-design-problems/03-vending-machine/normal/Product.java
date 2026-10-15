package normal;

/** Product value object: a code and a price (in cents to avoid float money bugs). */
public class Product {
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
