package com.minorityhobbies.wsesb;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface MessageBus {
    Future<Boolean> publish(Map<String, String> headers, Object msg);
    Future<Boolean> publish(Object msg);
    Closeable subscribe(Predicate<Map<String, String>> matcher,
                        BiFunction<Map<String, String>, ? super Object, Boolean> handler);
    Headers createHeaders();
    String getOriginId();

    static Predicate<Map<String, String>> destinationMatcher(String destination) {
        return headers -> destination.equals(headers.get(Headers.HeaderName.DESTINATION.toString()));
    }
}
