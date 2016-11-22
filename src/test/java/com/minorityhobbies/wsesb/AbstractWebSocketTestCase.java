package com.minorityhobbies.wsesb;

import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

class AbstractWebSocketTestCase {
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    Map<String, String> headers;
    Object msg;

    boolean onMessage(Map<String, String> headers, Object msg) {
        try {
            lock.lock();

            this.headers = headers;
            this.msg = msg;

            condition.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    void assertMessageReceived(Supplier<EsbMessage> msg, long maxWait, TimeUnit unit) {
        try {
            lock.lock();
            if (this.headers == null) {
                condition.await(maxWait, unit);
            }
            if (this.headers == null) {
                fail("Message not received");
            }

            assertEquals(this.headers, msg.get().getHeaders());
            assertEquals(this.msg, msg.get().getPayload());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    EsbMessage createEsbMessage() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Headers.HeaderName.MESSAGE_ID.toString(), "1");
        headers.put(Headers.HeaderName.ORIGIN_ID.toString(), "elsewhere");

        Object msg = new Object();
        return new EsbMessage(headers, msg);
    }
}
