package org.apache.synapse.commons.handlers;

/**
 * Message holder for inbound request info.
 */
public class MessageInfo {

    /**
     * The message to be passed to the handler implementations to perform message
     * handling. This is in Object type since there are variety of message types in
     * different protocols.
     */
    private Object message;

    /**
     * The protocol of the transport that the message belongs to.
     */
    private Protocol protocol;

    public ConnectionId connectionId;

    public MessageInfo(Object message, Protocol protocol, ConnectionId connectionId) {

        this.message = message;
        this.protocol = protocol;
        this.connectionId = connectionId;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public ConnectionId getConnectionId() {

        return connectionId;
    }

    public void setConnectionId(ConnectionId connectionId) {

        this.connectionId = connectionId;
    }
}
