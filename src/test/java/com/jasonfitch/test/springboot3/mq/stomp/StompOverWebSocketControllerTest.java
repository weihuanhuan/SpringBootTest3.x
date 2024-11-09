package com.jasonfitch.test.springboot3.mq.stomp;

import com.jasonfitch.test.springboot3.mq.stomp.controller.StompOverWebSocketController;
import com.jasonfitch.test.springboot3.mq.stomp.entity.Greeting;
import com.jasonfitch.test.springboot3.mq.stomp.entity.HelloMessage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SpringBootTest
public class StompOverWebSocketControllerTest {

    private static final String HTML_ENDPOINT = "/mq/stomp/StompOverWebSocket.html";

    private static final String WEBSOCKET_ENDPOINT = "/gs-guide-websocket";
    private static final String WEBSOCKET_URL = "ws://localhost:8080" + WEBSOCKET_ENDPOINT;

    private static final String APPLICATION_DESTINATION_NAME = "/app/hello";

    private static final String SUBSCRIBE_NAME = "/topic/subscribe-test";
    private static final String SUBSCRIBE_PAYLOAD = "payload-from-websocket-java-client";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private StompOverWebSocketController stompOverWebSocketController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void contextLoads() {
        Assertions.assertThat(webApplicationContext).isNotNull();
        Assertions.assertThat(simpMessagingTemplate).isNotNull();
        Assertions.assertThat(stompOverWebSocketController).isNotNull();
    }

    @Test
    void testHttpRequestToHtmlEndpoint() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(HTML_ENDPOINT))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testHttpRequestToWebsocketEndpoint() throws Exception {
        //TODO why response with actual 400 instead of expected 404 ?
        mockMvc.perform(MockMvcRequestBuilders.get(WEBSOCKET_ENDPOINT))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void shouldSendMessageToMessageMapping() {
        //TODO why server side not logging the message ?
        simpMessagingTemplate.convertAndSend(APPLICATION_DESTINATION_NAME, "Test message");
    }

    @Test
    void shouldReturnGreetingMessage() {
        String name = "World";

        HelloMessage helloMessage = new HelloMessage();
        helloMessage.setName(name);
        Greeting greeting = stompOverWebSocketController.greeting(helloMessage);
        Assertions.assertThat(greeting.getContent()).isEqualTo("Hello, " + name + "!");
    }

    @Test
    void shouldEscapeHtmlInGreetingMessage() {
        HelloMessage helloMessage = new HelloMessage();
        helloMessage.setName("<script>alert('XSS')</script>");
        Greeting greeting = stompOverWebSocketController.greeting(helloMessage);
        Assertions.assertThat(greeting.getContent()).isEqualTo("Hello, &lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;!");
    }

    /**
     * @see "https://docs.spring.io/spring-framework/reference/web/websocket/stomp/client.html"
     */
    @Test
    void shouldConnectAndSendMessageUsingWebSocketClient() throws ExecutionException, InterruptedException, TimeoutException {
        // create a WebSocket client
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        // create a stomp client
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        // receive the result from the subscription
        //TODO why we must startup the server manually before running this test rather than startup by @SpringBootTest ?
        SubscribeFutureStompFrameHandler<String> subscribeFutureStompFrameHandler = new SubscribeFutureStompFrameHandler<>();

        // connect to the WebSocket endpoint
        StompSessionHandlerAdapter connectionStompSessionHandler = new ConnectionStompSessionHandler();
        CompletableFuture<StompSession> sessionCompletableFuture = stompClient.connectAsync(WEBSOCKET_URL, connectionStompSessionHandler);
        sessionCompletableFuture.thenApplyAsync((session) -> {
            System.out.println("######## session.subscribe ########");
            StompSession.Subscription subscribe = session.subscribe(SUBSCRIBE_NAME, subscribeFutureStompFrameHandler);
            if (subscribe != null) {
//                String exceptionMessage = "test-subscribe-exception";
//                System.out.println("######## " + exceptionMessage + " ########");
//                throw new RuntimeException(exceptionMessage);
            }
            return session;
        }).thenApply((session) -> {
            System.out.println("######## session.send ########");
            StompSession.Receiptable send = session.send(SUBSCRIBE_NAME, SUBSCRIBE_PAYLOAD);
            if (send != null) {
                // because test-send-exception is after the session.send executed,
                // so the send is success cause the subscribe future success, and the session future finish with an exception
                // but @see java.util.concurrent.CompletableFuture.internalComplete can only complete once,
                // so the exception of @see java.util.concurrent.CompletableFuture.exceptionally from session future is ignored
                // 【return RESULT.compareAndSet(this, null, r);】
                // finally once the send is success, the subscribe future may finish with the payload prior to the exception with session future
//                String exceptionMessage = "test-send-exception";
//                System.out.println("######## " + exceptionMessage + " ########");
//                throw new RuntimeException(exceptionMessage);
            }
            return send;
        }).runAfterEither(subscribeFutureStompFrameHandler, () -> {
            System.out.println("######## session.runAfterEither");
            subscribeFutureStompFrameHandler.completeOnTimeout("completeOnTimeout ", 1000 * 3, TimeUnit.MILLISECONDS);
        }).exceptionally((throwable) -> {
            System.out.println("######## session.exceptionally=" + throwable);
            subscribeFutureStompFrameHandler.completeExceptionally(throwable);
            return null;
        });

        // wait for receive message completion
        String future = subscribeFutureStompFrameHandler.get(1000 * 3 * 1000, TimeUnit.MILLISECONDS);
        System.out.println("######## future=" + future);
        Assertions.assertThat(future).isEqualTo(SUBSCRIBE_PAYLOAD);
    }

}