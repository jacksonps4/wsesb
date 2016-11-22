package com.minorityhobbies.wsesb;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Headers {
    public enum HeaderName {
        ORIGIN_ID("originId"),
        MESSAGE_ID("messageId"),
        DESTINATION("destination"),
        REPLY_TO("replyTo"),
        CORRELATION_ID("correlationId");

        private final String key;

        HeaderName(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    private final Map<String, String> headers = new HashMap<>();

    Headers(String originId) {
        headers.put(HeaderName.ORIGIN_ID.toString(), originId);
        headers.put(HeaderName.MESSAGE_ID.toString(), UUID.randomUUID().toString());
    }

    public Headers destination(String destination) {
        headers.put(HeaderName.DESTINATION.toString(), destination);
        return this;
    }

    public Headers replyTo(String replyTo) {
        headers.put(HeaderName.REPLY_TO.toString(), replyTo);
        return this;
    }

    public Headers correlationId(String correlationId) {
        headers.put(HeaderName.CORRELATION_ID.toString(), correlationId);
        return this;
    }

    public Map<String, String> build() {
        return headers;
    }
}
