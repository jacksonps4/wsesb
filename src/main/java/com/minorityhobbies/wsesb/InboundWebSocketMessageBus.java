package com.minorityhobbies.wsesb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Support for an ESB over WebSockets. Designed to support a @ServerEndpoint instance
 * which receives multiple inbound connections. Inbound messages over any websocket are
 * published to the local message bus. Any messages published to the local message bus
 * are sent over all connected websockets.
 */
public class InboundWebSocketMessageBus extends AbstractWebSocketMessageBus {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public InboundWebSocketMessageBus(Supplier<MessageBus> messageBusProvider) {
        super(messageBusProvider);
        subscribe(x -> true, this::publishToAllConnectedEndpoints);
    }

    boolean publishToAllConnectedEndpoints(Map<String, String> headers, Object msg) {
        try {
            publish(headers, msg).get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Interrupted publishing to web socket", e);
            return false;
        }
    }

    @OnOpen
    public void open(Session session) {
        logger.info(String.format("Server endpoint opened web socket message bus session: %s", session.getId()));
        this.sessions.put(session.getId(), session);
    }

    @OnClose
    public void close(Session session, CloseReason closeReason) {
        logger.info(String.format("Server endpoint closed web socket message bus session: %s", session.getId()));
        this.sessions.remove(session.getId());
    }

    @OnMessage
    public void onMessage(EsbMessage msg) {
        publishMessageToSubscribers(msg.getHeaders(), msg.getPayload());
    }

    @Override
    public Future<Boolean> publish(Map<String, String> headers, Object msg) {
        this.sessions.values().stream()
                .forEach(session -> publishToRemoteSession(session, headers, msg));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Future<Boolean> publish(Object msg) {
        return publish(createHeaders().build(), msg);
    }
}
