package with;

/** Varies the parse step AND uses the optional hook. */
public class JsonProcessor extends DataProcessor {
    @Override
    protected void parse() {
        System.out.println("Parsing JSON");
    }

    @Override
    protected void afterSave() {
        System.out.println("Invalidating JSON cache"); // hook in action
    }
}
