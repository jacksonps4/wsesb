package com.minorityhobbies.wsesb;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Support for ESB over WebSockets. Designed to support a @ClientEndpoint which connects
 * to a single WebSocket endpoint. Messages published to the internal bus are sent over
 * the WebSocket. Messages received via the WebSocket connection are published to the
 * internal bus.
 */
public class OutboundWebSocketMessageBus extends AbstractWebSocketMessageBus {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private AtomicReference<Session> session = new AtomicReference<>();
    private Closeable handle;

    public OutboundWebSocketMessageBus(Supplier<MessageBus> messageBusProvider) {
        super(messageBusProvider);
    }

    @OnOpen
    public void open(Session session) {
        logger.info(String.format("Client endpoint opened web socket message bus session: %s", session.getId()));
        handle = subscribe(x -> true, this::publishToWebSocketSession);
        this.session.set(session);
    }

    @OnClose
    public void close(Session session, CloseReason closeReason) {
        logger.info(String.format("Client endpoint closed web socket message bus session: %s", session.getId()));
        this.session.set(null);
        try {
            handle.close();
        } catch (IOException e) {
            logger.warn("Failed to close handle", e);
        }
    }

    @OnMessage
    public void onMessage(EsbMessage msg) {
        publishMessageToSubscribers(msg.getHeaders(), msg.getPayload());
    }

    /**
     * Publishes the specified message to the connected remote websocket endpoint.
     * @param headers   The message headers
     * @param msg   The message payload
     * @return  A Future which will return true if the message was successfully sent
     * to the remote websocket. This does not guarantee delivery.
     */
    @Override
    public Future<Boolean> publish(Map<String, String> headers, Object msg) {
        return publishToRemoteSession(this.session.get(), headers, msg);
    }

    @Override
    public Future<Boolean> publish(Object msg) {
        return publish(createHeaders().build(), msg);
    }

    boolean publishToWebSocketSession(Map<String, String> headers, Object msg) {
        try {
            return publish(headers, msg).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.info("Failed to publish to websocket session");
            return false;
        }
    }
}
