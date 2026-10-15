package with;

import java.util.ArrayList;
import java.util.List;

/**
 * PROTOTYPE APPLIED.
 *
 * The expensive construction happens ONCE for the template. New enemies
 * are produced by copy() — a DEEP copy so each clone has its own
 * abilities list and mutating one never affects another.
 */
public class Enemy {

    private final String type;
    private int health;
    private final List<String> abilities;

    // Expensive "full" construction — used only for the template.
    public Enemy(String type, int health) {
        this.type = type;
        this.health = health;

        System.out.println("Loading abilities config from disk..."); // once
        this.abilities = new ArrayList<>();
        this.abilities.add("melee");
        this.abilities.add("block");
    }

    // Private copy constructor used by copy() for cloning.
    private Enemy(Enemy source) {
        this.type = source.type;
        this.health = source.health;
        // DEEP copy of the mutable list — not a shared reference.
        this.abilities = new ArrayList<>(source.abilities);
    }

    /** Produce an independent clone without rerunning expensive setup. */
    public Enemy copy() {
        return new Enemy(this);
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void addAbility(String ability) {
        this.abilities.add(ability);
    }

    @Override
    public String toString() {
        return type + "(hp=" + health + ", abilities=" + abilities + ")";
    }

    public static void main(String[] args) {
        // Build the template once (pays the cost once).
        Enemy template = new Enemy("Orc", 100);

        // Clone cheaply, then customize each copy independently.
        Enemy orc1 = template.copy();
        Enemy orc2 = template.copy();
        orc2.setHealth(80);
        orc2.addAbility("charge"); // only orc2 changes

        System.out.println(orc1); // melee, block
        System.out.println(orc2); // melee, block, charge  (deep copy proven)
    }
}
