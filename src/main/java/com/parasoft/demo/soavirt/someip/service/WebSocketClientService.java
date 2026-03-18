package com.parasoft.demo.soavirt.someip.service;

import at.favre.lib.bytes.Bytes;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parasoft.demo.soavirt.someip.dto.MessageType;
import com.parasoft.demo.soavirt.someip.dto.Records;
import com.parasoft.demo.soavirt.someip.utils.DeserializationUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;

@Slf4j
@Service
public class WebSocketClientService {

    public static final String TOPIC_SOMEIP_ECU_STATUS = "/topic/someip/ecu/status";
    public static final String TOPIC_SOMEIP_ECU_MESSAGE_RECORD_ACC = "/topic/someip/ecu/message-record/acc";
    public static final String TOPIC_SOMEIP_ECU_MESSAGE_RECORD_RADAR = "/topic/someip/ecu/message-record/radar";
    public static final String TOPIC_SOMEIP_ECU_MESSAGE_RECORD_TSR = "/topic/someip/ecu/message-record/tsr";

    private static final String FRONTEND_DESTINATION = "/topic/someip_messages";

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${manager.websocket.url}")
    private String managerWebSocketUrl;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private volatile boolean connected = false;

    public WebSocketClientService(SimpMessagingTemplate simpMessagingTemplate, ObjectMapper objectMapper) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connect() {
        initializeClient();
        doConnect();
    }

