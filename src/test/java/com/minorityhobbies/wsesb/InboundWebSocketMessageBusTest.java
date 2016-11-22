package com.minorityhobbies.wsesb;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InboundWebSocketMessageBusTest extends AbstractWebSocketTestCase {
    private ProcessMessageBus processMessageBus;
    private InboundWebSocketMessageBus inboundWebSocketMessageBus;

    @Mock
    private Session session1;

    @Mock
    private Session session2;

    @Mock
    private RemoteEndpoint.Basic basicRemote1;

    @Mock
    private RemoteEndpoint.Basic basicRemote2;

    @Before
    public void setUp() {
        processMessageBus = new ProcessMessageBus();

        when(session1.getId()).thenReturn("remote-session-1");
        when(session1.getBasicRemote()).thenReturn(basicRemote1);
        when(session2.getId()).thenReturn("remote-session-2");
        when(session2.getBasicRemote()).thenReturn(basicRemote2);

        inboundWebSocketMessageBus = new InboundWebSocketMessageBus(() -> processMessageBus);
    }

    @Test
    public void internalBusMessagePublishedToWebSockets() throws Exception {
        Map<String, String> headers = inboundWebSocketMessageBus.createHeaders()
                .build();
        Object msg = new Object();

        inboundWebSocketMessageBus.open(session1);
        inboundWebSocketMessageBus.open(session2);

        inboundWebSocketMessageBus.publishMessageToSubscribers(headers, msg);

        EsbMessage wireMessage1 = verifyMessageReceived(basicRemote1, headers, msg);

        verify(session1).getId();
        verify(session1).getBasicRemote();
        verify(basicRemote1).sendObject(wireMessage1);

        EsbMessage wireMessage2 = verifyMessageReceived(basicRemote2, headers, msg);
        verify(session2).getId();
        verify(session2).getBasicRemote();
        verify(basicRemote2).sendObject(wireMessage2);

        verifyNoMoreInteractions(session1, basicRemote1, session2, basicRemote2);
    }

    private EsbMessage verifyMessageReceived(RemoteEndpoint.Basic basicRemote, Map<String, String> headers, Object msg) throws IOException, EncodeException {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(basicRemote).sendObject(captor.capture());

        EsbMessage wireMessage = (EsbMessage) captor.getValue();

        assertEquals(headers, wireMessage.getHeaders());
        assertEquals(msg, wireMessage.getPayload());
        return wireMessage;
    }

    @Test
    public void webSocketMessagePublishedToInternalBus() throws IOException {
        try (Closeable handle = processMessageBus.subscribe(x -> true, this::onMessage)) {
            EsbMessage msg = createEsbMessage();
            inboundWebSocketMessageBus.onMessage(msg);

            assertMessageReceived(() -> msg, 5L, TimeUnit.SECONDS);
        };

        verifyNoMoreInteractions(session1, basicRemote1, session2, basicRemote2);
    }
}
