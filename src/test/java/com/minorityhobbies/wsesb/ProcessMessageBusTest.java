package com.minorityhobbies.wsesb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProcessMessageBusTest {
    private ProcessMessageBus processMessageBus;
    private AtomicReference<Map<String, String>> headersContainer;
    private AtomicReference<Object> msgContainer;
    @Before
    public void init() {
        headersContainer = new AtomicReference<>();
        msgContainer = new AtomicReference<>();
        processMessageBus = new ProcessMessageBus();
    }

    @Test
    public void publishedMessageIsReceivedWhenFilterMatches() {
        Map<String, String> headers = processMessageBus.createHeaders().build();
        Object msg = new Object();

        MessageContainer msgContainer = new MessageContainer();
        processMessageBus.subscribe(x -> true, msgContainer);
        processMessageBus.publish(headers, msg);
        validateMessageReceived(headers, msg, msgContainer);
    }

    @Test
    public void publishedMessageIsReceivedByAllSubscribersWhenFilterMatches() {
        Map<String, String> headers = processMessageBus.createHeaders().build();
        Object msg = new Object();

        MessageContainer msgContainer1 = new MessageContainer();
        MessageContainer msgContainer2 = new MessageContainer();
        MessageContainer msgContainer3 = new MessageContainer();
        processMessageBus.subscribe(x -> true, msgContainer1);
        processMessageBus.subscribe(x -> true, msgContainer2);
        processMessageBus.subscribe(x -> true, msgContainer3);

        processMessageBus.publish(headers, msg);

        validateMessageReceived(headers, msg, msgContainer1);
        validateMessageReceived(headers, msg, msgContainer2);
        validateMessageReceived(headers, msg, msgContainer3);
    }

    @Test
    public void publishedMessageIsReceivedBySubscribersWhenFilterMatches() {
        Map<String, String> headers = processMessageBus.createHeaders().build();
        Object msg = new Object();

        MessageContainer msgContainer1 = new MessageContainer();
        MessageContainer msgContainer2 = new MessageContainer();
        MessageContainer msgContainer3 = new MessageContainer();
        processMessageBus.subscribe(x -> true, msgContainer1);
        processMessageBus.subscribe(x -> false, msgContainer2);
        processMessageBus.subscribe(x -> true, msgContainer3);

        processMessageBus.publish(headers, msg);

        validateMessageReceived(headers, msg, msgContainer1);
        validateMessageReceived(headers, msg, msgContainer3);

        msgContainer2.await(2, TimeUnit.SECONDS);
        assertNull(msgContainer2.getHeaders());
        assertNull(msgContainer2.getMsg());
    }

    private void validateMessageReceived(Map<String, String> headers, Object msg, MessageContainer msgContainer1) {
        msgContainer1.await(5, TimeUnit.SECONDS);
        assertEquals(headers, msgContainer1.getHeaders());
        assertEquals(msg, msgContainer1.getMsg());
    }

    @Test
    public void publishedMessageNotReceivedWhenNoFilterMatch() {
        Map<String, String> headers = processMessageBus.createHeaders().build();
        Object msg = new Object();

        MessageContainer msgContainer = new MessageContainer();
        processMessageBus.subscribe(x -> false, msgContainer);
        processMessageBus.publish(headers, msg);
        msgContainer.await(2, TimeUnit.SECONDS);
        assertNull(msgContainer.getHeaders());
        assertNull(msgContainer.getMsg());
    }

    @Test
    public void syncRequestResponse() throws IOException {
        String responseMsg = new String("blah");
        AtomicReference<Object> requestContainer = new AtomicReference<>();
        Closeable handle = processMessageBus.subscribe(requestHeader -> "destination-name".equals(requestHeader.get(Headers.HeaderName.DESTINATION.toString())),
                (requestHeader, requestMsg) -> {
                    try {
                        requestContainer.set(requestMsg);
                        String replyDestination = requestHeader.get(Headers.HeaderName.REPLY_TO.toString());
                        Map<String, String> responseHeader = processMessageBus
                                .createHeaders()
                                .destination(replyDestination)
                                .build();
                        return processMessageBus.publish(responseHeader, responseMsg)
                                .get();
                    } catch (InterruptedException | ExecutionException e) {
                        return null;
                    }
                });


        Object request = new Object();
        Object response = processMessageBus.sendRequest("destination-name", request, Object.class, 5, TimeUnit.SECONDS);

        assertEquals(request, requestContainer.get());
        assertEquals(responseMsg, response);

        handle.close();
    }
}
