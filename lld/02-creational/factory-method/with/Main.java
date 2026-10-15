package with;

/**
 * The client picks a creator; the planning logic is shared and never
 * touches a concrete product. Adding AirLogistics + Plane needs no edit
 * to Logistics.planDelivery or to existing creators.
 */
public class Main {
    public static void main(String[] args) {
        Logistics road = new RoadLogistics();
        Logistics sea = new SeaLogistics();

        road.planDelivery(); // Truck
        sea.planDelivery();  // Ship
    }
}
