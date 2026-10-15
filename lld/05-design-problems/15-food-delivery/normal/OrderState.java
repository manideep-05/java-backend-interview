package normal;

/** Order lifecycle. Transitions are guarded in Order. */
public enum OrderState {
    PLACED, ACCEPTED, PREPARING, READY, ASSIGNED, PICKED_UP, DELIVERED, CANCELLED
}
