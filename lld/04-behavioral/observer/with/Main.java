package with;

/**
 * Observers subscribe themselves. The station doesn't know their
 * concrete types and never changes when we add or remove one.
 */
public class Main {
    public static void main(String[] args) {
        WeatherStation station = new WeatherStation();

        Observer phone = new PhoneDisplay();
        Observer tv = new TvDisplay();
        station.addObserver(phone);
        station.addObserver(tv);

        station.setTemperature(25.0f); // both notified

        station.removeObserver(tv);
        station.setTemperature(30.0f); // only phone notified
    }
}
