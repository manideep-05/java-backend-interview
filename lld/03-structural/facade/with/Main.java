package with;

import with.Subsystem.Amplifier;
import with.Subsystem.Lights;
import with.Subsystem.Player;
import with.Subsystem.Projector;

/**
 * The client makes ONE call. The orchestration lives in the facade,
 * so changing the startup sequence is a one-place edit.
 */
public class Main {
    public static void main(String[] args) {
        HomeTheaterFacade theater = new HomeTheaterFacade(
                new Amplifier(), new Lights(), new Projector(), new Player());

        theater.watchMovie("Interstellar");
        theater.endMovie();
    }
}
