package com.minorityhobbies.wsesb;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OutboundWebSocketMessageBusTest extends AbstractWebSocketTestCase {
    private ProcessMessageBus processMessageBus;
    private OutboundWebSocketMessageBus outboundWebSocketMessageBus;

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Basic basicRemote;

    @Before
    public void setUp() {
        processMessageBus = new ProcessMessageBus();
        when(session.getId()).thenReturn("remote-session");
        when(session.getBasicRemote()).thenReturn(basicRemote);

        outboundWebSocketMessageBus = new OutboundWebSocketMessageBus(() -> processMessageBus);
    }

    @Test
    public void messageSentOverWebSocketPublishedToLocalBus() {
        EsbMessage wireMessage = createEsbMessage();
        processMessageBus.subscribe(x -> true, this::onMessage);
        outboundWebSocketMessageBus.open(session);
        outboundWebSocketMessageBus.onMessage(wireMessage);

        assertMessageReceived(() -> wireMessage, 5L, TimeUnit.SECONDS);

        verify(session).getId();
        verifyNoMoreInteractions(session, basicRemote);
    }

    @Test
    public void messagePublishedToLocalBusSentOverWebSocket() throws IOException, EncodeException {
        Map<String, String> headers = processMessageBus.createHeaders()
                .build();
        Object msg = new Object();

        outboundWebSocketMessageBus.open(session);

        processMessageBus.publishMessageToSubscribers(headers, msg);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        verify(session).getBasicRemote();
        verify(basicRemote).sendObject(captor.capture());

        EsbMessage wireMessage = (EsbMessage) captor.getValue();
        assertEquals(headers, wireMessage.getHeaders());
        assertEquals(msg, wireMessage.getPayload());

        verify(session).getId();
        verifyNoMoreInteractions(session, basicRemote);
    }
}
