package com.jasonfitch.test.springboot3.mq.stomp;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;

public class ConnectionStompSessionHandler extends StompSessionHandlerAdapter {

    @Override
    public Type getPayloadType(StompHeaders headers) {
        System.out.println("connection.getPayloadType");
        return super.getPayloadType(headers);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        System.out.println("connection.handleFrame");
        super.handleFrame(headers, payload);
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("connection.afterConnected");
        super.afterConnected(session, connectedHeaders);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        System.out.println("connection.handleException");
        super.handleException(session, command, headers, payload, exception);
        throw new RuntimeException("connection.handleException", exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        System.out.println("connection.handleTransportError");
        super.handleTransportError(session, exception);
        throw new RuntimeException("connection.handleTransportError", exception);
    }

}
