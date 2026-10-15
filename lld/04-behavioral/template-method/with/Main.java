package with;

/**
 * Both share the exact same sequence (defined once). A fix to open/save/
 * close is made in ONE place and both processors inherit it.
 */
public class Main {
    public static void main(String[] args) {
        DataProcessor csv = new CsvProcessor();
        DataProcessor json = new JsonProcessor();

        System.out.println("-- CSV --");
        csv.process();

        System.out.println("-- JSON --");
        json.process();
    }
}
