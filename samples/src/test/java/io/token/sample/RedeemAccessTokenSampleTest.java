package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.RedeemAccessTokenSample.redeemAccessToken;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.List;

import org.junit.Test;

public class RedeemAccessTokenSampleTest {
    @Test
    public void redeemAccessTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(DEVELOPMENT, "devKey")) {
            Member grantor = tokenIO.createMember(newAlias());
            Member grantee = tokenIO.createMember(newAlias());

            LinkMemberAndBankSample.linkBankAccounts(grantor);

            Token token = createAccessToken(grantor, grantee.firstAlias());

            List<Account> grantorAccounts = redeemAccessToken(grantee, token.getId());
            assertThat(grantorAccounts.isEmpty()).isFalse();
        }
    }
}
