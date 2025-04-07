    package com.synapse.core.synctx;

    import java.util.HashMap;
    import java.util.Map;

    public class MsgContext {
        private Map<String, String> properties;
        private Message message;
        private Map<String, String> headers;

        public MsgContext() {
            this.properties = new HashMap<>();
            this.message = new Message();
            this.headers = new HashMap<>();
        }

        public static MsgContext createMsgContext() {
            return new MsgContext();
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        @Override
        public String toString() {
            return "MsgContext{" +
                    "properties=" + properties +
                    ", message=" + message +
                    ", headers=" + headers +
                    '}';
        }
    }
