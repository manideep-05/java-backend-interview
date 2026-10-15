package normal;

/** Order lifecycle (STATE). Kept as an enum here for brevity; the ATM/
 *  vending examples show the full state-class form if asked to expand. */
public enum OrderState {
    CREATED, PAID, CONFIRMED, CANCELLED
}
