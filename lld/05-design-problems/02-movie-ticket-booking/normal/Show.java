package normal;

import java.util.LinkedHashMap;
import java.util.Map;

/** A screening: movie + time, plus its seats keyed by seat id. */
public class Show {
    private final String id;
    private final String movie;
    private final Map<String, ShowSeat> seats = new LinkedHashMap<>();

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
