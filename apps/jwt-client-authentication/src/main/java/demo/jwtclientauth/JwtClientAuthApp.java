package demo.jwtclientauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@SpringBootApplication
public class JwtClientAuthApp {

    public static void main(String[] args) {
        new SpringApplicationBuilder(JwtClientAuthApp.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    CommandLineRunner cli() {
        return args -> {
            log.info("Jwt Client Authentication");

            var clientId = "acme-service-client-jwt-auth";
            var issuer = "https://id.acme.test:8443/auth/realms/acme-internal";
            var issuedAt = Instant.now();
            var tokenLifeTime = Duration.ofHours(24);

            //  generate client JWT
            String clientJwtToken = generateClientJwtToken(clientId, issuer, issuedAt, tokenLifeTime);
            log.info("Client JWT Token: {}", clientJwtToken);

            var accessTokenResponse = requestToken(issuer, clientJwtToken);
            log.info("AccessToken: {}", accessTokenResponse.get("access_token"));
        };
    }

    private Map<String, Object> requestToken(String issuer, String clientJwtToken) {

        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        requestBody.add("client_assertion", clientJwtToken);

        var tokenUrl = issuer + "/protocol/openid-connect/token";
        var responseEntity = rt.postForEntity(tokenUrl, new HttpEntity<>(requestBody, headers), Map.class);

        var accessTokenResponse = responseEntity.getBody();
        return accessTokenResponse;
    }

    private String generateClientJwtToken(String clientId, String issuer, Instant issuedAt, Duration tokenLifeTime) throws JsonProcessingException, JOSEException {

        var clientJwtPayload = Map.<String, Object>ofEntries( //
                Map.entry("iss", clientId), //
                Map.entry("sub", clientId), //
                Map.entry("aud", issuer), //
                Map.entry("iat", issuedAt.getEpochSecond()),  //
                Map.entry("exp", issuedAt.plus(tokenLifeTime).getEpochSecond()),  //
                Map.entry("jti", UUID.randomUUID().toString()) //
        );
        log.info("Payload: {}", new ObjectMapper().writeValueAsString(clientJwtPayload));

        var jwsObject = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), new Payload(clientJwtPayload));

        var signer = new RSASSASigner(readPrivateKeyFile("apps/jwt-client-authentication/client_key.pem"));
        jwsObject.sign(signer);

        var clientJwtToken = jwsObject.serialize();
        return clientJwtToken;
    }

    static RSAPrivateKey readPrivateKeyFile(String path) {

        try {
            var key = Files.readString(Path.of(path), Charset.defaultCharset());
            var privateKeyPEM = key //
                    .replace("-----BEGIN PRIVATE KEY-----", "") //
                    .replaceAll(System.lineSeparator(), "") //
                    .replace("-----END PRIVATE KEY-----", "");

            var encodedBytes = Base64.decodeBase64(privateKeyPEM);
            var keySpec = new PKCS8EncodedKeySpec(encodedBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
