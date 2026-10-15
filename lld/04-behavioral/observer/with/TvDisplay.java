package with;

/** Another concrete observer. */
public class TvDisplay implements Observer {
    @Override
    public void update(float temperature) {
        System.out.println("TV display: " + temperature + "C");
    }
}
