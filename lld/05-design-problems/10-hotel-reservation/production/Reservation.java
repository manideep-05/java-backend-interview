package production;

public final class Reservation {
    private final String roomId;
    private final String guest;
    private final DateRange range;

    public Reservation(String roomId, String guest, DateRange range) {
        this.roomId = roomId;
        this.guest = guest;
        this.range = range;
    }

    public String getRoomId() { return roomId; }
    public DateRange getRange() { return range; }
    public String getGuest() { return guest; }
}
