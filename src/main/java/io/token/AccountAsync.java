package io.token;

import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.rpc.Client;
import rx.Observable;

import java.util.List;
import java.util.Optional;

/**
 * Represents a funding account in the Token system.
 */
public final class AccountAsync {
    private final MemberAsync member;
    private final AccountProtos.Account.Builder account;
    private final Client client;
    private final Optional<String> onBehalfOf;

    /**
     * @param member account owner
     * @param account account information
     * @param client RPC client used to perform operations against the server
     */
    AccountAsync(MemberAsync member, AccountProtos.Account account, Client client) {
       this(member, account, client, Optional.empty());
    }

    /**
     * @param member account owner
     * @param account account information
     * @param client RPC client used to perform operations against the server
     * @param onBehalfOf the On-Behalf-Of value to be used with this account
     */
    AccountAsync(MemberAsync member, AccountProtos.Account account, Client client, Optional<String> onBehalfOf) {
        this.member = member;
        this.account = account.toBuilder();
        this.client = client;
        this.onBehalfOf = onBehalfOf;
    }

    /**
     * @return synchronous version of the account API
     */
    public Account sync() {
        return new Account(this);
    }

    /**
     * @return account owner
     */
    public MemberAsync member() {
        return member;
    }

    /**
     * @return account id
     */
    public String id() {
        return account.getId();
    }

    /**
     * @return account name
     */
    public String name() {
        return account.getName();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public Observable<Void> setAccountName(String newName) {
        return client
                .setAccountName(account.getId(), newName)
                .map(a -> {
                    this.account.clear().mergeFrom(a);
                    return null;
                });
    }

    /**
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Observable<Money> getBalance() {
        return client.getBalance(account.getId(), onBehalfOf);
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(String transactionId) {
        return client.getTransaction(account.getId(), transactionId, onBehalfOf);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token payments
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment record
     */
    public Observable<List<Transaction>> getTransactions(int offset, int limit) {
        return client.getTransactions(account.getId(), offset, limit, onBehalfOf);
    }
}
