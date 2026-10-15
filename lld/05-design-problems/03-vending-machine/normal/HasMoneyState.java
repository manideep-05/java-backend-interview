package normal;

/**
 * HasMoney: a product is selected; accept coins; dispense once enough is
 * inserted; allow cancel for a refund.
 */
public class HasMoneyState implements State {
    private final VendingMachine machine;

    public HasMoneyState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override
    public void selectProduct(String code) {
        System.out.println("Already selecting. Finish or cancel first.");
    }

    @Override
    public void insertMoney(int cents) {
        machine.addInserted(cents);
        System.out.println("Inserted total: " + machine.getInserted() + " cents.");
    }

    @Override
    public void dispense() {
        Product product = machine.getInventory().get(machine.getSelectedCode());
        if (machine.getInserted() < product.getPriceCents()) {
            System.out.println("Insufficient funds. Need " + product.getPriceCents()
                    + ", have " + machine.getInserted());
            return;
        }
        machine.setState(machine.getDispensingState()); // transition
        machine.getState().dispense(); // delegate the actual dispense
    }

    @Override
    public void cancel() {
        System.out.println("Refunding " + machine.getInserted() + " cents.");
        machine.reset();
    }
}
