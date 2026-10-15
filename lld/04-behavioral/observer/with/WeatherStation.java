package with;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER APPLIED — the subject.
 *
 * Keeps a list of Observers and notifies them on change. It depends
 * ONLY on the Observer interface, so new observer types can be added
 * with no change to this class.
 */
public class WeatherStation {

    private final List<Observer> observers = new ArrayList<>();

    public void addObserver(Observer o) {
        observers.add(o);
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    public void setTemperature(float temp) {
        System.out.println("Station: temperature is now " + temp + "C");
        notifyObservers(temp);
    }

    private void notifyObservers(float temp) {
        for (Observer o : observers) {
            o.update(temp);
        }
    }
}
