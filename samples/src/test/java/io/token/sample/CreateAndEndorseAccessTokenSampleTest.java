package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.common.Constants.devKey;
import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.token.TokenProtos.Token;

import org.junit.Test;

public class CreateAndEndorseAccessTokenSampleTest {
    @Test
    public void createAccessTokenTest() {
        try (TokenIO tokenIO = TokenIO.create(
                DEVELOPMENT,
                devKey)) {
            Member grantor = tokenIO.createMember(newAlias());
            Member grantee = tokenIO.createMember(newAlias());

            Token token = createAccessToken(grantor, grantee.firstAlias());
            assertThat(token).isNotNull();
        }
    }
}
