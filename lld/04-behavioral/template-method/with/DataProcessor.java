package with;

/**
 * TEMPLATE METHOD APPLIED — the base class.
 *
 * process() is the TEMPLATE: it fixes the sequence of steps and is
 * final so subclasses can't change the structure. The shared steps are
 * implemented here once; only parse() is abstract (varies by format).
 * afterSave() is an optional HOOK with a default no-op.
 */
public abstract class DataProcessor {

    // The template method — fixed skeleton, cannot be overridden.
    public final void process() {
        open();
        parse();     // subclass-specific
        save();
        afterSave(); // optional hook
        close();
    }

    private void open() {
        System.out.println("Opening file");
    }

    // The step that varies — subclasses MUST provide it.
    protected abstract void parse();

    private void save() {
        System.out.println("Saving to DB");
    }

    // Optional hook — subclasses MAY override; default does nothing.
    protected void afterSave() {
    }

    private void close() {
        System.out.println("Closing file");
    }
}
