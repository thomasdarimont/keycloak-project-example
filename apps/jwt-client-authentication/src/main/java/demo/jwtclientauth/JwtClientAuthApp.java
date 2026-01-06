package demo.jwtclientauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
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
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Example for client authentication via private_key_jwt https://oauth.net/private-key-jwt/
 */
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
            var tokenLifeTime = Duration.ofSeconds(5);

            var clientJwtPayload = Map.<String, Object>ofEntries( //
                    Map.entry("iss", clientId), //
                    Map.entry("sub", clientId), //
                    Map.entry("aud", issuer), // see: aud in private_key_jwt in https://openid.net/specs/openid-connect-core-1_0-36.html#rfc.section.9
                    Map.entry("iat", issuedAt.getEpochSecond()),  //
                    Map.entry("exp", issuedAt.plus(tokenLifeTime).getEpochSecond()),  //
                    Map.entry("jti", UUID.randomUUID().toString()) //
            );

            { // Signed JWT example
                //  generate Signed JWT
                var clientJwtToken = generateClientAssertionSignedWithPrivateKey(clientJwtPayload, //
                        "apps/jwt-client-authentication/client_cert.pem", //
                        "apps/jwt-client-authentication/client_key.pem" //
                );
                log.info("Client JWT Token: {}", clientJwtToken);

                // use clientjwt to request token for service
                var accessTokenResponse = requestToken(issuer, clientJwtToken);
                log.info("AccessToken: {}", accessTokenResponse.get("access_token"));

                // use clientjwt perform PAR request
//                var requestUri = requestPAR(issuer, clientId, UUID.randomUUID().toString(), "https://www.keycloak.org/app/", "openid profile", clientJwtToken);
//                log.info("RequestUri: {}", requestUri);
            }

            { // Signed JWT with Client Secret example
                //  generate Signed JWT with client secret
//                String clientSecret = "8FKyMMDOiBp2CIdu4TtssY6HRP5nHRsI";
//                var clientJwtToken = generateTokenSignedWithClientSecret(clientJwtPayload, clientSecret,
//                "apps/jwt-client-authentication/client_cert.pem");
//                log.info("Client JWT Token: {}", clientJwtToken);

                // use Signed JWT with client secret to request token for service
//                var accessTokenResponse = requestToken(issuer, clientJwtToken);
//                log.info("AccessToken: {}", accessTokenResponse.get("access_token"));

                 // use clientjwt perform PAR request
//                var requestUri = requestPAR(issuer, clientId, UUID.randomUUID().toString(), "https://www.keycloak.org/app/", "openid profile", clientJwtToken);
//                log.info("RequestUri: {}", requestUri);
            }
        };
    }

    private Map<String, Object> requestToken(String issuer, String clientAssertion) {

        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("grant_type", "client_credentials");
        requestBody.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        requestBody.add("client_assertion", clientAssertion);

        var tokenUrl = issuer + "/protocol/openid-connect/token";
        var responseEntity = rt.postForEntity(tokenUrl, new HttpEntity<>(requestBody, headers), Map.class);

        var accessTokenResponse = responseEntity.getBody();
        return accessTokenResponse;
    }

    private String requestPAR(String issuer, String clientId, String nonce, String redirectUri, String scope, String clientJwtToken) {

        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("response_type", "code");
        requestBody.add("client_id", clientId);
        requestBody.add("nonce", nonce);
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("scope", scope);
        requestBody.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        requestBody.add("client_assertion", clientJwtToken);

        var tokenUrl = issuer + "/protocol/openid-connect/ext/par/request";
        var responseEntity = rt.postForEntity(tokenUrl, new HttpEntity<>(requestBody, headers), Map.class);

        var parResponse = responseEntity.getBody();
        return String.valueOf(parResponse.get("request_uri"));
    }

    private String generateClientAssertionSignedWithPrivateKey(Map<String, Object> clientJwtPayload, String certLocation, String keyLocation) {

        try {
            // x5t header
            log.info("Payload: {}", new ObjectMapper().writeValueAsString(clientJwtPayload));

            var cert = parseCertificate(certLocation);
            var privateKey = readPrivateKeyFile(keyLocation);
            var base64URL = createKeyThumbprint(cert, "SHA-1");

            var jwsObject = new JWSObject(new JWSHeader
                    .Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
//                    .keyID("mykey") // explicit kid
                    // .x509CertThumbprint(base64URL) // SHA-1
                    .x509CertSHA256Thumbprint(base64URL) // SHA256
                    .build(), new Payload(clientJwtPayload));

            var signer = new RSASSASigner(privateKey);
            jwsObject.sign(signer);

            var clientAssertion = jwsObject.serialize();
            return clientAssertion;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateClientAssertionSignedWithClientSecret(Map<String, Object> clientJwtPayload, String clientSecret, String certLocation) {

        var cert = parseCertificate(certLocation);
        var base64URL = createKeyThumbprint(cert, "SHA-1");

        var jwsObject = new JWSObject(new JWSHeader
                .Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                // .x509CertThumbprint(base64URL) // SHA-1
                .x509CertSHA256Thumbprint(base64URL) // SHA256
                .build(), new Payload(clientJwtPayload));

        try {
            var signer = new MACSigner(clientSecret);
            jwsObject.sign(signer);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

        var clientAssertion = jwsObject.serialize();
        return clientAssertion;
    }

    private String generateClientSignedJwtToken(String clientId, String issuer, Instant issuedAt, Duration tokenLifeTime, Function<Map<String, Object>, String> jwtGenerator) throws JsonProcessingException, JOSEException {

        var clientJwtPayload = Map.<String, Object>ofEntries( //
                Map.entry("iss", clientId), //
                Map.entry("sub", clientId), //
                Map.entry("aud", issuer), //
                Map.entry("iat", issuedAt.getEpochSecond()),  //
                Map.entry("exp", issuedAt.plus(tokenLifeTime).getEpochSecond()),  //
                Map.entry("jti", UUID.randomUUID().toString()) //
        );

        return jwtGenerator.apply(clientJwtPayload);
    }

    private X509Certificate parseCertificate(String path) {
        try {
            var cert = X509CertUtils.parse(Files.readString(Path.of(path), Charset.defaultCharset()));
            return cert;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Base64URL createKeyThumbprint(X509Certificate cert, String hashAlgorithm) {
        try {
            RSAKey rsaKey = RSAKey.parse(cert);
            return rsaKey.computeThumbprint(hashAlgorithm);
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
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
