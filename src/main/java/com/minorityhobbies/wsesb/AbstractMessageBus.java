package com.minorityhobbies.wsesb;

import java.io.Closeable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Predicate;

abstract class AbstractMessageBus implements MessageBus {
    private final String originId = UUID.randomUUID().toString();
    private final Map<Predicate<Map<String, String>>,
            BiFunction<Map<String, String>, ? super Object, Boolean>> consumers = new ConcurrentHashMap<>();

    protected final boolean publishMessageToSubscribers(Map<String, String> headers, Object msg) {
        return consumers.entrySet().stream()
                .filter(entry -> entry.getKey().test(headers))
                .map(Map.Entry::getValue)
                .map(consumer -> consumer.apply(headers, msg))
                .reduce(Boolean.FALSE, (a, b) -> a || b);
    }

    @Override
    public Future<Boolean> publish(Object msg) {
        return publish(createHeaders().build(), msg);
    }

    @Override
    public final Closeable subscribe(Predicate<Map<String, String>> matcher,
                                     BiFunction<Map<String, String>, ? super Object, Boolean> handler) {
        consumers.put(matcher, handler);
        return () -> consumers.remove(matcher);
    }

    @Override
    public Headers createHeaders() {
        return new Headers(originId);
    }

    public String getOriginId() {
        return originId;
    }
}
