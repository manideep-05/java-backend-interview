package with;

/** A concrete observer. */
public class PhoneDisplay implements Observer {
    @Override
    public void update(float temperature) {
        System.out.println("Phone display: " + temperature + "C");
    }
}
