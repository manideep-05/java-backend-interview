package with;

import with.Subsystem.Amplifier;
import with.Subsystem.Lights;
import with.Subsystem.Player;
import with.Subsystem.Projector;

/**
 * FACADE APPLIED.
 *
 * Owns the subsystem parts and encapsulates the correct sequence behind
 * two simple methods. The client no longer needs to know the order or
 * even that these subsystems exist.
 */
public class HomeTheaterFacade {

    private final Amplifier amp;
    private final Lights lights;
    private final Projector projector;
    private final Player player;

    public HomeTheaterFacade(Amplifier amp, Lights lights, Projector projector, Player player) {
        this.amp = amp;
        this.lights = lights;
        this.projector = projector;
        this.player = player;
    }

    public void watchMovie(String movie) {
        System.out.println("--- Get ready to watch a movie ---");
        lights.dim(10);
        amp.on();
        amp.setVolume(5);
        projector.on();
        projector.wideScreen();
        player.on();
        player.play(movie);
    }

    public void endMovie() {
        System.out.println("--- Shutting down the theater ---");
        lights.dim(100);
    }
}
