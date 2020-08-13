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

package org.apache.synapse.mediators.builtin;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.CustomNTLMV1AuthScheme;
import org.apache.synapse.util.CustomNTLMV2AuthScheme;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NTLM Mediator mainly creates an authenticator with the credentials which user
 * provides as parameters. Required authentication params are username, password,
 * host, domain and NTLM version. Created authenticator authenticator is set to
 * the MessageContext as _NTLM_DIGEST_BASIC_AUTHENTICATION_ property
 * (HTTPConstants.AUTHENTICATE).
 * <p/>
 * Further this Reads the MultiThreadedHttpConnectionManager from the cache and set
 * it to the Axis2MessageContext as MULTITHREAD_HTTP_CONNECTION_MANAGER property.
 * <p/>
 * After NTLM mediator authenticates properly user can use callout mediator or call
 * mediator with blocking=true to send the payload to backend. In order to persist
 * the authenticator through out the call and callout mediators please use
 * initAxis2ClientOptions="false" parameter in call and callout mediators.
 */
public class NTLMMediator extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(NTLMMediator.class);
    private String username = null;
    private String password = null;
    private String host = null;
    private String domain = null;
    private String ntlmVersion = null;

    private ConfigurationContext configCtx = null;

    private int maxConnectionManagerCacheSize = 32;

    /** regex for secure vault expression */
    private static final String SECURE_VAULT_REGEX = "\\{(wso2:vault-lookup\\('(.*?)'\\))\\}";

    private static final String NTLM_V1 = "v1";
    private static final String NTLM_V2 = "v2";

    private Pattern vaultLookupPattern = Pattern.compile(SECURE_VAULT_REGEX);

    public boolean mediate(MessageContext messageContext) {

        if (log.isDebugEnabled()) {
            log.debug("[NTLMMediator] mediate method Invoked.");
        }

        // Creating a HTTP authenticator to cater the NTLM Authentication scheme
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        List<String> authScheme = new ArrayList<String>();
        authScheme.add(HttpTransportProperties.Authenticator.NTLM);
        authenticator.setAuthSchemes(authScheme);

        if (username != null) {
            authenticator.setUsername(username);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[NTLMMediator] Username not specified.");
            }
        }

        if (password != null) {
            authenticator.setPassword(resolveSecureVaultExpressions(password, messageContext));
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[NTLMMediator] Password not specified.");
            }
        }

        if (host != null) {
            authenticator.setHost(host);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[NTLMMediator] Host not specified.");
            }
        }

        if (domain != null) {
            authenticator.setDomain(domain);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[NTLMMediator] Domain not specified.");
            }
        }

        if (ntlmVersion != null) {
            if (log.isDebugEnabled()) {
                log.debug("[NTLMMediator] NTLM version is: " + ntlmVersion);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[NTLMMediator] NTLM version is not specified.");
            }
        }

        // Set the newly created NTLM authenticator to the Axis2MessageContext
        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        axis2MessageContext.getOptions().setProperty(HTTPConstants.AUTHENTICATE, authenticator);

        // Read the MultiThreadedHttpConnectionManager from the cache and set it to the Axis2MessageContext
        MultiThreadedHttpConnectionManager connectionManager;
        String cacheKey = new StringBuilder().append(authenticator.getUsername()).append("@")
                                             .append(authenticator.getDomain()).append(":")
                                             .append(authenticator.getPassword()).toString();
        if (connectionManagerCache.containsKey(cacheKey)) {
            connectionManager = connectionManagerCache.get(cacheKey);
        } else {
            connectionManager = connectionManagerCache.put(cacheKey,
                                                           new MultiThreadedHttpConnectionManager());
        }
        axis2MessageContext.getOptions().setProperty(HTTPConstants.MULTITHREAD_HTTP_CONNECTION_MANAGER,
                                                     connectionManager);
        axis2MessageContext.getEnvelope().buildWithAttachments();
        return true;
    }

    private Map<String, MultiThreadedHttpConnectionManager> connectionManagerCache = Collections.synchronizedMap
            (
                    new LinkedHashMap<String, MultiThreadedHttpConnectionManager>(16, .75F, true) {
                        @Override
                        public boolean removeEldestEntry(Map.Entry eldest) {
                            //when to remove the eldest entry
                            return size() > maxConnectionManagerCacheSize;   //size exceeded the max allowed
                        }

                        @Override
                        public MultiThreadedHttpConnectionManager put(String key,
                                                                      MultiThreadedHttpConnectionManager value) {
                            if (!containsKey(key)) {
                                synchronized (this) {
                                    if (!containsKey(key)) {
                                        return super.put(key, value);
                                    }
                                }
                            }
                            return get(key);
                        }
                    }
            );


    /**
     * When init have to set the NTLM Custom Authenticator as auth scheme and set
     * jcifs encoding to ASCII.
     */
    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        if (log.isDebugEnabled()) {
            log.debug("[NTLMMediator] Init method Invoked.");
        }
        log.info("[NTLMMediator] Init method Invoked.");
        //Register the custom NTLM authenticator as an Auth Scheme in HttpClient and set the encoding
        //property of the JCICF lib to ASCII.
        jcifs.Config.setProperty("jcifs.encoding", "ASCII");
        //Differentiate Auth scheme based on the NTLM version
        if (NTLM_V2.equalsIgnoreCase(ntlmVersion)) {
            AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, CustomNTLMV2AuthScheme.class);
        } else {
            AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, CustomNTLMV1AuthScheme.class);
        }

    }

    /**
     * Use secure vault to secure password in NTLM Mediator.
     *
     * @param value Value of password from NTLM Mediator
     * @return the actual password from the Secure Vault Password Management.
     */
    private String resolveSecureVaultExpressions(String value, MessageContext synCtx) {
        //Password can be null, it is optional
        if (value == null) {
            return null;
        }
        Matcher lookupMatcher = vaultLookupPattern.matcher(value);
        String resolvedValue = value;
        if (lookupMatcher.find()) {
            Value expression = null;
            //getting the expression with out curly brackets
            String expressionStr = lookupMatcher.group(1);
            try {
                expression = new Value(new SynapseXPath(expressionStr));
            } catch (JaxenException e) {
                throw new SynapseException("Error while building the expression : " + expressionStr, e);
            }
            resolvedValue = expression.evaluateValue(synCtx);
            if (StringUtils.isEmpty(resolvedValue)) {
                log.warn("Found Empty value for expression : " + expression.getExpression());
                resolvedValue = "";
            }
        }
        return resolvedValue;
    }

    @Override
    public void destroy() {
        if (configCtx != null) {
            try {
                configCtx.terminate();
            } catch (AxisFault ignore) {
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getNtlmVersion() {
        return ntlmVersion;
    }

    public void setNtlmVersion(String ntlmVersion) {
        this.ntlmVersion = ntlmVersion;
    }
}
