package com.parasoft.demo.soavirt.someip.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    RECEIVED_RESPONSE("received_response"),
    EVENT_NOTIFY("event_notify"),
    REPLIED_RESPONSE("replied_response"),
    STATUS_STARTUP("status_startup"),
    STATUS_SHUTDOWN("status_shutdown");

    private final String messageType;

    MessageType(String typeValue) {
        this.messageType = typeValue;
    }

    @JsonValue
    public String getValue() {
        return messageType;
    }

    @JsonCreator
    public static MessageType fromValue(String value) {
        for (MessageType type : values()) {
            if (type.messageType.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown messageType: " + value);
    }

    public boolean isStatusType() {
        return this == STATUS_STARTUP || this == STATUS_SHUTDOWN;
    }
}
