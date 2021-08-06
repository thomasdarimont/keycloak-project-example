package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.support.UserAgentParser;
import org.jboss.resteasy.spi.HttpRequest;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import ua_parser.OS;
import ua_parser.UserAgent;

import javax.ws.rs.core.HttpHeaders;

public class TrustedDeviceName {

    private static final PolicyFactory TEXT_ONLY_SANITIZATION_POLICY = new HtmlPolicyBuilder().toFactory();

    public static String generateDeviceName(HttpRequest request) {

        String userAgentString = request.getHttpHeaders().getHeaderString(HttpHeaders.USER_AGENT);
        String deviceName = "Browser";

        // TODO generate a better device name based on the user agent
        UserAgent userAgent = UserAgentParser.parseUserAgent(userAgentString);
        if (userAgent == null) {
            return deviceName;
        }
        String userAgentPart = userAgent.family;

        String osNamePart = "";
        OS os = UserAgentParser.parseOperationSystem(userAgentString);
        if (os != null) {
            osNamePart = "(" + os.family + ")";
        }

        return deviceName + " " + userAgentPart + " " + osNamePart;
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
