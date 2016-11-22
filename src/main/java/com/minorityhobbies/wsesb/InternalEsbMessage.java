package com.minorityhobbies.wsesb;

import java.util.Map;

class InternalEsbMessage {
    private final Map<String, String> headers;
    private final Object payload;

    public InternalEsbMessage(Map<String, String> headers, Object payload) {
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
