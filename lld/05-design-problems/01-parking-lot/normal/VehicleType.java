package normal;

/**
 * Vehicle size categories. Using an enum (not raw strings) makes the
 * fits-in-spot logic type-safe and keeps the domain explicit.
 */
public enum VehicleType {
    MOTORCYCLE, CAR, BUS
}
