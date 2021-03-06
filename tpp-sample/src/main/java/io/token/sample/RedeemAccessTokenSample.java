package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.token.proto.PagedList;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.StandingOrder;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.tpp.Account;
import io.token.tpp.Member;
import io.token.tpp.Representable;

import java.util.List;

/**
 * Redeems an information access token.
 */
public final class RedeemAccessTokenSample {
    /**
     * Redeems access token to acquire access to the grantor's account balances.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return balance of one of grantor's accounts
     */
    public static Money redeemBalanceAccessToken(Member grantee, String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the data we want
        Money balance = accounts.get(0).getBalanceBlocking(STANDARD).getCurrent();
        // forAccessToken snippet end
        return balance;
    }

    /**
     * Redeems access token to acquire access to the grantor's account transactions.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return transaction history of one of grantor's accounts
     */
    public static List<Transaction> redeemTransactionsAccessToken(Member grantee, String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the 10 most recent transactions
        PagedList<Transaction, String> transactions = accounts.get(0)
                .getTransactionsBlocking(null, 10, STANDARD);

        // Get the 10 most recent transactions in the specified range
        PagedList<Transaction, String> transactionsByDate = accounts.get(0)
                .getTransactionsBlocking(null, 10, STANDARD, "2019-01-15", "2022-01-15");

        // Pass this offset to the next getTransactions
        // call to fetch the next page of transactions.
        String nextOffset = transactions.getOffset();

        return transactions.getList();
    }

    /**
     * Redeems access token to acquire access to the grantor's standing orders at an account.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return standing orders of one of grantor's accounts
     */
    public static List<StandingOrder> redeemStandingOrdersAccessToken(
            Member grantee,
            String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the first 5 standing orders
        PagedList<StandingOrder, String> standingOrders = accounts.get(0)
                .getStandingOrdersBlocking(null, 5, STANDARD);

        // Pass this offset to the next getStandingOrders
        // call to fetch the next page of standing orders.
        String nextOffset = standingOrders.getOffset();

        return standingOrders.getList();
    }
}
