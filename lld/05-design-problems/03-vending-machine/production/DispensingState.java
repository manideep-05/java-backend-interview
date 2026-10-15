package production;

public class DispensingState implements State {
    private final VendingMachine machine;

    public DispensingState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override public void selectProduct(String code) { System.out.println("Please wait, dispensing."); }
    @Override public void insertMoney(int cents) { System.out.println("Please wait, dispensing. Money rejected."); }

    @Override
    public void dispense() {
        Product product = machine.getInventory().get(machine.getSelectedCode());
        // Atomic take: if a concurrent flow grabbed the last unit, refund.
        if (!machine.getInventory().decrementIfAvailable(product.getCode())) {
            System.out.println("Sold out during dispense; refunding " + machine.getInserted());
            machine.reset();
            return;
        }
        int change = machine.getInserted() - product.getPriceCents();
        System.out.println("Dispensed " + product.getCode() + ". Change: " + change + " cents.");
        machine.reset();
    }

    @Override public void cancel() { System.out.println("Too late to cancel; dispensing."); }
}
