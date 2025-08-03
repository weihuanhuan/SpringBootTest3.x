package com.jasonfitch.test.springboot3.cloud.stream.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.FunctionBindingTestUtils;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@EnableTestBinder
@SpringBootTest
class LogEnricherApplicationUnitTest {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    /**
     * 关于这里为什么发送给 queue.log.messages 的消息能被 queue.pretty.log.messages 所接收的根本原因就是 spring cloud stream 内部处理了映射关系
     * 这里的核心就是将 InputDestination 对应 【enrichLogMessage-in-0】 ，将 OutputDestination 对应 【enrichLogMessage-out-0】
     * 而他们是用一个 enrichLogMessage 函数的 2 端， in 是 source ，而 out 是 sink 端，所以发送到 in 的信息，都会被转发给 out 中
     *
     * @see OutputDestination#outputQueue(String)
     * @see FunctionBindingTestUtils#bind(ConfigurableApplicationContext, Object)
     */
    @Test
    void whenSendingLogMessage_thenItsEnrichedWithPrefix() {
        Message<String> sendMessage = MessageBuilder
                .withPayload("hello world")
                .build();

        input.send(sendMessage, "queue.log.messages");

        Message<byte[]> receiveMessage = output.receive(1000L, "queue.pretty.log.messages");

        byte[] payload = receiveMessage.getPayload();
        assertThat(payload)
                .asString()
                .isEqualTo("[Baeldung] - hello world");
    }

    @Test
    void whenProcessingLongLogMessage_thenItsEnrichedWithPrefix() {
        Message<String> sendMessage = MessageBuilder
                .withPayload("hello processLogs")
                .build();

        input.send(sendMessage, "processLogs-in-0");

        Message<byte[]> receiveMessage = output.receive(1000L, "queue.pretty.log.messages");

        assertThat(receiveMessage.getPayload())
                .asString()
                .isEqualTo("[Baeldung] - hello processLogs");
    }

    @Test
    void whenProcessingShortLogMessage_thenItsNotEnrichedWithPrefix() {
        Message<String> sendMessage = MessageBuilder.withPayload("hello")
                .setHeader("contentType", "text/plain")
                .build();

        input.send(sendMessage, "processLogs-in-0");

        Message<byte[]> receiveMessage = output.receive(1000L, "queue.pretty.log.messages");

        assertThat(receiveMessage.getPayload())
                .asString()
                .isEqualTo("hello");
    }

    @Test
    void whenHighlightingLogMessage_thenItsTransformedToUppercase() {
        Message<String> sendMessage = MessageBuilder.withPayload("hello")
                .setHeader("contentType", "text/plain")
                .build();
        input.send(sendMessage, "highlightLogs-in-0");

        Message<byte[]> receiveMessage = output.receive(1000L, "highlightLogs-out-0");
        assertThat(receiveMessage.getPayload())
                .asString()
                .isEqualTo("HELLO");
    }

}
