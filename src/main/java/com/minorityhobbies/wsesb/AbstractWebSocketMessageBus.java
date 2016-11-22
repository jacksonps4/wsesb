package com.minorityhobbies.wsesb;

import javax.websocket.EncodeException;
import javax.websocket.Session;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

abstract class AbstractWebSocketMessageBus implements MessageBus {
    private final MessageBus messageBus;

    public AbstractWebSocketMessageBus(Supplier<MessageBus> messageBus) {
        this.messageBus = messageBus.get();
    }

    Future<Boolean> publishToRemoteSession(Session session, Map<String, String> headers, Object msg) {
        // don't re-publish messages that were not originated by the internal bus (to avoid cycles)
        if (!messageBus.getOriginId().equals(headers.get(Headers.HeaderName.ORIGIN_ID.toString()))) {
            return CompletableFuture.completedFuture(false);
        }
        if (session != null) {
            try {
                session.getBasicRemote()
                        .sendObject(new EsbMessage(headers, msg));
                return CompletableFuture.completedFuture(true);
            } catch (IOException | EncodeException e) {
                throw new RuntimeException(e);
            }
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    public Future<Boolean> publishMessageToSubscribers(Map<String, String> headers, Object msg) {
        return messageBus.publish(headers, msg);
    }

    @Override
    public Closeable subscribe(Predicate<Map<String, String>> matcher, BiFunction<Map<String, String>, ? super Object, Boolean> handler) {
        return messageBus.subscribe(matcher, handler);
    }

    @Override
    public Headers createHeaders() {
        return messageBus.createHeaders();
    }

    @Override
    public String getOriginId() {
        return messageBus.getOriginId();
    }
}
