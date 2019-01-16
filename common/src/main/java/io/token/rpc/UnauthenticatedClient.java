/**
 * Copyright (c) 2019 Token, Inc.
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

package io.token.rpc;

import static com.google.common.base.Strings.nullToEmpty;
import static io.token.proto.common.alias.AliasProtos.VerificationStatus.SUCCESS;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toObservable;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.MemberNotFoundException;
import io.token.exceptions.VerificationException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.blob.BlobProtos;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.BeginRecoveryRequest;
import io.token.proto.gateway.Gateway.BeginRecoveryResponse;
import io.token.proto.gateway.Gateway.CompleteRecoveryRequest;
import io.token.proto.gateway.Gateway.CompleteRecoveryResponse;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.GetBanksRequest;
import io.token.proto.gateway.Gateway.GetBanksResponse;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.InvalidateNotificationRequest;
import io.token.proto.gateway.Gateway.InvalidateNotificationResponse;
import io.token.proto.gateway.Gateway.ResolveAliasRequest;
import io.token.proto.gateway.Gateway.ResolveAliasResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.util.Converters;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Similar to {@link Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * getMember an existing one and switch to the authenticated {@link Client}.
 */
public class UnauthenticatedClient {
    protected final GatewayServiceFutureStub gateway;

    /**
     * Creates an instance.
     *
     * @param gateway gateway gRPC stub
     */
    public UnauthenticatedClient(GatewayServiceFutureStub gateway) {
        this.gateway = gateway;
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias already exists, {@code false} otherwise
     */
    public Observable<Boolean> aliasExists(Alias alias) {
        return toObservable(gateway
                .resolveAlias(ResolveAliasRequest
                        .newBuilder()
                        .setAlias(alias)
                        .build()))
                .map(new Function<ResolveAliasResponse, Boolean>() {
                    public Boolean apply(ResolveAliasResponse response) {
                        return response.hasMember();
                    }
                });
    }

    /**
     * Looks up member id for a given alias.
     *
     * @param alias alias to check
     * @return member id, or throws exception if member not found
     */
    public Observable<String> getMemberId(final Alias alias) {
        return toObservable(
                gateway.resolveAlias(ResolveAliasRequest.newBuilder()
                        .setAlias(alias)
                        .build()))
                .map(new Function<ResolveAliasResponse, String>() {
                    public String apply(ResolveAliasResponse response) {
                        if (response.hasMember()) {
                            return response.getMember().getId();
                        } else {
                            throw new MemberNotFoundException(alias);
                        }
                    }
                });
    }

    /**
     * Looks up member information for the given member ID. The user is defined by
     * the key used for authentication.
     *
     * @param memberId member id
     * @return an observable of member
     */
    public Observable<Member> getMember(String memberId) {
        return Converters
                .toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(new Function<GetMemberResponse, Member>() {
                    public Member apply(GetMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Creates new member ID. After the method returns the ID is reserved on the server.
     *
     * @param memberType the type of member to register
     * @param tokenRequestId (optional) token request id
     * @return newly created member id
     */
    public Observable<String> createMemberId(
            CreateMemberType memberType,
            @Nullable String tokenRequestId) {
        return
                toObservable(gateway.createMember(CreateMemberRequest.newBuilder()
                        .setNonce(generateNonce())
                        .setMemberType(memberType)
                        .setTokenRequestId(nullToEmpty(tokenRequestId))
                        .build()))
                        .map(new Function<CreateMemberResponse, String>() {
                            public String apply(CreateMemberResponse response) {
                                return response.getMemberId();
                            }
                        });
    }

    /**
     * Creates a new token member.
     *
     * @param memberId member ID
     * @param operations operations to set up member keys and aliases
     * @param metadata metadata of operations
     * @param signer the signer used to sign the requests
     * @return member information
     */
    public Observable<Member> createMember(
            String memberId,
            List<MemberOperation> operations,
            List<MemberOperationMetadata> metadata,
            Signer signer) {
        MemberUpdate.Builder update = MemberUpdate.newBuilder()
                .setMemberId(memberId)
                .addAllOperations(operations);
        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                .setUpdate(update)
                .setUpdateSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(signer.getKeyId())
                        .setSignature(signer.sign(update.build())))
                .addAllMetadata(metadata)
                .build()))
                .map(new Function<UpdateMemberResponse, Member>() {
                    public Member apply(UpdateMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Notifies subscribed devices that a key should be added.
     *
     * @param alias alias of the member
     * @param addKey the add key payload to be sent
     * @return status status of the notification
     */
    public Observable<NotifyStatus> notifyAddKey(Alias alias, AddKey addKey) {
        return toObservable(gateway.notify(
                Gateway.NotifyRequest.newBuilder()
                        .setAlias(alias)
                        .setBody(NotifyBody.newBuilder()
                                .setAddKey(addKey)
                                .build())
                        .build()))
                .map(new Function<Gateway.NotifyResponse, NotifyStatus>() {
                    public NotifyStatus apply(Gateway.NotifyResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Invalidate a notification.
     *
     * @param notificationId notification id to invalidate
     * @return status of the invalidation request
     */
    public Observable<NotifyStatus> invalidateNotification(String notificationId) {
        return toObservable(gateway.invalidateNotification(
                InvalidateNotificationRequest.newBuilder()
                        .setNotificationId(notificationId)
                        .build()))
                .map(new Function<InvalidateNotificationResponse, NotifyStatus>() {
                    public NotifyStatus apply(InvalidateNotificationResponse response) {
                        return response.getStatus();
                    }
                });
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getBlob(String blobId) {
        return toObservable(gateway
                .getBlob(Gateway.GetBlobRequest
                        .newBuilder()
                        .setBlobId(blobId)
                        .build()))
                .map(new Function<GetBlobResponse, BlobProtos.Blob>() {
                    public BlobProtos.Blob apply(GetBlobResponse response) {
                        return response.getBlob();
                    }
                });
    }

    /**
     * Begins account recovery.
     *
     * @param alias the alias used to recover
     * @return the verification id
     */
    public Observable<String> beginRecovery(Alias alias) {
        return toObservable(gateway
                .beginRecovery(BeginRecoveryRequest.newBuilder()
                        .setAlias(normalizeAlias(alias))
                        .build()))
                .map(new Function<BeginRecoveryResponse, String>() {
                    public String apply(BeginRecoveryResponse response) {
                        return response.getVerificationId();
                    }
                });
    }

    /**
     * Create a recovery authorization for some agent to sign.
     *
     * @param memberId Id of member we claim to be.
     * @param privilegedKey new privileged key we want to use.
     * @return authorization structure for agent to sign
     */
    public Observable<Authorization> createRecoveryAuthorization(
            final String memberId,
            final Key privilegedKey) {
        return toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                .setMemberId(memberId)
                .build()))
                .map(new Function<GetMemberResponse, Authorization>() {
                    public Authorization apply(GetMemberResponse response) {
                        return Authorization.newBuilder()
                                .setMemberId(memberId)
                                .setMemberKey(privilegedKey)
                                .setPrevHash(response.getMember().getLastHash())
                                .build();
                    }
                });
    }

    /**
     * Completes account recovery.
     *
     * @param memberId the member id
     * @param recoveryOperations the member recovery operations
     * @param privilegedKey the privileged public key in the member recovery operations
     * @param cryptoEngine the new crypto engine
     * @return an observable of the updatedMember
     */
    public Observable<Member> completeRecovery(
            final String memberId,
            final List<MemberRecoveryOperation> recoveryOperations,
            final Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        final Key standardKey = cryptoEngine.generateKey(STANDARD);
        final Key lowKey = cryptoEngine.generateKey(LOW);
        final Signer signer = cryptoEngine.createSigner(PRIVILEGED);
        final List<MemberOperation> operations = new LinkedList<>();
        for (MemberRecoveryOperation op : recoveryOperations) {
            operations.add(MemberOperation.newBuilder().setRecover(op).build());
        }
        return toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                .setMemberId(memberId)
                .build()))
                .map(new Function<GetMemberResponse, MemberUpdate>() {
                    public MemberUpdate apply(GetMemberResponse response) {
                        return MemberUpdate.newBuilder()
                                .setMemberId(memberId)
                                .setPrevHash(response.getMember().getLastHash())
                                .addAllOperations(operations)
                                .addAllOperations(toMemberOperations(
                                        privilegedKey,
                                        standardKey,
                                        lowKey))
                                .build();
                    }
                })
                .flatMap(new Function<MemberUpdate, Observable<Member>>() {
                    public Observable<Member> apply(MemberUpdate memberUpdate) {
                        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                                .setUpdate(memberUpdate)
                                .setUpdateSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(memberUpdate)))
                                .build()))
                                .map(new Function<UpdateMemberResponse, Member>() {
                                    public Member apply(UpdateMemberResponse response) {
                                        return response.getMember();
                                    }
                                });
                    }
                });
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @param cryptoEngine the new crypto engine
     * @return the recovery entry
     */
    public Observable<Member> completeRecoveryWithDefaultRule(
            final String memberId,
            final String verificationId,
            final String code,
            CryptoEngine cryptoEngine) {
        final Key privilegedKey = cryptoEngine.generateKey(PRIVILEGED);
        final Key standardKey = cryptoEngine.generateKey(STANDARD);
        final Key lowKey = cryptoEngine.generateKey(LOW);
        final Signer signer = cryptoEngine.createSigner(PRIVILEGED);
        return toObservable(gateway
                .completeRecovery(CompleteRecoveryRequest.newBuilder()
                        .setVerificationId(verificationId)
                        .setCode(code)
                        .setKey(privilegedKey)
                        .build()))
                .flatMap(new Function<CompleteRecoveryResponse, Observable<MemberUpdate>>() {
                    public Observable<MemberUpdate> apply(final CompleteRecoveryResponse res) {
                        return toObservable(gateway
                                .getMember(GetMemberRequest.newBuilder()
                                        .setMemberId(memberId)
                                        .build()))
                                .map(new Function<GetMemberResponse, MemberUpdate>() {
                                    public MemberUpdate apply(GetMemberResponse memberRes) {
                                        return MemberUpdate.newBuilder()
                                                .setPrevHash(memberRes.getMember().getLastHash())
                                                .setMemberId(memberId)
                                                .addOperations(MemberOperation.newBuilder()
                                                        .setRecover(res.getRecoveryEntry()))
                                                .addAllOperations(toMemberOperations(
                                                        privilegedKey,
                                                        standardKey,
                                                        lowKey))
                                                .build();
                                    }
                                });
                    }
                })
                .flatMap(new Function<MemberUpdate, Observable<Member>>() {
                    public Observable<Member> apply(MemberUpdate memberUpdate) {
                        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                                .setUpdate(memberUpdate)
                                .setUpdateSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(memberUpdate)))
                                .build()))
                                .map(new Function<UpdateMemberResponse, Member>() {
                                    public Member apply(UpdateMemberResponse response) {
                                        return response.getMember();
                                    }
                                });
                    }
                });
    }

