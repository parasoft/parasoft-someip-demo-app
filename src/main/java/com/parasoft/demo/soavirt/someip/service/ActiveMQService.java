package com.parasoft.demo.soavirt.someip.service;

import at.favre.lib.bytes.Bytes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parasoft.demo.soavirt.someip.config.ActiveMQConfig;
import com.parasoft.demo.soavirt.someip.dto.MessageType;
import com.parasoft.demo.soavirt.someip.dto.Records;
import com.parasoft.demo.soavirt.someip.utils.DeserializationUtil;
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
            simpMessagingTemplate.convertAndSend("/topic/someip_messages", processSomeipMessage(message));
        } catch (Exception e){
            log.error(e.getMessage(), e);
        }
    }

    public String processSomeipMessage(String message) throws JsonProcessingException {
        Records.ActiveMQMessage activeMQMessage = objectMapper.readValue(message, Records.ActiveMQMessage.class);

        return switch (activeMQMessage.ecuName().toUpperCase()) {
            case "GOOGLEMAP" -> message;
            case "ACC" -> deSerializeMessageFromPPDM(activeMQMessage);
            case "RADAR"-> deSerializeMessageFromRadar(activeMQMessage);
            case "TSR" -> deSerializeMessageFromTSR(activeMQMessage);
            default -> throw new RuntimeException("Unknown ecu name: " + activeMQMessage.ecuName());
        };
    }

    private String deSerializeMessageFromPPDM(Records.ActiveMQMessage activeMQMessage) throws JsonProcessingException {
        String ecuName = activeMQMessage.ecuName();
        MessageType messageType = activeMQMessage.type();

        String payload = DeserializationUtil.normalizeNativeMessage(activeMQMessage.payload());
        if (payload.length() == 8) {
            Integer maxSpeed = Bytes.parseHex(payload).toInt();
            return objectMapper.writeValueAsString(new Records.ACCMessage(ecuName, messageType, maxSpeed, null));
        } else {
            int errorCode = Bytes.parseHex(payload.substring(30).substring(0, 8)).toInt();
            String errorMessage = "Invalid payload: " + payload;
            if (errorCode == 1) {
                errorMessage = "All Services (GoogleMap, TSR, Radar) are not available";
            } else if (errorCode == 2) {
                errorMessage = "Invalid unit. Use 'kph' or 'mph'";
            }

            return objectMapper.writeValueAsString(new Records.ACCMessage(ecuName, messageType, null, errorMessage));
        }
    }

    private String deSerializeMessageFromRadar(Records.ActiveMQMessage activeMQMessage) throws JsonProcessingException {
        String ecuName = activeMQMessage.ecuName();
        MessageType messageType = activeMQMessage.type();
        int distance;
        int relativeSpeed;

        String payload = DeserializationUtil.normalizeNativeMessage(activeMQMessage.payload());
        if (payload.length() != 16) {
            String errorMessage = "Invalid payload: " + payload;
            return objectMapper.writeValueAsString(new Records.RadarMessage(ecuName, messageType, null, null, errorMessage));
        }
        distance = Bytes.parseHex(payload.substring(0, 8)).toInt();
        relativeSpeed = Bytes.parseHex(payload.substring(8, 16)).toInt();

        return objectMapper.writeValueAsString(new Records.RadarMessage(ecuName, messageType, distance, relativeSpeed, null));
    }

    private String deSerializeMessageFromTSR(Records.ActiveMQMessage activeMQMessage) throws JsonProcessingException {
        String ecuName = activeMQMessage.ecuName();
        MessageType messageType = activeMQMessage.type();

        String payload = DeserializationUtil.normalizeNativeMessage(activeMQMessage.payload());
        if (payload.length() == 8) {
            int speedLimit = Bytes.parseHex(payload).toInt();
            return objectMapper.writeValueAsString(new Records.TSRMessage(ecuName, messageType, speedLimit, null));
        } else {
            int errorCode = Bytes.parseHex(payload.substring(30).substring(0, 8)).toInt();
            String errorMessage = "Invalid payload: " + payload;
            if (errorCode == 1) {
                errorMessage = "Server denied due to permissions or other reasons";
            } else if (errorCode == 2) {
                errorMessage = "Too many connections causing the server to be busy";
            }
            return objectMapper.writeValueAsString(new Records.TSRMessage(ecuName, messageType, null, errorMessage));
        }

    }
}