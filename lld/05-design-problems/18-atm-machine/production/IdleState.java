package production;

public class IdleState implements ATMState {
    private final ATM atm;

    public IdleState(ATM atm) {
        this.atm = atm;
    }

    @Override
    public void insertCard(String cardNo) {
        atm.setCurrentCard(cardNo);
        System.out.println("Card inserted. Enter PIN.");
        atm.setState(atm.getHasCardState());
    }

    @Override public void enterPin(String pin) { System.out.println("Insert card first."); }
    @Override public void withdraw(long amount) { System.out.println("Insert card first."); }
    @Override public void ejectCard() { System.out.println("No card to eject."); }
}
