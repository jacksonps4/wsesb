package com.minorityhobbies.wsesb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProcessMessageBus extends AbstractMessageBus implements MessageBus, AutoCloseable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService thread = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<InternalEsbMessage> outboundQueue = new LinkedBlockingQueue<>();

    public ProcessMessageBus() {
        thread.submit(this::publishMessagesFromQueue);
    }

    void publishMessagesFromQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                InternalEsbMessage msg = outboundQueue.take();
                publishMessageToSubscribers(msg.getHeaders(), msg.getPayload());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("ProcessMessageBus dispatcher thread terminated");
            } catch (Exception e) {
                logger.error("Failed to publish to subscriber: this is a client error", e);
            }
        }
    }

    @Override
    public Future<Boolean> publish(Map<String, String> headers, Object msg) {
        try {
            outboundQueue.put(new InternalEsbMessage(headers, msg));
            return CompletableFuture.completedFuture(true);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while writing to outbound message queue");
            return CompletableFuture.completedFuture(false);
        }
    }

    public <T, R> R sendRequest(String destination, T request, Class<? extends R> responseType,
                                long maxWait, TimeUnit unit) {
        String replyDestination = UUID.randomUUID().toString();
        Map<String, String> header = createHeaders()
                .destination(destination)
                .replyTo(replyDestination)
                .build();
        AtomicReference<R> response = new AtomicReference<>();
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        Closeable handle = subscribe(responseHeader -> replyDestination
                        .equals(responseHeader.get(Headers.HeaderName.DESTINATION.toString())),
                (responseHeader, responseMsg) -> {
                    try {
                        lock.lock();
                        response.set((R) responseMsg);
                        condition.signalAll();
                        return true;
                    } finally {
                        lock.unlock();
                    }
                });
        publish(header, request);
        try {
            lock.lock();
            for (long start = System.currentTimeMillis(); response.get() == null; ) {
                if ((System.currentTimeMillis() - start) > TimeUnit.MILLISECONDS.convert(maxWait, unit)) {
                    break;
                }
                condition.await(maxWait, unit);
            }
            handle.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.warn("Failed to close request-response handle");
        } finally {
            lock.unlock();
        }

        return response.get();
    }

    @Override
    public void close() throws Exception {
        thread.shutdownNow();
    }
}
