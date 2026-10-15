package normal;

/**
 * Dispensing: terminal action phase. Rejects money/selection/cancel to
 * avoid interleaving, dispenses the product, returns change, resets to Idle.
 */
public class DispensingState implements State {
    private final VendingMachine machine;

    public DispensingState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override
    public void selectProduct(String code) {
        System.out.println("Please wait, dispensing.");
    }

    @Override
    public void insertMoney(int cents) {
        System.out.println("Please wait, dispensing. Money rejected.");
    }

    @Override
    public void dispense() {
        Product product = machine.getInventory().get(machine.getSelectedCode());
        machine.getInventory().decrement(product.getCode());
        int change = machine.getInserted() - product.getPriceCents();
        System.out.println("Dispensed " + product.getCode() + ". Change: " + change + " cents.");
        machine.reset(); // back to Idle
    }

    @Override
    public void cancel() {
        System.out.println("Too late to cancel; dispensing.");
    }
}
