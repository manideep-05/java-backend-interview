package without;

/**
 * OBSERVER NOT USED — the problem.
 *
 * The station hard-codes a direct call to every display it knows about.
 * It is tightly coupled to concrete displays, and adding a new display
 * (or removing one) means editing this class.
 */
public class WeatherStation {

    static class PhoneDisplay { void update(float t) { System.out.println("Phone: " + t + "C"); } }
    static class TvDisplay    { void update(float t) { System.out.println("TV: " + t + "C"); } }

    private final PhoneDisplay phone = new PhoneDisplay();
    private final TvDisplay tv = new TvDisplay();

    public void setTemperature(float temp) {
        // Must know and call each concrete display explicitly.
        phone.update(temp);
        tv.update(temp);
        // Adding a WebDisplay? Edit this method.
    }

    public static void main(String[] args) {
        new WeatherStation().setTemperature(25.0f);
    }
}
