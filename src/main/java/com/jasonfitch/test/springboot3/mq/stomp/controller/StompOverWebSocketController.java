package com.jasonfitch.test.springboot3.mq.stomp.controller;

import com.jasonfitch.test.springboot3.mq.stomp.entity.Greeting;
import com.jasonfitch.test.springboot3.mq.stomp.entity.HelloMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class StompOverWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(StompOverWebSocketController.class);

    /**
     * @curl "http://127.0.0.1:8080/mq/stomp/StompOverWebSocket.html"
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage helloMessage) {
        logger.info("greeting: helloMessage={}.", helloMessage);

        String name = helloMessage.getName();
        String htmlEscape = HtmlUtils.htmlEscape(name);
        Greeting greeting = new Greeting("Hello, " + htmlEscape + "!");
        return greeting;
    }

}