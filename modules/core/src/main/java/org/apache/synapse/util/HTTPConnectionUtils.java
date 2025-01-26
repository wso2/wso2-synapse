/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.llom.factory.OMLinkedListImplFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.SynapseConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This utility class is responsible for parsing an OMElement to extract
 * various HTTP connection parameters and generating an OMElement
 * representation of an HTTP Endpoint.
 */
public class HTTPConnectionUtils {

    /**
     * Generates an OMElement representation of the HTTP endpoint.
     *
     * @return the OMElement representing the HTTP endpoint
     */
    public static OMElement generateHTTPEndpointOMElement(OMElement connectionOMElement) {

        Map<String, String> connectionElements = generateConnectionElementsMap(connectionOMElement);

        OMFactory factory = new OMLinkedListImplFactory();
        OMNamespace synapseNS = factory.createOMNamespace("http://ws.apache.org/ns/synapse", "");

        OMElement endpoint = factory.createOMElement(SynapseConstants.ENDPOINT, synapseNS);
        endpoint.addAttribute(SynapseConstants.NAME,
                connectionElements.get(SynapseConstants.NAME) + SynapseConstants.ENDPOINT_IDENTIFIER, null);

        OMElement http = factory.createOMElement(SynapseConstants.HTTP, synapseNS);
        if (SynapseConstants.ENABLE.equalsIgnoreCase(connectionElements.get(SynapseConstants.TRACE))) {
            http.addAttribute(SynapseConstants.TRACE, SynapseConstants.ENABLE, null);
        }
        if (SynapseConstants.ENABLE.equalsIgnoreCase(connectionElements.get(SynapseConstants.STATISTICS))) {
            http.addAttribute(SynapseConstants.STATISTICS, SynapseConstants.ENABLE, null);
        }
        http.addAttribute(SynapseConstants.URI_TEMPLATE, "{uri.var.base}{+uri.var.path}{+uri.var.query}", null);

        populateEnableAddressingElement(factory, synapseNS, http, connectionElements);
        populateEnableSecurityElement(factory, synapseNS, http, connectionElements);
        populateTimeoutElement(factory, synapseNS, http, connectionElements);
        populateSuspendOnFailureElement(factory, synapseNS, http, connectionElements);
        populateMarkForSuspensionElement(factory, synapseNS, http, connectionElements);
        populateAuthenticationElement(factory, synapseNS, http, connectionElements);
        endpoint.addChild(http);

        if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.MISCELLANEOUS_PROPERTIES))) {
            List<OMElement> miscellaneousProperties = generateMiscellaneousPropertiesElement(
                    factory, synapseNS, connectionElements.get(SynapseConstants.MISCELLANEOUS_PROPERTIES));
            for (OMElement property : miscellaneousProperties) {
                endpoint.addChild(property);
            }
        }
        if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.MISCELLANEOUS_DESCRIPTION))) {
            OMElement description = factory.createOMElement(SynapseConstants.DESCRIPTION, synapseNS);
            description.setText(connectionElements.get(SynapseConstants.MISCELLANEOUS_DESCRIPTION));
            endpoint.addChild(description);
        }
        return endpoint;
    }

    /**
     * Populates the enable security element for the HTTP endpoint configuration.
     *
     * @param factory            the OMFactory used to create OMElements
     * @param synapseNS          the OMNamespace for the Synapse configuration
     * @param http               the OMElement representing the HTTP endpoint
     * @param connectionElements the map containing the HTTP connection parameters
     */
    private static void populateEnableSecurityElement(OMFactory factory, OMNamespace synapseNS,
                                                      OMElement http, Map<String, String> connectionElements) {

        if (SynapseConstants.ENABLE.equalsIgnoreCase(
                connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OPTION))) {
            OMElement enableSecurity = factory.createOMElement(SynapseConstants.ENABLE_SECURITY, synapseNS);
            if (SynapseConstants.ENABLE.equalsIgnoreCase(
                    connectionElements.get(
                            SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_OUTBOUND_POLICY_OPTION))) {
                if (StringUtils.isNotEmpty(
                        connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_POLICY_KEY))) {
                    enableSecurity.addAttribute(SynapseConstants.INBOUND_POLICY,
                            connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_POLICY_KEY),
                            null);
                }
                if (StringUtils.isNotEmpty(
                        connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OUTBOUND_POLICY_KEY))) {
                    enableSecurity.addAttribute(SynapseConstants.OUTBOUND_POLICY,
                            connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OUTBOUND_POLICY_KEY),
                            null);
                }
            } else {
                if (StringUtils.isNotEmpty(
                        connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_POLICY_KEY))) {
                    enableSecurity.addAttribute(SynapseConstants.POLICY,
                            connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_POLICY_KEY), null);
                }
            }
            http.addChild(enableSecurity);
        }
    }

    /**
     * Populates the enable addressing element for the HTTP endpoint configuration.
     *
     * @param factory            the OMFactory used to create OMElements
     * @param synapseNS          the OMNamespace for the Synapse configuration
     * @param http               the OMElement representing the HTTP endpoint
     * @param connectionElements the map containing the HTTP connection parameters
     */
    private static void populateEnableAddressingElement(OMFactory factory, OMNamespace synapseNS, OMElement http,
                                                        Map<String, String> connectionElements) {

        if (SynapseConstants.ENABLE.equalsIgnoreCase(
                connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_OPTION))) {
            OMElement enableAddressing = factory.createOMElement(SynapseConstants.ENABLE_ADDRESSING, synapseNS);
            if (SynapseConstants.ENABLE.equalsIgnoreCase(connectionElements.get(
                    SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_SEPARATE_LISTENER))) {
                enableAddressing.addAttribute(SynapseConstants.SEPARATE_LISTENER, "true", null);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(
                    SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_VERSION))) {
                enableAddressing.addAttribute(SynapseConstants.VERSION,
                        connectionElements.get(SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_VERSION), null);
            }
            http.addChild(enableAddressing);
        }
    }

    /**
     * Populates the authentication element for the HTTP endpoint configuration.
     * This method adds the appropriate authentication details (Basic or OAuth)
     *
     * @param factory            the OMFactory used to create OMElements
     * @param synapseNS          the OMNamespace for the Synapse configuration
     * @param http               the OMElement representing the HTTP endpoint
     * @param connectionElements the map containing the HTTP connection parameters
     */
    private static void populateAuthenticationElement(OMFactory factory, OMNamespace synapseNS,
                                                      OMElement http, Map<String, String> connectionElements) {

        if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.AUTH_TYPE))) {
            if (SynapseConstants.BASIC_AUTH.equalsIgnoreCase(connectionElements.get(SynapseConstants.AUTH_TYPE))) {
                OMElement authentication = factory.createOMElement(SynapseConstants.AUTHENTICATION, synapseNS);
                OMElement basicAuth = generateBasicAuthenticationElement(factory, synapseNS,
                        connectionElements.get(SynapseConstants.USERNAME),
                        connectionElements.get(SynapseConstants.PASSWORD));
                authentication.addChild(basicAuth);
                http.addChild(authentication);
            } else if (SynapseConstants.OAUTH.equalsIgnoreCase(connectionElements.get(SynapseConstants.AUTH_TYPE))) {
                OMElement authentication = factory.createOMElement(SynapseConstants.AUTHENTICATION, synapseNS);
                OMElement oauth = factory.createOMElement(SynapseConstants.OAUTH_TAG, synapseNS);
                if (SynapseConstants.OAUTH_GRANT_TYPE_AUTHORIZATION_CODE.equalsIgnoreCase(
                        connectionElements.get(SynapseConstants.OAUTH_GRANT_TYPE))) {
                    OMElement authorizationCode = generateOAuthAuthorizationCodeElement(factory, synapseNS,
                            connectionElements.get(SynapseConstants.REFRESH_TOKEN),
                            connectionElements.get(SynapseConstants.CLIENT_ID),
                            connectionElements.get(SynapseConstants.CLIENT_SECRET),
                            connectionElements.get(SynapseConstants.TOKEN_URL),
                            connectionElements.get(SynapseConstants.OAUTH_AUTHORIZATION_MODE),
                            connectionElements.get(SynapseConstants.REQUEST_PARAMETERS));
                    oauth.addChild(authorizationCode);
                    authentication.addChild(oauth);
                    http.addChild(authentication);
                } else if (SynapseConstants.OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS.equalsIgnoreCase(
                        connectionElements.get(SynapseConstants.OAUTH_GRANT_TYPE))) {
                    OMElement clientCredentials = generateOAuthClientCredentialsElement(factory, synapseNS,
                            connectionElements.get(SynapseConstants.CLIENT_ID),
                            connectionElements.get(SynapseConstants.CLIENT_SECRET),
                            connectionElements.get(SynapseConstants.TOKEN_URL),
                            connectionElements.get(SynapseConstants.OAUTH_AUTHORIZATION_MODE),
                            connectionElements.get(SynapseConstants.REQUEST_PARAMETERS));
                    oauth.addChild(clientCredentials);
                    authentication.addChild(oauth);
                    http.addChild(authentication);
                } else if (SynapseConstants.OAUTH_GRANT_TYPE_PASSWORD.equalsIgnoreCase(
                        connectionElements.get(SynapseConstants.OAUTH_GRANT_TYPE))) {
                    OMElement passwordCredentials = generateOAuthPasswordCredentialsElement(factory, synapseNS,
                            connectionElements.get(SynapseConstants.USERNAME),
                            connectionElements.get(SynapseConstants.PASSWORD),
                            connectionElements.get(SynapseConstants.CLIENT_ID),
                            connectionElements.get(SynapseConstants.CLIENT_SECRET),
                            connectionElements.get(SynapseConstants.TOKEN_URL),
                            connectionElements.get(SynapseConstants.OAUTH_AUTHORIZATION_MODE),
                            connectionElements.get(SynapseConstants.REQUEST_PARAMETERS));
                    oauth.addChild(passwordCredentials);
                    authentication.addChild(oauth);
                    http.addChild(authentication);
                }
            }
        }
    }

    /**
     * Populates the mark for suspension element for the HTTP endpoint configuration.
     *
     * @param factory            the OMFactory used to create OMElements
     * @param synapseNS          the OMNamespace for the Synapse configuration
     * @param http               the OMElement representing the HTTP endpoint
     * @param connectionElements the map containing the HTTP connection parameters
     */
    private static void populateMarkForSuspensionElement(OMFactory factory, OMNamespace synapseNS, OMElement http,
                                                         Map<String, String> connectionElements) {

        if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.RETRY_ERROR_CODES)) ||
                StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.RETRY_COUNT)) ||
                StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.RETRY_DELAY))) {

            OMElement markForSuspension = factory.createOMElement(SynapseConstants.MARK_FOR_SUSPENSION, synapseNS);
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.RETRY_ERROR_CODES))) {
                OMElement retryErrorCodes = factory.createOMElement(SynapseConstants.ERROR_CODES, synapseNS);
                retryErrorCodes.setText(connectionElements.get(SynapseConstants.RETRY_ERROR_CODES));
                markForSuspension.addChild(retryErrorCodes);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.RETRY_COUNT))) {
                OMElement retriesBeforeSuspension =
                        factory.createOMElement(SynapseConstants.RETRIES_BEFORE_SUSPENSION, synapseNS);
                retriesBeforeSuspension.setText(String.valueOf(connectionElements.get(SynapseConstants.RETRY_COUNT)));
                markForSuspension.addChild(retriesBeforeSuspension);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.RETRY_DELAY))) {
                OMElement retryDelay = factory.createOMElement(SynapseConstants.RETRY_DELAY, synapseNS);
                retryDelay.setText(String.valueOf(connectionElements.get(SynapseConstants.RETRY_DELAY)));
                markForSuspension.addChild(retryDelay);
            }
            http.addChild(markForSuspension);
        }
    }

    /**
     * Populates the suspend on failure element for the HTTP endpoint configuration.
     *
     * @param factory             the OMFactory used to create OMElements
     * @param synapseNS           the OMNamespace for the Synapse configuration
     * @param httpEndpointElement the OMElement representing the HTTP endpoint
     * @param connectionElements  the map containing the HTTP connection parameters
     */
    private static void populateSuspendOnFailureElement(OMFactory factory, OMNamespace synapseNS,
                                                        OMElement httpEndpointElement,
                                                        Map<String, String> connectionElements) {

        if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_ERROR_CODES)) ||
                StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_INITIAL_DURATION)) ||
                StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_PROGRESSION_FACTOR)) ||
                StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_MAXIMUM_DURATION))) {

            OMElement suspendOnFailure = factory.createOMElement(SynapseConstants.SUSPEND_ON_FAILURE, synapseNS);
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_ERROR_CODES))) {
                OMElement errorCodes = factory.createOMElement(SynapseConstants.ERROR_CODES, synapseNS);
                errorCodes.setText(connectionElements.get(SynapseConstants.SUSPEND_ERROR_CODES));
                suspendOnFailure.addChild(errorCodes);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_INITIAL_DURATION))) {
                OMElement initialDuration = factory.createOMElement(SynapseConstants.INITIAL_DURATION, synapseNS);
                initialDuration.setText(
                        String.valueOf(connectionElements.get(SynapseConstants.SUSPEND_INITIAL_DURATION)));
                suspendOnFailure.addChild(initialDuration);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_PROGRESSION_FACTOR))) {
                OMElement progressionFactor = factory.createOMElement(SynapseConstants.PROGRESSION_FACTOR, synapseNS);
                progressionFactor.setText(
                        String.valueOf(connectionElements.get(SynapseConstants.SUSPEND_PROGRESSION_FACTOR)));
                suspendOnFailure.addChild(progressionFactor);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.SUSPEND_MAXIMUM_DURATION))) {
                OMElement maximumDuration = factory.createOMElement(SynapseConstants.MAXIMUM_DURATION, synapseNS);
                maximumDuration.setText(
                        String.valueOf(connectionElements.get(SynapseConstants.SUSPEND_MAXIMUM_DURATION)));
                suspendOnFailure.addChild(maximumDuration);
            }
            httpEndpointElement.addChild(suspendOnFailure);
        }
    }

    /**
     * Populates the timeout element for the HTTP endpoint configuration.
     *
     * @param factory             the OMFactory used to create OMElements
     * @param synapseNS           the OMNamespace for the Synapse configuration
     * @param httpEndpointElement the OMElement representing the HTTP endpoint
     * @param connectionElements  the map containing the HTTP connection parameters
     */
    private static void populateTimeoutElement(OMFactory factory, OMNamespace synapseNS,
                                               OMElement httpEndpointElement, Map<String, String> connectionElements) {

        if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.TIMEOUT_DURATION)) ||
                StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.TIMEOUT_ACTION))) {

            OMElement timeout = factory.createOMElement(SynapseConstants.TIMEOUT, synapseNS);
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.TIMEOUT_DURATION))) {
                OMElement duration = factory.createOMElement(SynapseConstants.DURATION, synapseNS);
                duration.setText(String.valueOf(connectionElements.get(SynapseConstants.TIMEOUT_DURATION)));
                timeout.addChild(duration);
            }
            if (StringUtils.isNotEmpty(connectionElements.get(SynapseConstants.TIMEOUT_ACTION)) &&
                    !connectionElements.get(SynapseConstants.TIMEOUT_ACTION).equalsIgnoreCase(SynapseConstants.NEVER)) {
                OMElement responseAction = factory.createOMElement(SynapseConstants.RESPONSE_ACTION, synapseNS);
                responseAction.setText(connectionElements.get(SynapseConstants.TIMEOUT_ACTION));
                timeout.addChild(responseAction);
            }
            httpEndpointElement.addChild(timeout);
        }
    }

    /**
     * Generates a list of OMElements representing miscellaneous properties.
     * Each property is parsed from a comma-separated string of key:scope:value pairs.
     *
     * @param factory                 the OMFactory used to create OMElements
     * @param synapseNS               the OMNamespace for the Synapse configuration
     * @param miscellaneousProperties the comma-separated string of key:scope:value pairs
     * @return a list of OMElements representing the miscellaneous properties
     */
    private static List<OMElement> generateMiscellaneousPropertiesElement(OMFactory factory, OMNamespace synapseNS,
                                                                          String miscellaneousProperties) {

        List<OMElement> miscellaneousPropertiesList = new ArrayList<>();
        for (String miscProperty : miscellaneousProperties.split(",")) {
            String[] keyScopeValue = miscProperty.split(":");
            if (keyScopeValue.length == 3) {
                OMElement property = factory.createOMElement("property", synapseNS);
                property.addAttribute("name", keyScopeValue[0].trim(), null);
                property.addAttribute("scope", keyScopeValue[1].trim(), null);
                property.addAttribute("value", keyScopeValue[2].trim(), null);
                miscellaneousPropertiesList.add(property);
            }
        }
        return miscellaneousPropertiesList;
    }

    /**
     * Generates an OMElement representing the OAuth password credentials grant type.
     * This includes the username, password, client ID, client secret, token URL,
     * authorization mode, and any additional properties.
     *
     * @param factory                   the OMFactory used to create OMElements
     * @param synapseNS                 the OMNamespace for the Synapse configuration
     * @param username                  the username
     * @param password                  the password
     * @param clientId                  the client ID
     * @param clientSecret              the client secret
     * @param tokenUrl                  the token URL
     * @param authMode                  the authorization mode
     * @param oauthAdditionalProperties the additional properties for OAuth authentication
     * @return the OMElement representing the OAuth password credentials grant type
     */
    private static OMElement generateOAuthPasswordCredentialsElement(OMFactory factory, OMNamespace synapseNS,
                                                                     String username, String password, String clientId,
                                                                     String clientSecret, String tokenUrl,
                                                                     String authMode,
                                                                     String oauthAdditionalProperties) {

        OMElement passwordCredentials = factory.createOMElement(SynapseConstants.PASSWORD_CREDENTIALS, synapseNS);

        OMElement usernameElement = generateAuthUsernameElement(factory, synapseNS, username);
        OMElement passwordElement = generateAuthPasswordElement(factory, synapseNS, password);
        OMElement clientIdElement = generateOAuthClientIDElement(factory, synapseNS, clientId);
        OMElement clientSecretElement = generateOAuthClientSecretElement(factory, synapseNS, clientSecret);
        OMElement tokenUrlElement = generateOAuthTokenUrlElement(factory, synapseNS, tokenUrl);
        OMElement authModeElement = generateOAuthAuthorizationModeElement(factory, synapseNS, authMode);

        passwordCredentials.addChild(usernameElement);
        passwordCredentials.addChild(passwordElement);
        passwordCredentials.addChild(clientIdElement);
        passwordCredentials.addChild(clientSecretElement);
        passwordCredentials.addChild(tokenUrlElement);
        passwordCredentials.addChild(authModeElement);

        if (StringUtils.isNotEmpty(oauthAdditionalProperties)) {
            OMElement additionalProperties =
                    generateOAuthAdditionalPropertiesElement(factory, synapseNS, oauthAdditionalProperties);
            passwordCredentials.addChild(additionalProperties);
        }
        return passwordCredentials;
    }

    /**
     * Generates an OMElement representing the OAuth client credentials grant type.
     * This includes the client ID, client secret, token URL, authorization mode,
     * and any additional properties.
     *
     * @param factory                   the OMFactory used to create OMElements
     * @param synapseNS                 the OMNamespace for the Synapse configuration
     * @param clientId                  the client ID
     * @param clientSecret              the client secret
     * @param tokenUrl                  the token URL
     * @param authMode                  the authorization mode
     * @param oauthAdditionalProperties the additional properties for OAuth authentication
     * @return the OMElement representing the OAuth client credentials grant type
     */
    private static OMElement generateOAuthClientCredentialsElement(OMFactory factory, OMNamespace synapseNS,
                                                                   String clientId, String clientSecret,
                                                                   String tokenUrl, String authMode,
                                                                   String oauthAdditionalProperties) {

        OMElement clientCredentials = factory.createOMElement(SynapseConstants.CLIENT_CREDENTIALS, synapseNS);

        OMElement oauthClientId = generateOAuthClientIDElement(factory, synapseNS, clientId);
        OMElement oauthClientSecret = generateOAuthClientSecretElement(factory, synapseNS, clientSecret);
        OMElement oauthTokenUrl = generateOAuthTokenUrlElement(factory, synapseNS, tokenUrl);
        OMElement authorizationMode = generateOAuthAuthorizationModeElement(factory, synapseNS, authMode);

        clientCredentials.addChild(oauthClientId);
        clientCredentials.addChild(oauthClientSecret);
        clientCredentials.addChild(oauthTokenUrl);
        clientCredentials.addChild(authorizationMode);

        if (StringUtils.isNotEmpty(oauthAdditionalProperties)) {
            OMElement additionalProperties =
                    generateOAuthAdditionalPropertiesElement(factory, synapseNS, oauthAdditionalProperties);
            clientCredentials.addChild(additionalProperties);
        }
        return clientCredentials;
    }

    /**
     * Generates an OMElement representing the OAuth authorization code grant type.
     * This includes the refresh token, client ID, client secret, token URL,
     * authorization mode, and any additional properties.
     *
     * @param factory                   the OMFactory used to create OMElements
     * @param synapseNS                 the OMNamespace for the Synapse configuration
     * @param refreshToken              the refresh token
     * @param clientId                  the client ID
     * @param clientSecret              the client secret
     * @param tokenUrl                  the token URL
     * @param authMode                  the authorization mode
     * @param oauthAdditionalProperties the additional properties for OAuth authentication
     * @return the OMElement representing the OAuth authorization code grant type
     */
    private static OMElement generateOAuthAuthorizationCodeElement(OMFactory factory, OMNamespace synapseNS,
                                                                   String refreshToken, String clientId,
                                                                   String clientSecret, String tokenUrl,
                                                                   String authMode, String oauthAdditionalProperties) {

        OMElement authorizationCode = factory.createOMElement(SynapseConstants.AUTHORIZATION_CODE, synapseNS);

        OMElement oauthRefreshToken = generateOAuthRefreshTokenElement(factory, synapseNS, refreshToken);
        OMElement oauthClientId = generateOAuthClientIDElement(factory, synapseNS, clientId);
        OMElement oauthClientSecret = generateOAuthClientSecretElement(factory, synapseNS, clientSecret);
        OMElement oauthTokenUrl = generateOAuthTokenUrlElement(factory, synapseNS, tokenUrl);
        OMElement authorizationMode = generateOAuthAuthorizationModeElement(factory, synapseNS, authMode);

        authorizationCode.addChild(oauthRefreshToken);
        authorizationCode.addChild(oauthClientId);
        authorizationCode.addChild(oauthClientSecret);
        authorizationCode.addChild(oauthTokenUrl);
        authorizationCode.addChild(authorizationMode);

        if (StringUtils.isNotEmpty(oauthAdditionalProperties)) {
            OMElement additionalProperties =
                    generateOAuthAdditionalPropertiesElement(factory, synapseNS, oauthAdditionalProperties);
            authorizationCode.addChild(additionalProperties);
        }
        return authorizationCode;
    }

    /**
     * Generates an OMElement representing additional OAuth properties.
     * This element contains key-value pairs of additional properties used for OAuth authentication.
     *
     * @param factory   the OMFactory used to create OMElements
     * @param synapseNS the OMNamespace for the Synapse configuration
     * @return the OMElement representing the additional OAuth properties
     */
    private static OMElement generateOAuthAdditionalPropertiesElement(OMFactory factory, OMNamespace synapseNS,
                                                                      String oauthAdditionalProperties) {

        OMElement additionalProperties = factory.createOMElement(SynapseConstants.REQUEST_PARAMETERS, synapseNS);
        for (String additionalProperty : oauthAdditionalProperties.split(",")) {
            String[] keyValue = additionalProperty.split(":");
            if (keyValue.length == 2) {
                OMElement parameter = factory.createOMElement("parameter", synapseNS);
                parameter.addAttribute("name", keyValue[0].trim(), null);
                parameter.setText(keyValue[1].trim());
                additionalProperties.addChild(parameter);
            }
        }
        return additionalProperties;
    }

    /**
     * Generates an OMElement representing the OAuth authorization mode.
     *
     * @param factory                the OMFactory used to create OMElements
     * @param synapseNS              the OMNamespace for the Synapse configuration
     * @param oauthAuthorizationMode the OAuth authorization mode
     * @return the OMElement representing the OAuth authorization mode
     */
    private static OMElement generateOAuthAuthorizationModeElement(OMFactory factory, OMNamespace synapseNS,
                                                                   String oauthAuthorizationMode) {

        OMElement authMode = factory.createOMElement(SynapseConstants.AUTH_MODE, synapseNS);
        authMode.setText(oauthAuthorizationMode);
        return authMode;
    }

    /**
     * Generates an OMElement representing the OAuth token URL.
     *
     * @param factory   the OMFactory used to create OMElements
     * @param synapseNS the OMNamespace for the Synapse configuration
     * @param tokenUrl  the token URL
     * @return the OMElement representing the OAuth token URL
     */
    private static OMElement generateOAuthTokenUrlElement(OMFactory factory, OMNamespace synapseNS, String tokenUrl) {

        OMElement oauthTokenUrl = factory.createOMElement(SynapseConstants.TOKEN_URL, synapseNS);
        oauthTokenUrl.setText(tokenUrl);
        return oauthTokenUrl;
    }

    /**
     * Generates an OMElement representing the OAuth client secret.
     *
     * @param factory      the OMFactory used to create OMElements
     * @param synapseNS    the OMNamespace for the Synapse configuration
     * @param clientSecret the client secret
     * @return the OMElement representing the OAuth client secret
     */
    private static OMElement generateOAuthClientSecretElement(OMFactory factory, OMNamespace synapseNS,
                                                              String clientSecret) {

        OMElement oauthClientSecret = factory.createOMElement(SynapseConstants.CLIENT_SECRET, synapseNS);
        oauthClientSecret.setText(clientSecret);
        return oauthClientSecret;
    }

    /**
     * Generates an OMElement representing the OAuth client ID.
     *
     * @param factory   the OMFactory used to create OMElements
     * @param synapseNS the OMNamespace for the Synapse configuration
     * @return the OMElement representing the OAuth client ID
     */
    private static OMElement generateOAuthClientIDElement(OMFactory factory, OMNamespace synapseNS, String clientId) {

        OMElement oauthClientID = factory.createOMElement(SynapseConstants.CLIENT_ID, synapseNS);
        oauthClientID.setText(clientId);
        return oauthClientID;
    }

    /**
     * Generates an OMElement representing the OAuth refresh token.
     *
     * @param factory      the OMFactory used to create OMElements
     * @param synapseNS    the OMNamespace for the Synapse configuration
     * @param refreshToken the refresh token
     * @return the OMElement representing the OAuth refresh token
     */
    private static OMElement generateOAuthRefreshTokenElement(OMFactory factory, OMNamespace synapseNS,
                                                              String refreshToken) {

        OMElement oauthRefreshToken = factory.createOMElement(SynapseConstants.REFRESH_TOKEN, synapseNS);
        oauthRefreshToken.setText(refreshToken);
        return oauthRefreshToken;
    }

    /**
     * Generates an OMElement representing the basic authentication credentials.
     * This element contains the username and password used for basic authentication.
     *
     * @param factory   the OMFactory used to create OMElements
     * @param synapseNS the OMNamespace for the Synapse configuration
     * @param username  the username
     * @param password  the password
     * @return the OMElement representing the basic authentication credentials
     */
    private static OMElement generateBasicAuthenticationElement(OMFactory factory, OMNamespace synapseNS,
                                                                String username, String password) {

        OMElement basicAuth = factory.createOMElement(SynapseConstants.BASIC_AUTH_TAG, synapseNS);
        OMElement usernameElement = generateAuthUsernameElement(factory, synapseNS, username);
        OMElement passwordElement = generateAuthPasswordElement(factory, synapseNS, password);

        basicAuth.addChild(usernameElement);
        basicAuth.addChild(passwordElement);
        return basicAuth;
    }

    /**
     * Generates an OMElement representing the authentication password.
     *
     * @param factory   the OMFactory used to create OMElements
     * @param synapseNS the OMNamespace for the Synapse configuration
     * @param password  the password
     * @return the OMElement representing the authentication password
     */
    private static OMElement generateAuthPasswordElement(OMFactory factory, OMNamespace synapseNS, String password) {

        OMElement authPassword = factory.createOMElement(SynapseConstants.PASSWORD, synapseNS);
        authPassword.setText(password);
        return authPassword;
    }

    /**
     * Generates an OMElement representing the authentication username.
     *
     * @param factory   the OMFactory used to create OMElements
     * @param synapseNS the OMNamespace for the Synapse configuration
     * @param username  the username
     * @return the OMElement representing the authentication username
     */
    private static OMElement generateAuthUsernameElement(OMFactory factory, OMNamespace synapseNS, String username) {

        OMElement authUsername = factory.createOMElement(SynapseConstants.USERNAME, synapseNS);
        authUsername.setText(username);
        return authUsername;
    }

    /**
     * Generates a map of connection elements from the provided OMElement.
     * This method iterates through the child elements of the given OMElement
     * and extracts various HTTP connection parameters, storing them in a map.
     *
     * @param connectionOMElement the OMElement representing the HTTP connection configuration
     * @return a map containing the HTTP connection parameters
     */
    private static Map<String, String> generateConnectionElementsMap(OMElement connectionOMElement) {

        HashMap<String, String> connectionElementsMap = new HashMap<>();
        Iterator connectionElements = connectionOMElement.getChildElements();
        while (connectionElements.hasNext()) {
            OMElement connectionElement = (OMElement) connectionElements.next();
            switch (connectionElement.getLocalName()) {
                case SynapseConstants.AUTH_TYPE:
                    connectionElementsMap.put(SynapseConstants.AUTH_TYPE, connectionElement.getText());
                    break;
                case SynapseConstants.USERNAME:
                    connectionElementsMap.put(SynapseConstants.USERNAME, connectionElement.getText());
                    break;
                case SynapseConstants.PASSWORD:
                    connectionElementsMap.put(SynapseConstants.PASSWORD, connectionElement.getText());
                    break;
                case SynapseConstants.OAUTH_AUTHORIZATION_MODE:
                    connectionElementsMap.put(SynapseConstants.OAUTH_AUTHORIZATION_MODE, connectionElement.getText());
                    break;
                case SynapseConstants.OAUTH_GRANT_TYPE:
                    connectionElementsMap.put(SynapseConstants.OAUTH_GRANT_TYPE, connectionElement.getText());
                    break;
                case SynapseConstants.CLIENT_ID:
                    connectionElementsMap.put(SynapseConstants.CLIENT_ID, connectionElement.getText());
                    break;
                case SynapseConstants.CLIENT_SECRET:
                    connectionElementsMap.put(SynapseConstants.CLIENT_SECRET, connectionElement.getText());
                    break;
                case SynapseConstants.TOKEN_URL:
                    connectionElementsMap.put(SynapseConstants.TOKEN_URL, connectionElement.getText());
                    break;
                case SynapseConstants.REFRESH_TOKEN:
                    connectionElementsMap.put(SynapseConstants.REFRESH_TOKEN, connectionElement.getText());
                    break;
                case SynapseConstants.ADDITIONAL_PROPERTIES:
                    connectionElementsMap.put(SynapseConstants.REQUEST_PARAMETERS, connectionElement.getText());
                    break;
                case SynapseConstants.TIMEOUT_DURATION:
                    connectionElementsMap.put(SynapseConstants.TIMEOUT_DURATION, connectionElement.getText());
                    break;
                case SynapseConstants.TIMEOUT_ACTION:
                    connectionElementsMap.put(SynapseConstants.TIMEOUT_ACTION, connectionElement.getText());
                    break;
                case SynapseConstants.SUSPEND_ERROR_CODES:
                    connectionElementsMap.put(SynapseConstants.SUSPEND_ERROR_CODES, connectionElement.getText());
                    break;
                case SynapseConstants.SUSPEND_INITIAL_DURATION:
                    connectionElementsMap.put(SynapseConstants.SUSPEND_INITIAL_DURATION, connectionElement.getText());
                    break;
                case SynapseConstants.SUSPEND_MAXIMUM_DURATION:
                    connectionElementsMap.put(SynapseConstants.SUSPEND_MAXIMUM_DURATION, connectionElement.getText());
                    break;
                case SynapseConstants.SUSPEND_PROGRESSION_FACTOR:
                    connectionElementsMap.put(SynapseConstants.SUSPEND_PROGRESSION_FACTOR, connectionElement.getText());
                    break;
                case SynapseConstants.RETRY_ERROR_CODES:
                    connectionElementsMap.put(SynapseConstants.RETRY_ERROR_CODES, connectionElement.getText());
                    break;
                case SynapseConstants.RETRY_COUNT:
                    connectionElementsMap.put(SynapseConstants.RETRY_COUNT, connectionElement.getText());
                    break;
                case SynapseConstants.RETRY_DELAY:
                    connectionElementsMap.put(SynapseConstants.RETRY_DELAY, connectionElement.getText());
                    break;
                case SynapseConstants.NAME:
                    connectionElementsMap.put(SynapseConstants.NAME, connectionElement.getText());
                    break;
                case SynapseConstants.TRACE:
                    connectionElementsMap.put(SynapseConstants.TRACE, connectionElement.getText());
                    break;
                case SynapseConstants.STATISTICS:
                    connectionElementsMap.put(SynapseConstants.STATISTICS, connectionElement.getText());
                    break;
                case SynapseConstants.MISCELLANEOUS_DESCRIPTION:
                    connectionElementsMap.put(SynapseConstants.MISCELLANEOUS_DESCRIPTION, connectionElement.getText());
                    break;
                case SynapseConstants.MISCELLANEOUS_PROPERTIES:
                    connectionElementsMap.put(SynapseConstants.MISCELLANEOUS_PROPERTIES, connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_OPTION:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_OPTION,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_VERSION:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_VERSION,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_SEPARATE_LISTENER:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_ADDRESS_SEPARATE_LISTENER,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OPTION:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OPTION,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_OUTBOUND_POLICY_OPTION:
                    connectionElementsMap.put(
                            SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_OUTBOUND_POLICY_OPTION,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_SECURITY_POLICY_KEY:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_POLICY_KEY,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_POLICY_KEY:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_INBOUND_POLICY_KEY,
                            connectionElement.getText());
                    break;
                case SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OUTBOUND_POLICY_KEY:
                    connectionElementsMap.put(SynapseConstants.QUALITY_OF_SERVICE_SECURITY_OUTBOUND_POLICY_KEY,
                            connectionElement.getText());
                    break;
                default:
                    // Ignore unknown elements
            }
        }
        return connectionElementsMap;
    }
}
