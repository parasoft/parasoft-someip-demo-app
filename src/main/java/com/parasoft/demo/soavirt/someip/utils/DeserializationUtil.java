package com.parasoft.demo.soavirt.someip.utils;

import org.apache.commons.lang3.Strings;

public class DeserializationUtil {
    private static final String HEX_MESSAGE_PREFIX = "0x";

    public static String normalizeNativeMessage(String nativeMessage) {
        String normalizedHexString = nativeMessage.toLowerCase();
        if (normalizedHexString.toLowerCase().startsWith(HEX_MESSAGE_PREFIX)) {
            normalizedHexString = Strings.CI.removeStart(nativeMessage, HEX_MESSAGE_PREFIX);
        }
        return normalizedHexString;
    }
}