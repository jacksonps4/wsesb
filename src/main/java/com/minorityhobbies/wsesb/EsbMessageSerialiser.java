package com.minorityhobbies.wsesb;

import javax.websocket.*;
import java.io.*;
import java.nio.ByteBuffer;

public class EsbMessageSerialiser implements Decoder.Binary<EsbMessage>, Encoder.Binary<EsbMessage> {
    @Override
    public ByteBuffer encode(EsbMessage esbMessage) throws EncodeException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(esbMessage);
        } catch (IOException e) {
            throw new EncodeException(esbMessage, "Failed to serialise object", e);
        }
        return ByteBuffer.wrap(bos.toByteArray());
    }

    @Override
    public EsbMessage decode(ByteBuffer msg) throws DecodeException {
        ByteArrayInputStream bin = new ByteArrayInputStream(msg.array());
        try (ObjectInputStream oin = new ObjectInputStream(bin)) {
            return (EsbMessage) oin.readObject();
        } catch (IOException e) {
            throw new DecodeException(msg, "Failed to deserialise object", e);
        } catch (ClassNotFoundException e) {
            throw new DecodeException(msg, "Failed to find class for deserialising object", e);
        }
    }

    @Override
    public boolean willDecode(ByteBuffer byteBuffer) {
        return true;
    }

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }
}
