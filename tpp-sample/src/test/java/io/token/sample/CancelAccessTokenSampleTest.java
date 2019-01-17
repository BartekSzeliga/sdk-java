package io.token.sample;

import static io.token.sample.CancelAccessTokenSample.cancelAccessToken;
import static io.token.sample.TestUtil.createAccessToken;
import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createUserMember;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class CancelAccessTokenSampleTest {
    @Test
    public void cancelAccessTokenByGranteeTest() {
        try (TokenClient tokenClient = createClient()) {
            io.token.user.Member grantor = createUserMember();
            String accountId = grantor.getAccountsBlocking().get(0).id();
            Alias granteeAlias = randomAlias();
            Member grantee = tokenClient.createMemberBlocking(granteeAlias);

            Token token = createAccessToken(grantor, accountId, granteeAlias);
            TokenOperationResult result = cancelAccessToken(grantee, token.getId());
            assertThat(result.getStatus()).isEqualTo(TokenOperationResult.Status.SUCCESS);
        }
    }
}
