package with;

/**
 * ISP APPLIED.
 *
 * A basic printer implements ONLY Printer. No throwing stubs, no lies.
 * There is no scan() or fax() for a caller to accidentally invoke.
 */
public class SimplePrinter implements Printer {
    @Override
    public void print(String document) {
        System.out.println("Printing: " + document);
    }
}
