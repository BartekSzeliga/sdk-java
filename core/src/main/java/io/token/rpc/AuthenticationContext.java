/**
 * Copyright (c) 2020 Token, Inc.
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

import com.google.auto.value.AutoValue;
import io.token.proto.common.security.SecurityProtos.CustomerTrackingMetadata;
import io.token.proto.common.security.SecurityProtos.Key;

import javax.annotation.Nullable;

/**
 * Authentication context. Stores the values of On-Behalf-Of and Key-Level to be used for request
 * authentication and signing.
 */
@AutoValue
public abstract class AuthenticationContext {
    static AuthenticationContext create(
            @Nullable String onBehalfOf,
            boolean customerInitiated,
            Key.Level keyLevel,
            CustomerTrackingMetadata customerTrackingMetadata) {
        return new AutoValue_AuthenticationContext(
                onBehalfOf,
                customerInitiated,
                keyLevel,
                customerTrackingMetadata);
    }

    abstract @Nullable String getOnBehalfOf();

    abstract boolean getCustomerInitiated();

    abstract Key.Level getKeyLevel();

    abstract CustomerTrackingMetadata getCustomerTrackingMetadata();
}
