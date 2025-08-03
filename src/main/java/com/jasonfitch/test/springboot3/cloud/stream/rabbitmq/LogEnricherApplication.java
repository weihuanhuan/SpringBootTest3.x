package com.jasonfitch.test.springboot3.cloud.stream.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Function;

/**
 * @see <a href="https://www.baeldung.com/spring-cloud-stream">Introduction to Spring Cloud Stream</a>
 */
@SpringBootApplication
public class LogEnricherApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogEnricherApplication.class, args);
    }

    @Bean
    public Function<String, String> enrichLogMessage() {
        return value -> "[%s] - %s".formatted("Baeldung", value);
    }

    @Bean
    public Function<String, Message<String>> processLogs() {
        return log -> {
            boolean shouldBeEnriched = log.length() > 10;
            String destination = shouldBeEnriched ? "enrichLogMessage-in-0" : "queue.pretty.log.messages";

            return MessageBuilder.withPayload(log)
                    .setHeader("spring.cloud.stream.sendto.destination", destination)
                    .build();
        };
    }

    @Bean
    Function<LogMessage, String> highlightLogs() {
        return logMsg -> logMsg
                .message()
                .toUpperCase();
    }

}
