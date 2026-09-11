package com.github.thomasdarimont.keycloak.custom.security.password.hibp;

import com.github.thomasdarimont.keycloak.custom.security.password.hibp.HibpPasswordPolicyProvider.HibpClient;
import com.github.thomasdarimont.keycloak.custom.security.password.hibp.HibpPasswordPolicyProvider.HibpConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HibpClientTest {

    // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
    static final String PASSWORD_HASH_PREFIX = "5BAA6";

    static final String PASSWORD_HASH_SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    @Test
    public void sha1HexShouldBeUppercase() {
        assertThat(HibpClient.sha1Hex("password")).isEqualTo(PASSWORD_HASH_PREFIX + PASSWORD_HASH_SUFFIX);
    }

    @Test
    public void parseRangeShouldHandleCrlfPaddingAndMalformedLines() {

        String body = "0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n" //
                + PASSWORD_HASH_SUFFIX.toLowerCase() + ":9,545,824\r\n" //
                + "00D4F6E8FA6EECAD2A3AA415EEC418D38EC:0\r\n" // padded entry
                + "malformed-line\r\n" //
                + "011053FD0102E94D6AE2F8B83D76FAF94F6:not-a-number\r\n" //
                + "\r\n";

        Map<String, Integer> range = HibpClient.parseRange(body);

        assertThat(range).containsOnly( //
                Map.entry("0018A45C4D1DEF81644B54AB7F969B88D65", 1), //
                Map.entry(PASSWORD_HASH_SUFFIX, 9545824));
    }

    @Test
    public void parseRangeShouldReturnEmptyMapForEmptyBody() {
        assertThat(HibpClient.parseRange(null)).isEmpty();
        assertThat(HibpClient.parseRange("")).isEmpty();
    }

    @Test
    public void lookupShouldQueryPrefixAndMatchSuffix() throws IOException {

        var client = new HibpClient(null, HibpConfig.defaults()) {
            @Override
            protected Map<String, Integer> fetchRange(String prefix) {
                assertThat(prefix).isEqualTo(PASSWORD_HASH_PREFIX);
                return Map.of(PASSWORD_HASH_SUFFIX, 42);
            }
        };

        assertThat(client.lookupBreachCount("password")).isEqualTo(42);
    }

    @Test
    public void lookupShouldReturnZeroForUnknownSuffix() throws IOException {

        var client = new HibpClient(null, HibpConfig.defaults()) {
            @Override
            protected Map<String, Integer> fetchRange(String prefix) {
                return Map.of("0018A45C4D1DEF81644B54AB7F969B88D65", 1);
            }
        };

        assertThat(client.lookupBreachCount("password")).isZero();
    }
}
