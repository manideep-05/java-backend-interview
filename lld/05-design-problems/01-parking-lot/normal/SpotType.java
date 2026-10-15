package normal;

/**
 * Spot size categories and the rule of which vehicles each can hold.
 * Encapsulating "fits" here keeps the compatibility rule in one place
 * (SRP) instead of scattering if/else across the codebase.
 */
public enum SpotType {
    MOTORCYCLE, COMPACT, LARGE;

    public boolean canFit(VehicleType vehicle) {
        switch (vehicle) {
            case MOTORCYCLE:
                return true;                 // a motorcycle fits any spot
            case CAR:
                return this == COMPACT || this == LARGE;
            case BUS:
                return this == LARGE;        // (single-spot simplification)
            default:
                return false;
        }
    }
}
