package normal;

/**
 * STATE pattern interface. Each phase implements only the legal ops and
 * performs the transition. Illegal ops (e.g. withdraw before auth) are
 * rejected by the state.
 */
public interface ATMState {
    void insertCard(String cardNo);
    void enterPin(String pin);
    void withdraw(long amount);
    void ejectCard();
}
