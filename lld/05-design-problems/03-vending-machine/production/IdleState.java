package production;

public class IdleState implements State {
    private final VendingMachine machine;

    public IdleState(VendingMachine machine) {
        this.machine = machine;
    }

    @Override
    public void selectProduct(String code) {
        if (!machine.getInventory().isAvailable(code)) {
            System.out.println("Out of stock: " + code);
            return;
        }
        machine.setSelectedCode(code);
        System.out.println("Selected " + code + ". Please insert money.");
        machine.setState(machine.getHasMoneyState());
    }

    @Override public void insertMoney(int cents) { System.out.println("Select a product first."); }
    @Override public void dispense() { System.out.println("Select and pay first."); }
    @Override public void cancel() { System.out.println("Nothing to cancel."); }
}
