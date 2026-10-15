package normal;

/**
 * STATE pattern — the machine's behavior interface.
 *
 * Each concrete state implements only the operations legal in that phase
 * and performs the transition to the next state. Illegal operations are
 * rejected by the state itself, so the context has no giant switch.
 */
public interface State {
    void selectProduct(String code);
    void insertMoney(int cents);
    void dispense();
    void cancel();
}
