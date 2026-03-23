package com.parasoft.demo.soavirt.someip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parasoft.demo.soavirt.someip.config.ActiveMQConfig;
import com.parasoft.demo.soavirt.someip.dto.Records;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActiveMQService {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    public ActiveMQService(SimpMessagingTemplate simpMessagingTemplate, ObjectMapper objectMapper) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = ActiveMQConfig.SOMEIP_MESSAGE_QUEUE)
    public void handleMessage(String message) {
        log.info("Received message: {}", message);
        try {
            String processed = processMessage(message);
            if (processed != null) {
                simpMessagingTemplate.convertAndSend("/topic/someip_messages", processed);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public String processMessage(String message) throws JsonProcessingException {
        Records.ActiveMQMessage activeMQMessage = objectMapper.readValue(message, Records.ActiveMQMessage.class);

        if ("GOOGLEMAP".equalsIgnoreCase(activeMQMessage.ecuName())) {
            return message;
        }

        log.debug("Ignoring non-GoogleMap message from ActiveMQ: {}", activeMQMessage.ecuName());
        return null;
    }
}