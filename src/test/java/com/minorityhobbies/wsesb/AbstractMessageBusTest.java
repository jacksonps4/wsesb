package com.minorityhobbies.wsesb;


import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class AbstractMessageBusTest {
    private AbstractMessageBus abstractMessageBus;
    private Map<String, String> headers;
    private Object msg;

    @Before
    public void setUp() {
        abstractMessageBus = new AbstractMessageBus() {
            @Override
            public Future<Boolean> publish(Map<String, String> headers, Object msg) {
                return null;
            }
        };

        abstractMessageBus.subscribe(x -> true, this::onMessage);
    }

    @Test
    public void headersAndMessageAreReceived() {
        Map<String, String> headers = abstractMessageBus.createHeaders()
                .build();
        Object msg = new Object();
        abstractMessageBus.publishMessageToSubscribers(headers, msg);

        assertEquals(headers, this.headers);
        assertEquals(msg, this.msg);
    }

    @Test
    public void originIdIsCorrect() {
        Map<String, String> headers = abstractMessageBus.createHeaders()
                .build();
        Object msg = new Object();
        abstractMessageBus.publishMessageToSubscribers(headers, msg);
        assertEquals(abstractMessageBus.getOriginId(), this.headers.get(Headers.HeaderName.ORIGIN_ID.toString()));
    }

    boolean onMessage(Map<String, String> headers, Object msg) {
        this.headers = headers;
        this.msg = msg;
        return true;
    }
}
