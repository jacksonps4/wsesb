package com.minorityhobbies.wsesb;

import java.io.Serializable;
import java.util.Map;

/**
 * Wire-format message for transmission between processes
 */
public class EsbMessage implements Serializable {
    private final Map<String, String> headers;
    private final Object payload;

    public EsbMessage(Map<String, String> headers, Object payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getPayload() {
        return payload;
    }
}
