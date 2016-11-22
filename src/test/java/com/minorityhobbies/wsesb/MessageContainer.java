package com.minorityhobbies.wsesb;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

class MessageContainer implements BiFunction<Map<String, String>, Object, Boolean> {
    private final AtomicReference<Map<String, String>> headersContainer = new AtomicReference<>();
    private final AtomicReference<Object> msgContainer = new AtomicReference<>();
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile boolean set = false;

    @Override
    public Boolean apply(Map<String, String> headers, Object msg) {
        try {
            lock.lock();
            headersContainer.set(headers);
            msgContainer.set(msg);

            set = true;
            condition.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void await(long time, TimeUnit unit) {
        try {
            lock.lock();
            long start = System.currentTimeMillis();
            while (!set) {
                if ((System.currentTimeMillis() - start) > TimeUnit.MILLISECONDS.convert(time, unit)) {
                    break;
                }
                condition.await(time, unit);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, String> getHeaders() {
        return headersContainer.get();
    }

    public Object getMsg() {
        return msgContainer.get();
    }
}
