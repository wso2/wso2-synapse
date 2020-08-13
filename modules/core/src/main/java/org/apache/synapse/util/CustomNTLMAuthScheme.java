/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.util;

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.commons.httpclient.auth.MalformedChallengeException;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.builtin.NTLMMediator;
import java.io.IOException;

/**
 * Custom NTLM Authentication Scheme.
 */
public abstract class CustomNTLMAuthScheme implements AuthScheme {
    /**
     * Log object for this class.
     */
    private static final Log logger = LogFactory.getLog(CustomNTLMAuthScheme.class);

    /**
     * NTLM challenge string.
     */
    private String ntlmChallenge = null;

    private static final int UNINITIATED = 0;
    private static final int INITIATED = 1;
    private static final int TYPE1_MSG_GENERATED = 2;
    private static final int TYPE2_MSG_RECEIVED = 3;
    private static final int TYPE3_MSG_GENERATED = 4;
    private static final int FAILED = Integer.MAX_VALUE;

    /**
     * Authentication process state
     */
    private int state;

    /**
     * Default constructor for the NTLM authentication scheme.
     *
     * @since 3.0
     */
    public CustomNTLMAuthScheme() {
        super();
        this.state = UNINITIATED;
    }

    /**
     * Constructor for the NTLM authentication scheme.
     *
     * @param challenge The authentication challenge
     * @throws MalformedChallengeException is thrown if the authentication challenge is malformed
     */
    public CustomNTLMAuthScheme(final String challenge)
            throws MalformedChallengeException {
        super();
        processChallenge(challenge);
    }

    /**
     * Processes the NTLM challenge.
     *
     * @param challenge the challenge string
     * @throws MalformedChallengeException is thrown if the authentication challenge is malformed
     * @since 3.0
     */
    public void processChallenge(final String challenge)
            throws MalformedChallengeException {

        if (logger.isDebugEnabled()) {
            logger.debug("[CustomNTLMAuthScheme] processChallenge Invoked.");
        }

        String s = AuthChallengeParser.extractScheme(challenge);
        if (!s.equalsIgnoreCase(getSchemeName())) {
            throw new MalformedChallengeException("[CustomNTLMAuthScheme] Invalid NTLM challenge: "
                                                  + challenge);
        }
        int i = challenge.indexOf(' ');
        if (i != -1) {
            s = challenge.substring(i, challenge.length());
            this.ntlmChallenge = s.trim();
            this.state = TYPE2_MSG_RECEIVED;
        } else {
            this.ntlmChallenge = "";
            if (this.state == UNINITIATED) {
                this.state = INITIATED;
            } else {
                this.state = FAILED;
            }
        }
    }

    /**
     * Tests if the NTLM authentication process has been completed.
     *
     * @return true if Basic authorization has been processed,
     * false otherwise.
     * @since 3.0
     */
    public boolean isComplete() {
        return this.state == TYPE3_MSG_GENERATED || this.state == FAILED;
    }

    /**
     * Returns textual designation of the NTLM authentication scheme.
     *
     * @return ntlm
     */
    public String getSchemeName() {
        return "ntlm";
    }

    /**
     * The concept of an authentication realm is not supported by the NTLM
     * authentication scheme. Always returns null.
     *
     * @return null
     */
    public String getRealm() {
        return null;
    }

