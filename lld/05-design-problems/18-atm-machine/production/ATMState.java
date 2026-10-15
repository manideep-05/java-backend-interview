package production;

/** STATE interface (same contract as normal). */
public interface ATMState {
    void insertCard(String cardNo);
    void enterPin(String pin);
    void withdraw(long amount);
    void ejectCard();
}
