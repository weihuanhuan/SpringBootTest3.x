package com.jasonfitch.test.springboot3.mq.stomp;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

public class SubscribeFutureStompFrameHandler<T> extends CompletableFuture<T> implements StompFrameHandler {

    @Override
    public Type getPayloadType(StompHeaders headers) {
        System.out.println("subscribe.getPayloadType");

        // TODO how to dynamically determine the payload type with class generic type argument named [<T>] ?
        return String.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        System.out.println("subscribe.handleFrame");

        try {
            T payloadString = (T) payload;
            if (payloadString != null) {
//                    String exceptionMessage = "test-payload-exception";
//                    System.out.println(" ########" + exceptionMessage + " ########");
//                    throw new RuntimeException(exceptionMessage);
            }
            super.complete(payloadString);
        } catch (Exception exception) {
            super.completeExceptionally(exception);
        }
    }

}