    /**
     * Gets recovery authorization from Token.
     *
     * @param verificationId the verification id
     * @param code the code
     * @param privilegedKey the privileged key
     * @return the recovery entry
     * @throws VerificationException if the code verification fails
     */
    public Observable<MemberRecoveryOperation> getRecoveryAuthorization(
            String verificationId,
            String code,
            Key privilegedKey) throws VerificationException {
        return toObservable(gateway.completeRecovery(CompleteRecoveryRequest.newBuilder()
                .setVerificationId(verificationId)
                .setCode(code)
                .setKey(privilegedKey)
                .build()))
                .map(new Function<CompleteRecoveryResponse, MemberRecoveryOperation>() {
                    public MemberRecoveryOperation apply(CompleteRecoveryResponse response) {
                        if (response.getStatus() != SUCCESS) {
                            throw new VerificationException(response.getStatus());
                        }
                        return response.getRecoveryEntry();
                    }
                });
    }

    /**
     * Returns a list of token enabled banks.
     *
     * @param bankIds If specified, return banks whose 'id' matches any one of the given ids
     *     (case-insensitive). Can be at most 1000.
     * @param search If specified, return banks whose 'name' or 'identifier' contains the given
     *     search string (case-insensitive)
     * @param country If specified, return banks whose 'country' matches the given ISO 3166-1
     *     alpha-2 country code (case-insensitive)
     * @param page Result page to retrieve. Default to 1 if not specified.
     * @param perPage Maximum number of records per page. Can be at most 200. Default to 200
     *     if not specified.
     * @param sort The key to sort the results. Could be one of: name, provider and country.
     *     Defaults to name if not specified.
     * @param provider If specified, return banks whose 'provider' matches the given provider
     *     (case insensitive).
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks(
            @Nullable List<String> bankIds,
            @Nullable String search,
            @Nullable String country,
            @Nullable Integer page,
            @Nullable Integer perPage,
            @Nullable String sort,
            @Nullable String provider) {
        GetBanksRequest.Builder request = GetBanksRequest.newBuilder();

        if (bankIds != null) {
            request.addAllIds(bankIds);
        }
        if (search != null) {
            request.setSearch(search);
        }
        if (country != null) {
            request.setCountry(country);
        }
        if (page != null) {
            request.setPage(page);
        }
        if (perPage != null) {
            request.setPerPage(perPage);
        }
        if (sort != null) {
            request.setSort(sort);
        }
        if (provider != null) {
            request.setProvider(provider);
        }

        return toObservable(gateway.getBanks(request.build()))
                .map(new Function<GetBanksResponse, List<Bank>>() {
                    public List<Bank> apply(GetBanksResponse response) {
                        return response.getBanksList();
                    }
                });
    }

    /**
     * Get the default recovery agent id.
     *
     * @return the default recovery agent id.
     */
    public Observable<String> getDefaultAgent() {
        // TODO(sibin): Use GetDefaultAgentRequest instead after the call is available.
        return toObservable(gateway.resolveAlias(
                ResolveAliasRequest.newBuilder()
                        .setAlias(Alias.newBuilder()
                                .setType(Alias.Type.DOMAIN)
                                .setValue("token.io")
                                .build())
                        .build()))
                .map(new Function<ResolveAliasResponse, String>() {
                    @Override
                    public String apply(ResolveAliasResponse response) throws Exception {
                        return response.getMember().getId();
                    }
                });
    }

    private List<MemberOperation> toMemberOperations(Key... keys) {
        List<MemberOperation> operations = new LinkedList<>();
        for (Key key : keys) {
            operations.add(MemberOperation.newBuilder()
                    .setAddKey(MemberAddKeyOperation.newBuilder()
                            .setKey(key))
                    .build());
        }
        return operations;
    }
}