    /**
     * Unsupported.
     */
    public String getID() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the authentication parameter with the given name, if available.
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * There are no valid parameters for NTLM authentication so this method
     * always returns null.
     *
     * @param name The name of the parameter to be returned
     * @return the parameter with the given name
     */
    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        return null;
    }

    /**
     * Returns true. NTLM authentication scheme is connection based.
     *
     * @return true.
     * @since 3.0
     */
    public boolean isConnectionBased() {
        return true;
    }

    /**
     * Unsupported.
     */
    public static String authenticate(
            final NTCredentials credentials, final String challenge)
            throws AuthenticationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported.
     */
    public static String authenticate(
            final NTCredentials credentials, final String challenge,
            String charset) throws AuthenticationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported.
     */
    public abstract String getNTLMVersion();

    /**
     * Unsupported.
     */
    public String authenticate(
            Credentials credentials, String method, String uri)
            throws AuthenticationException {
        throw new UnsupportedOperationException();

    }

    /**
     * Produces NTLM authorization string for the given set of
     * {@link Credentials}.
     *
     * @param credentials The set of credentials to be used for athentication
     * @param method      The method being authenticated
     * @return an NTLM authorization string
     * @throws InvalidCredentialsException if authentication credentials are not valid or not applicable
     *                                     for this authentication scheme
     * @throws AuthenticationException     if authorization string cannot be generated due to an
     *                                     authentication failure
     * @since 3.0
     */
    public String authenticate(Credentials credentials, HttpMethod method)
            throws AuthenticationException {

        if (logger.isDebugEnabled()) {
            logger.debug("[CustomNTLMAuthScheme] NTLM Scheme Authentication Method Invoked.");
        }

        if (this.state == UNINITIATED) {
            throw new IllegalStateException(
                    "[CustomNTLMAuthScheme] NTLM authentication process has not been initiated");
        }

        //Get the NTLM version from the NTLMMediator and identify the flags to be used for authentication.
        String ntlmVersion = getNTLMVersion();
        if (logger.isDebugEnabled()) {
            logger.debug("[CustomNTLMAuthScheme] The NTLM version going to use is: " + ntlmVersion);
        }
        int flags = 0;
        if (ntlmVersion.toUpperCase().equals("V1")) {
            flags = NtlmFlags.NTLMSSP_NEGOTIATE_NTLM;
        } else if (ntlmVersion.toUpperCase().equals("V2")) {
            flags = NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("[CustomNTLMAuthScheme] NTLM Version not specified.");
            }
        }

        NTCredentials ntcredentials = null;
        try {
            ntcredentials = (NTCredentials) credentials;
        } catch (ClassCastException e) {
            throw new InvalidCredentialsException(
                    "[CustomNTLMAuthScheme] Credentials cannot be used for NTLM authentication: "
                    + credentials.getClass().getName());
        }
        byte[] msgBytes = null;
        String response = null;
        if (this.state == INITIATED) {
            Type1Message type1Message = new Type1Message(flags, ntcredentials.getDomain(), ntcredentials.getHost());
            msgBytes = type1Message.toByteArray();
            this.state = TYPE1_MSG_GENERATED;

            if (logger.isDebugEnabled()) {
                logger.debug("[CustomNTLMAuthScheme] Type1Message Generated.");
            }

        } else if (this.state == TYPE2_MSG_RECEIVED) {

            if (logger.isDebugEnabled()) {
                logger.debug("[CustomNTLMAuthScheme] Type2Message Received.");
            }

            Type2Message type2Message;
            try {
                type2Message = new jcifs.ntlmssp.Type2Message(jcifs.util.Base64.decode(this.ntlmChallenge));
            } catch (IOException e) {
                throw new RuntimeException("[CustomNTLMAuthScheme] Invalid Type2 message", e);
            }

            Type3Message type3Message = new Type3Message(type2Message, ntcredentials.getPassword(), ntcredentials.getDomain(), ntcredentials.getUserName(), ntcredentials.getHost(), flags);
            msgBytes = type3Message.toByteArray();
            this.state = TYPE3_MSG_GENERATED;

            if (logger.isDebugEnabled()) {
                logger.debug("[CustomNTLMAuthScheme] Type3Message Generated.");
            }

        } else {
            throw new RuntimeException("[CustomNTLMAuthScheme] Failed to Authenticate");
        }
        response = EncodingUtil.getAsciiString(Base64.encodeBase64(msgBytes));
        return "NTLM " + response;
    }
}