    @PreDestroy
    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            log.info("Disconnected from manager WebSocket");
        }
        if (stompClient != null) {
            stompClient.stop();
        }
        connected = false;
    }

    private void initializeClient() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter jsonConverter = new MappingJackson2MessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(jsonConverter);
    }

    private void doConnect() {
        log.info("Connecting to manager WebSocket at: {}", managerWebSocketUrl);
        stompClient.connectAsync(managerWebSocketUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(@NonNull StompSession session, @NonNull StompHeaders connectedHeaders) {
                log.info("Connected to manager WebSocket successfully");
                stompSession = session;
                connected = true;
                subscribeToTopics(session);
            }

            @Override
            public void handleException(@NonNull StompSession session, StompCommand command,
                                        @NonNull StompHeaders headers, @NonNull byte[] payload,
                                        @NonNull Throwable exception) {
                log.error("WebSocket client error: {}", exception.getMessage(), exception);
            }

            @Override
            public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                log.error("WebSocket client transport error: {}", exception.getMessage(), exception);
                connected = false;
            }
        });
    }

    private void subscribeToTopics(StompSession session) {
        session.subscribe(TOPIC_SOMEIP_ECU_STATUS, new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return ECUStatusPayloadDto.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                handleStatusMessage((ECUStatusPayloadDto) payload);
            }
        });
        log.info("Subscribed to manager topic: {}", TOPIC_SOMEIP_ECU_STATUS);

        subscribeMessageRecordTopic(session, TOPIC_SOMEIP_ECU_MESSAGE_RECORD_ACC);
        subscribeMessageRecordTopic(session, TOPIC_SOMEIP_ECU_MESSAGE_RECORD_RADAR);
        subscribeMessageRecordTopic(session, TOPIC_SOMEIP_ECU_MESSAGE_RECORD_TSR);
    }

    private void subscribeMessageRecordTopic(StompSession session, String topic) {
        session.subscribe(topic, new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return SomeIpMessageRecordDto.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                handleMessageRecord(topic, (SomeIpMessageRecordDto) payload);
            }
        });
        log.info("Subscribed to manager topic: {}", topic);
    }

    private void handleMessageRecord(String topic, SomeIpMessageRecordDto messageRecord) {
        log.info("Received message record from manager [{}]: {}", topic, messageRecord);
        try {
            String result = switch (topic) {
                case TOPIC_SOMEIP_ECU_MESSAGE_RECORD_ACC -> deserializeMessageFromACC(messageRecord);
                case TOPIC_SOMEIP_ECU_MESSAGE_RECORD_RADAR -> deserializeMessageFromRadar(messageRecord);
                case TOPIC_SOMEIP_ECU_MESSAGE_RECORD_TSR -> deserializeMessageFromTSR(messageRecord);
                default -> throw new IllegalArgumentException("Unknown topic: " + topic);
            };
            simpMessagingTemplate.convertAndSend(FRONTEND_DESTINATION, result);
        } catch (Exception e) {
            log.error("Error processing message record: {}", e.getMessage(), e);
        }
    }

    private void handleStatusMessage(ECUStatusPayloadDto statusPayload) {
        log.info("Received status message from manager: {}", statusPayload);
        try {
            ECUStatusPayloadDto.EcuStatus status = statusPayload.getStatus();
            if (status == ECUStatusPayloadDto.EcuStatus.RUNNING || status == ECUStatusPayloadDto.EcuStatus.INACTIVE) {
                MessageType messageType = (status == ECUStatusPayloadDto.EcuStatus.RUNNING) ? MessageType.STATUS_STARTUP : MessageType.STATUS_SHUTDOWN;
                String result = objectMapper.writeValueAsString(new Records.StatusMessage(statusPayload.getEcuName(), messageType));
                simpMessagingTemplate.convertAndSend(FRONTEND_DESTINATION, result);
            } else {
                log.debug("Ignoring status message with unrecognized status: {}", status);
            }
        } catch (Exception e) {
            log.error("Error processing status message: {}", e.getMessage(), e);
        }
    }

    private MessageType mapToFrontendMessageType(SomeIpMessageRecordDto dto) {
        return switch (dto.operation()) {
            case RESPONSE, REQUEST, REQUEST_NO_RETURN, ERROR -> MessageType.RECEIVED_RESPONSE;
            case PUBLISH, SUBSCRIBE -> MessageType.EVENT_NOTIFY;
        };
    }

    private String deserializeMessageFromACC(SomeIpMessageRecordDto messageRecord) throws JsonProcessingException {
        String ecuName = messageRecord.ecuName();
        MessageType messageType = mapToFrontendMessageType(messageRecord);

        String payload = DeserializationUtil.normalizeNativeMessage(messageRecord.payload());
        if (payload.length() == 8) {
            Integer maxSpeed = Bytes.parseHex(payload).toInt();
            return objectMapper.writeValueAsString(new Records.ACCMessage(ecuName, messageType, maxSpeed, null));
        } else {
            String errorMessage;
            if (payload.length() == 38) {
                int errorCode = Bytes.parseHex(payload.substring(30, 38)).toInt();
                errorMessage = switch (errorCode) {
                    case 1 -> "All Services (GoogleMap, TSR, Radar) are not available";
                    case 2 -> "Invalid unit. Use 'kph' or 'mph'";
                    default -> "Invalid payload: " + payload;
                };
            } else {
                errorMessage = "Invalid payload: " + payload;
            }
            return objectMapper.writeValueAsString(new Records.ACCMessage(ecuName, messageType, null, errorMessage));
        }
    }

    private String deserializeMessageFromRadar(SomeIpMessageRecordDto messageRecord) throws JsonProcessingException {
        String ecuName = messageRecord.ecuName();
        MessageType messageType = mapToFrontendMessageType(messageRecord);

        String payload = DeserializationUtil.normalizeNativeMessage(messageRecord.payload());
        if (payload.length() != 16) {
            String errorMessage = "Invalid payload: " + payload;
            return objectMapper.writeValueAsString(new Records.RadarMessage(ecuName, messageType, null, null, errorMessage));
        }
        int relativeSpeed = Bytes.parseHex(payload.substring(0, 8)).toInt();
        int distance = Bytes.parseHex(payload.substring(8, 16)).toInt();
        return objectMapper.writeValueAsString(new Records.RadarMessage(ecuName, messageType, distance, relativeSpeed, null));
    }

    private String deserializeMessageFromTSR(SomeIpMessageRecordDto messageRecord) throws JsonProcessingException {
        String ecuName = messageRecord.ecuName();
        MessageType messageType = mapToFrontendMessageType(messageRecord);

        String payload = DeserializationUtil.normalizeNativeMessage(messageRecord.payload());
        if (payload.length() == 8) {
            int speedLimit = Bytes.parseHex(payload).toInt();
            return objectMapper.writeValueAsString(new Records.TSRMessage(ecuName, messageType, speedLimit, null));
        } else {
            String errorMessage;
            if (payload.length() == 38) {
                int errorCode = Bytes.parseHex(payload.substring(30, 38)).toInt();
                errorMessage = switch (errorCode) {
                    case 1 -> "Server denied due to permissions or other reasons";
                    case 2 -> "Too many connections causing the server to be busy";
                    default -> "Invalid payload: " + payload;
                };
            } else {
                errorMessage = "Invalid payload: " + payload;
            }
            return objectMapper.writeValueAsString(new Records.TSRMessage(ecuName, messageType, null, errorMessage));
        }
    }

    @Data
    static class ECUStatusPayloadDto {
        private String ecuName;
        private EcuStatus status;

        public enum EcuStatus {
            INACTIVE("Inactive"),
            STARTING("Starting"),
            RUNNING("Running"),
            STOPPING("Stopping");

            private final String ecuStatus;

            EcuStatus(String ecuStatus) {
                this.ecuStatus = ecuStatus;
            }

            @JsonValue
            public String getValue() {
                return ecuStatus;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SomeIpMessageRecordDto(
            @JsonProperty("ecu_name") String ecuName,
            @JsonProperty("operation") Operation operation,
            @JsonProperty("payload") String payload
    ) {
        public enum Operation {
            SUBSCRIBE,
            PUBLISH,
            REQUEST,
            REQUEST_NO_RETURN,
            RESPONSE,
            ERROR
        }
    }
}

