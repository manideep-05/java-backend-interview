package production;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BankService backed by concurrent maps so registration/lookup are safe
 * under concurrent ATM access. Banking logic stays separate from ATM
 * hardware (SRP, DIP).
 */
public class BankService {
    private final Map<String, String> cardToPin = new ConcurrentHashMap<>();
    private final Map<String, Account> cardToAccount = new ConcurrentHashMap<>();

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
