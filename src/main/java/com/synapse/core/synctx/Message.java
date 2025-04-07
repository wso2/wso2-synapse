package com.synapse.core.synctx;

import java.util.Arrays;

public class Message {
    private byte[] rawPayload;
    private String contentType;

    public Message() {
        this.rawPayload = new byte[0];
        this.contentType = "";
    }

    public Message(byte[] rawPayload, String contentType) {
        this.rawPayload = rawPayload;
        this.contentType = contentType;
    }

    public byte[] getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(byte[] rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return "Message{" +
                "rawPayload=" + Arrays.toString(rawPayload) +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
