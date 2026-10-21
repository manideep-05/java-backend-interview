package com.concurrency.locking.database.service;

import com.concurrency.locking.database.entity.BankAccount;
import com.concurrency.locking.database.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * <h1>DEADLOCK AVOIDANCE PATTERN: DETERMINISTIC RESOURCE ORDERING</h1>
 *
 * <h2>The Classic Bank Transfer Deadlock:</h2>
 * <pre>
 * Thread 1: Transfer Account 1 -> Account 2
 *   Step 1: Lock Account 1 (Success)
 *   Step 2: Try to Lock Account 2 (Wait...)
 *
 * Thread 2: Transfer Account 2 -> Account 1
 *   Step 1: Lock Account 2 (Success)
 *   Step 2: Try to Lock Account 1 (Wait...)
 *
 * -> BOTH THREADS ARE MUTUALLY WAITING FOR EACH OTHER. DEADLOCK!
 * </pre>
 *
 * <h2>The 4 Coffman Conditions for Deadlock:</h2>
 * <ol>
 *   <li><b>Mutual Exclusion:</b> Resources cannot be shared simultaneously.</li>
 *   <li><b>Hold and Wait:</b> Thread holds resource A while waiting for resource B.</li>
 *   <li><b>No Preemption:</b> Resources cannot be forcibly taken away.</li>
 *   <li><b>Circular Wait:</b> Thread 1 waits for Thread 2, which waits for Thread 1.</li>
 * </ol>
 *
 * <h2>The Production Fix (Breaking Circular Wait):</h2>
 * <p>Enforce a <b>Global Deterministic Ordering</b> of locks.
 * Always acquire locks in ascending order of primary keys (e.g. {@code fromId < toId ? lock from then to : lock to then from}).
 * This mathematically destroys the Circular Wait condition!</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlockFreeTransferService {

    private final BankAccountRepository bankAccountRepository;

    /**
     * PRODUCTION-GRADE DEADLOCK-FREE TRANSFER.
     * Orders lock acquisition by Account ID so circular waits are impossible.
     */
    @Transactional
    public boolean transferFundsDeadlockFree(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Determine deterministic lock acquisition order
        Long firstLockId = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        Long secondLockId = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        log.info("Locking in deterministic order: firstId={}, secondId={}", firstLockId, secondLockId);

        // Step 1: Lock smaller ID first
        BankAccount firstAccount = bankAccountRepository.findByIdWithPessimisticLock(firstLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstLockId));

        // Step 2: Lock larger ID second
        BankAccount secondAccount = bankAccountRepository.findByIdWithPessimisticLock(secondLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondLockId));

        // Identify which is source and destination
        BankAccount fromAccount = firstAccount.getId().equals(fromAccountId) ? firstAccount : secondAccount;
        BankAccount toAccount = secondAccount.getId().equals(toAccountId) ? secondAccount : firstAccount;

        // Perform balance verification and transfer
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Transfer failed: Insufficient funds in account {}", fromAccountId);
            return false;
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        log.info("Successfully transferred {} from account {} to {}", amount, fromAccountId, toAccountId);
        return true;
    }
}
