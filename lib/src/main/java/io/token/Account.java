/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token;

import io.token.proto.PagedList;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

import javax.annotation.Nullable;

/**
 * Represents a funding account in the Token system.
 */
public final class Account {
    private final AccountAsync async;

    /**
     * Creates an instance.
     *
     * @param async real implementation that the calls are delegated to
     */
    Account(AccountAsync async) {
        this.async = async;
    }

    /**
     * Returns an async version of the API.
     *
     * @return asynchronous version of the account API
     */
    public AccountAsync async() {
        return async;
    }

    /**
     * Gets a sync version of the Member API.
     *
     * @return account owner
     */
    public Member member() {
        return async.member().sync();
    }

    /**
     * Gets an account id.
     *
     * @return account id
     */
    public String id() {
        return async.id();
    }

    /**
     * Gets an account name.
     *
     * @return account name
     */
    public String name() {
        return async.name();
    }

    /**
     * Gets a bank ID.
     *
     * @return bank ID
     */
    public String bankId() {
        return async.bankId();
    }

    /**
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Money getBalance() {
        return async.getBalance().blockingSingle();
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token transfer.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction getTransaction(String transactionId) {
        return async.getTransaction(transactionId).blockingSingle();
    }

    /**
     * Looks up existing transactions by using an access token.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return list of transactions
     */
    public PagedList<Transaction, String> getTransactions(@Nullable String offset, int limit) {
        return async.getTransactions(offset, limit).blockingSingle();
    }

    @Override
    public int hashCode() {
        return async.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Account) {
            return async.equals(((Account) obj).async);
        } else {
            return async.equals(obj);
        }
    }
}