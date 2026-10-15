package with;

/**
 * Callers depend only on the capability they need. A function that
 * just prints asks for a Printer and can never accidentally call
 * scan() or fax() on a device that lacks them.
 */
public class Main {

    static void printDocument(Printer printer, String doc) {
        printer.print(doc);
    }

    public static void main(String[] args) {
        printDocument(new SimplePrinter(), "report.pdf");
        printDocument(new AllInOnePrinter(), "invoice.pdf");

        AllInOnePrinter mfp = new AllInOnePrinter();
        mfp.scan("contract.pdf");
        mfp.fax("contract.pdf");
    }
}
