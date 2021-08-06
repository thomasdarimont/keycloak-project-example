/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.thomasdarimont.keycloak.custom.auth.mfa.otp;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.ManageTrustedDeviceAction;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;

import javax.ws.rs.core.MultivaluedMap;

public class AcmeOTPFormAuthenticator extends OTPFormAuthenticator {

    @Override
    public void validateOTP(AuthenticationFlowContext context) {
        super.validateOTP(context);

        if (FlowStatus.SUCCESS.equals(context.getStatus())) {
            MultivaluedMap<String, String> formParams = context.getHttpRequest().getDecodedFormParameters();
            if (formParams.containsKey("register-trusted-device")) {
                context.getUser().addRequiredAction(ManageTrustedDeviceAction.ID);
            }
        }
    }
}
