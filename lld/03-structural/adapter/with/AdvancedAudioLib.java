package with;

/**
 * The "adaptee": a third-party/legacy class we cannot modify. Its
 * method name and shape don't match our MediaPlayer interface.
 */
public class AdvancedAudioLib {
    public void playMp3(String path) {
        System.out.println("Playing MP3 file: " + path);
    }
}
