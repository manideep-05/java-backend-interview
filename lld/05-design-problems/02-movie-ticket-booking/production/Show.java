package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A screening; seats in a ConcurrentHashMap for safe concurrent lookup. */
public class Show {
    private final String id;
    private final String movie;
    private final Map<String, ShowSeat> seats = new ConcurrentHashMap<>();

    public Show(String id, String movie, int seatCount) {
        this.id = id;
        this.movie = movie;
        for (int i = 1; i <= seatCount; i++) {
            String seatId = "S" + i;
            seats.put(seatId, new ShowSeat(seatId));
        }
    }

    public ShowSeat getSeat(String seatId) {
        return seats.get(seatId);
    }

    public String getId() {
        return id;
    }

    public String getMovie() {
        return movie;
    }
}
