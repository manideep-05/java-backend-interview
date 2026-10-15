package production;

/** STATE interface (same contract; transitions guarded by the context's lock). */
public interface State {
    void selectProduct(String code);
    void insertMoney(int cents);
    void dispense();
    void cancel();
}
