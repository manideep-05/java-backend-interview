package normal;

import java.util.List;
import java.util.Map;

/**
 * FACADE: apply an expense to the balance sheet using a split strategy.
 * The payer covered the whole bill, so every OTHER participant owes the
 * payer their computed share.
 */
public class ExpenseService {
    private final BalanceSheet balanceSheet = new BalanceSheet();

    public void addExpense(String payer, long totalCents, List<String> participants,
                           SplitStrategy strategy, long[] args) {
        Map<String, Long> shares = strategy.computeShares(totalCents, participants, args);
        for (Map.Entry<String, Long> e : shares.entrySet()) {
            String participant = e.getKey();
            long share = e.getValue();
            // participant owes the payer their share (self-debt is ignored)
            balanceSheet.addDebt(participant, payer, share);
        }
    }

    public BalanceSheet getBalanceSheet() {
        return balanceSheet;
    }

    public static void main(String[] args) {
        ExpenseService service = new ExpenseService();
        // Alice pays $30 (3000c) dinner, split equally among Alice, Bob, Carol.
        service.addExpense("alice", 3000, List.of("alice", "bob", "carol"),
                new EqualSplitStrategy(), new long[0]);
        service.getBalanceSheet().print();
        // bob owes alice 1000, carol owes alice 1000 (alice's own share nets out)
    }
}
