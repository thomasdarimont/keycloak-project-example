package demo;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.styra.opa.wasm.DefaultMappers;
import com.styra.opa.wasm.OpaPolicy;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * See: https://github.com/StyraOSS/opa-java-wasm
 */
public class OpaEmbeddedDemo {

    public static void main(String[] args) throws Exception {

        var jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        var policy = OpaPolicy.builder()
                .withMaxMemory(16) // 16 pages a 64kb
                .withJsonMapper(DefaultMappers.jsonMapper)
                .withPolicy(Paths.get(OpaEmbeddedDemo.class.getResource("../policy/policy.wasm").toURI()))
                .build();

        String data = Files.readString(Paths.get(OpaEmbeddedDemo.class.getResource("../data/user_roles.json").toURI()));
        policy.data(data);

        var requests = List.of(
                """
                        {
                            "user": "alice",
                            "action": "read",
                            "object": "id123",
                            "type": "dog"
                        }
                        """,
                """
                        {
                            "user": "bob",
                            "action": "read",
                            "object": "id123",
                            "type": "dog"
                        }
                        """);

        for (var input : requests) {
            System.out.println("#####");
            System.out.println("Input: " + input);
            String result = policy.evaluate(input);
            System.out.println("Output: " + jsonMapper.writeValueAsString(jsonMapper.readValue(result, Object.class)));
            System.out.println();
        }
    }
}
