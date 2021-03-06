package io.token.android;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.mock.MockContext;
import com.google.common.util.concurrent.Uninterruptibles;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.security.AKSCryptoEngineFactory;
import io.token.security.CryptoEngine;
import io.token.security.UserAuthenticationStore;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AKSCryptoEngineTest {
    private static final long ONE_DAY = 86400000L;

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("io.token.browser.test", appContext.getPackageName());
    }

    @Test
    public void createSigner_usesValidKey() {
        CryptoEngine cryptoEngine = new AKSCryptoEngineFactory(
                new MockContext(),
                new UserAuthenticationStore(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                .create(randomId());

        cryptoEngine.generateKey(LOW, System.currentTimeMillis() + 1000);
        Key validKey = cryptoEngine.generateKey(LOW, System.currentTimeMillis() + ONE_DAY);
        Uninterruptibles.sleepUninterruptibly(2000, TimeUnit.MILLISECONDS);
        assertEquals(cryptoEngine.createSigner(LOW).getKeyId(), validKey.getId());

        Key validStandardKey = cryptoEngine.generateKey(STANDARD);
        cryptoEngine.generateKey(STANDARD, System.currentTimeMillis() + 1000);
        Uninterruptibles.sleepUninterruptibly(2000, TimeUnit.MILLISECONDS);
        assertEquals(cryptoEngine.createSigner(STANDARD).getKeyId(), validStandardKey.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createVerifier_enforcesNonExpired() {
        CryptoEngine cryptoEngine = new AKSCryptoEngineFactory(
                new MockContext(),
                new UserAuthenticationStore(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                .create(randomId());

        final Key expiredKey = cryptoEngine.generateKey(
                STANDARD,
                System.currentTimeMillis() + 1000);
        Uninterruptibles.sleepUninterruptibly(2000, TimeUnit.MILLISECONDS);
        cryptoEngine.createVerifier(expiredKey.getId());
    }

    @Test
    public void deleteKeys() {
        CryptoEngine cryptoEngine = new AKSCryptoEngineFactory(
                new MockContext(),
                new UserAuthenticationStore(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                .create(randomId());

        cryptoEngine.generateKey(LOW);
        cryptoEngine.generateKey(STANDARD);
        cryptoEngine.generateKey(PRIVILEGED);
        assertEquals(cryptoEngine.getPublicKeys().size(), 3);

        cryptoEngine.generateKey(LOW);
        cryptoEngine.generateKey(STANDARD);
        cryptoEngine.generateKey(PRIVILEGED);
        assertEquals(cryptoEngine.getPublicKeys().size(), 6);

        cryptoEngine.deleteKeys();
        assertEquals(cryptoEngine.getPublicKeys().size(), 0);
    }

    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
