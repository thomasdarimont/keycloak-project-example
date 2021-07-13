package com.github.thomasdarimont.keycloak.custom.mfa.sms;

public class PhoneNumberUtils {

    public static String abbreviatePhoneNumber(String phoneNumber) {

        // +49178****123
        if (phoneNumber.length() > 6) {
            // if only show the first 6 and last 3 digits of the phone number
            return phoneNumber.substring(0, 6) + "***" + phoneNumber.replaceAll(".*(\\d{3})$", "$1");
        }

        return phoneNumber;
    }
}
