package org.apache.synapse.mediators.opa;

import org.apache.synapse.MessageContext;

import java.util.Map;

public interface OPARequestGenerator {

    String createRequest(MessageContext messageContent,  Map<String, Object> advancedProperties);

    boolean handleResponse(MessageContext messageContent, String response);

}
