package with;

/** Fills in only the step that differs. */
public class CsvProcessor extends DataProcessor {
    @Override
    protected void parse() {
        System.out.println("Parsing CSV");
    }
}
