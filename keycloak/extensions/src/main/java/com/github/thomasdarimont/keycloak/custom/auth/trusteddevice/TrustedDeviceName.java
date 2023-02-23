package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.support.UserAgentParser;
import org.keycloak.http.HttpRequest;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import ua_parser.OS;
import ua_parser.UserAgent;

import javax.ws.rs.core.HttpHeaders;

public class TrustedDeviceName {

    private static final PolicyFactory TEXT_ONLY_SANITIZATION_POLICY = new HtmlPolicyBuilder().toFactory();

    public static String generateDeviceName(HttpRequest request) {

        String userAgentString = request.getHttpHeaders().getHeaderString(HttpHeaders.USER_AGENT);
        String deviceType = guessDeviceTypeFromUserAgentString(userAgentString);

        // TODO generate a better device name based on the user agent
        UserAgent userAgent = UserAgentParser.parseUserAgent(userAgentString);
        if (userAgent == null) {
            // user agent not parsable, return just device type as a fallback.
            return deviceType;
        }

        String osNamePart = guessOsFromUserAgentString(userAgentString);
        String browserFamily = userAgent.family;
        String generatedDeviceName = osNamePart + " - " + browserFamily + " " + deviceType;

        return sanitizeDeviceName(generatedDeviceName);
    }

    private static String guessOsFromUserAgentString(String userAgentString) {

        OS os = UserAgentParser.parseOperationSystem(userAgentString);
        if (os == null) {
            return "Computer";
        }
        return os.family;
    }

    private static String guessDeviceTypeFromUserAgentString(String userAgentString) {

        // see https://developer.mozilla.org/en-US/docs/Web/HTTP/Browser_detection_using_the_user_agent#mobile_tablet_or_desktop
        // best effort guess to detect mobile device type.

        if (userAgentString.contains("iPad")) {
            return "iPad";
        }

        if (userAgentString.contains("iPhone")) {
            return "iPhone";
        }

        if (userAgentString.contains("Mobi")) {
            return "Mobile Browser";
        }

        return "Browser";
    }

    public static String sanitizeDeviceName(String deviceNameInput) {

        String deviceName = deviceNameInput;

        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Browser";
        } else if (deviceName.length() > 32) {
            deviceName = deviceName.substring(0, 32);
        }

        deviceName = TEXT_ONLY_SANITIZATION_POLICY.sanitize(deviceName);
        deviceName = deviceName.trim();

        return deviceName;
    }
}
