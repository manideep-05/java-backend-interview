package production;

import java.util.List;
import java.util.Map;

/**
 * FACADE. Thread-safe because the BalanceSheet it delegates to is
 * internally synchronized. The service itself is stateless beyond that
 * reference, so no additional locking is needed here.
 */
public class ExpenseService {
    private final BalanceSheet balanceSheet = new BalanceSheet();

    public void addExpense(String payer, long totalCents, List<String> participants,
                           SplitStrategy strategy, long[] args) {
        Map<String, Long> shares = strategy.computeShares(totalCents, participants, args);
        for (Map.Entry<String, Long> e : shares.entrySet()) {
            balanceSheet.addDebt(e.getKey(), payer, e.getValue());
        }
    }

    public BalanceSheet getBalanceSheet() {
        return balanceSheet;
    }

    public static void main(String[] args) throws InterruptedException {
        ExpenseService service = new ExpenseService();

        // Many concurrent expenses, all equal-split dinners paid by alice.
        Runnable worker = () -> {
            for (int i = 0; i < 100; i++) {
                service.addExpense("alice", 300, List.of("alice", "bob", "carol"),
                        new EqualSplitStrategy(), new long[0]);
            }
        };
        Thread t1 = new Thread(worker), t2 = new Thread(worker);
        t1.start(); t2.start();
        t1.join(); t2.join();

        // 200 dinners x 100c share each => bob owes alice 20000, carol 20000.
        // No lost updates thanks to the write lock on addDebt.
        service.getBalanceSheet().print();
    }
}
