package without;

/**
 * ISP VIOLATION in practice.
 *
 * This is a basic printer. It can only print. But the fat Machine
 * interface forces it to implement scan() and fax(), so it fills them
 * with throwing stubs. Callers holding a Machine reference can crash
 * at runtime by calling a method this device never supported.
 */
public class OldPrinter implements Machine {

    @Override
    public void print(String document) {
        System.out.println("Printing: " + document);
    }

    @Override
    public void scan(String document) {
        // Forced to implement something we don't have.
        throw new UnsupportedOperationException("This printer cannot scan");
    }

    @Override
    public void fax(String document) {
        // Another lie forced by the fat interface.
        throw new UnsupportedOperationException("This printer cannot fax");
    }
}
