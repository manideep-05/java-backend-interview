package production;

/** Spot size categories plus the compatibility rule (unchanged logic). */
public enum SpotType {
    MOTORCYCLE, COMPACT, LARGE;

    public boolean canFit(VehicleType vehicle) {
        switch (vehicle) {
            case MOTORCYCLE:
                return true;
            case CAR:
                return this == COMPACT || this == LARGE;
            case BUS:
                return this == LARGE;
            default:
                return false;
        }
    }
}
