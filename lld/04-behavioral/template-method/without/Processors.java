package without;

/**
 * TEMPLATE METHOD NOT USED — the problem.
 *
 * Both processors repeat the SAME overall sequence (open, parse, save,
 * close). Only the parse step differs. The shared structure is
 * duplicated, so a fix to open/save/close must be made in every class.
 */
public class Processors {

    static class CsvProcessor {
        void run() {
            System.out.println("Opening file");
            System.out.println("Parsing CSV");   // the only real difference
            System.out.println("Saving to DB");
            System.out.println("Closing file");
        }
    }

    static class JsonProcessor {
        void run() {
            System.out.println("Opening file");
            System.out.println("Parsing JSON");  // the only real difference
            System.out.println("Saving to DB");
            System.out.println("Closing file");
        }
    }

    public static void main(String[] args) {
        new CsvProcessor().run();
        new JsonProcessor().run();
    }
}
