package com.parasoft.demo.soavirt.someip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class Records {
    public record ActiveMQMessage(String ecuName, MessageType type, String payload) {}
    public record StatusMessage(String ecuName, MessageType type) {}
    public record ACCMessage(String ecuName, MessageType type, @JsonInclude(JsonInclude.Include.NON_NULL) Integer maxSpeed, @JsonInclude(JsonInclude.Include.NON_NULL) String errorMessage) {}
    public record TSRMessage(String ecuName, MessageType type, @JsonInclude(JsonInclude.Include.NON_NULL) Integer speedLimit, @JsonInclude(JsonInclude.Include.NON_NULL) String errorMessage) {}
    public record RadarMessage(String ecuName, MessageType type, @JsonInclude(JsonInclude.Include.NON_NULL) Integer distance, @JsonInclude(JsonInclude.Include.NON_NULL) Integer relativeSpeed, @JsonInclude(JsonInclude.Include.NON_NULL) String errorMessage) {}
}