package with;

/**
 * ISP APPLIED.
 *
 * A multifunction device genuinely HAS all three capabilities, so it
 * composes the three small interfaces. This is honest: it implements
 * exactly what it can do.
 */
public class AllInOnePrinter implements Printer, Scanner, Fax {
    @Override
    public void print(String document) {
        System.out.println("Printing: " + document);
    }

    @Override
    public void scan(String document) {
        System.out.println("Scanning: " + document);
    }

    @Override
    public void fax(String document) {
        System.out.println("Faxing: " + document);
    }
}
