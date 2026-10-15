package without;

import java.util.ArrayList;
import java.util.List;

/**
 * PROTOTYPE NOT USED — the problem.
 *
 * Every enemy runs an expensive constructor that reloads the same
 * shared configuration (abilities list) from "disk". Spawning 100
 * identical enemies pays that cost 100 times.
 */
public class Enemy {

    private String type;
    private int health;
    private List<String> abilities;

    public Enemy(String type, int health) {
        this.type = type;
        this.health = health;

        // Pretend this is an expensive load (file/db/network).
        System.out.println("Loading abilities config from disk...");
        this.abilities = new ArrayList<>();
        this.abilities.add("melee");
        this.abilities.add("block");
    }

    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public String toString() {
        return type + "(hp=" + health + ", abilities=" + abilities + ")";
    }

    public static void main(String[] args) {
        // Each `new` reloads the config — wasteful.
        Enemy orc1 = new Enemy("Orc", 100);
        Enemy orc2 = new Enemy("Orc", 100);
        orc2.setHealth(80);
        System.out.println(orc1);
        System.out.println(orc2);
    }
}
