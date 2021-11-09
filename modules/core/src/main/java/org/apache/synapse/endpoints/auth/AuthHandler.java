package org.apache.synapse.endpoints.auth;

import org.apache.synapse.MessageContext;

public interface AuthHandler {

    /**
     * Gets the auth handler type of the instance.
     */
    String getAuthType();

    /**
     * This method will set the Authorization header with the relevant auth token.
     *
     * @param messageContext Message context to which the token needs to be set
     * @throws AuthException In the event of errors when generating new token
     */
    void setAuthHeader(MessageContext messageContext) throws AuthException;
}
