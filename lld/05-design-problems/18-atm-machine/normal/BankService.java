package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * BankService: banking logic (auth + account lookup), kept separate from
 * the ATM hardware flow (SRP). The ATM depends on this, not on account
 * internals (DIP) — easy to swap for a fake in tests.
 */
public class BankService {
    private final Map<String, String> cardToPin = new HashMap<>();
    private final Map<String, Account> cardToAccount = new HashMap<>();

    public void register(String cardNo, String pin, Account account) {
        cardToPin.put(cardNo, pin);
        cardToAccount.put(cardNo, account);
    }

    public boolean authenticate(String cardNo, String pin) {
        return pin.equals(cardToPin.get(cardNo));
    }

    public Account getAccount(String cardNo) {
        return cardToAccount.get(cardNo);
    }
}
