import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.util.List;

import static java.nio.file.Files.getOwner;

class envcheck {

    public static void main(String[] args) throws IOException, InterruptedException {

        var returnCode = 0;

        /* Check required tools: maven */
        var pbMaven = new ProcessBuilder(List.of("mvn", "-version"));
        pbMaven.inheritIO();
        var processMaven = pbMaven.start();
        returnCode += processMaven.waitFor();
        if (returnCode > 0) {
            System.out.println("Please install maven.");
        }

        /* Check required tools: docker compose */
        var pbDockerComposer = new ProcessBuilder(List.of("docker", "compose", "version"));
        pbDockerComposer.inheritIO();
        var processDockerComposer = pbDockerComposer.start();
        returnCode += processDockerComposer.waitFor();
        if (returnCode > 0) {
            System.out.println("Please install docker compose.");
        }

        /* Check required tools: mkcert */
        System.out.print("mkcert: ");
        var pbMkcert = new ProcessBuilder(List.of("mkcert", "-version"));
        pbMkcert.inheritIO();
        try {
            pbMkcert.start();
            var processMkcert = pbMkcert.start();
            returnCode += processMkcert.waitFor();
            if (returnCode > 0) {
                System.out.println("Please install mkcert.");
            }
        } catch (Exception e) {
            System.out.println("Please install mkcert.");
        }


        /*Check directories exist */
        var requiredDirectories = List.of("./keycloak/extensions/target/classes",
                "./keycloak/imex","./keycloak/themes/apps",
                "./deployments/local/dev/run/keycloak/data",
                "./keycloak/extensions/target/classes",
                "./keycloak/themes/internal",
                "./keycloak/config",
                "./keycloak/cli");
        requiredDirectories.forEach(requiredDirectoryString ->
        {
            var requiredDirectory = new File(requiredDirectoryString);
            if (!requiredDirectory.exists()) {
                System.out.printf("Path \"%s\" required. Please create it or build the project with maven.%n", requiredDirectoryString);
            } else {
                try {
                    var currentUser = System.getProperty("user.name");
                    var fileOwner = getOwner(requiredDirectory.toPath(), LinkOption.NOFOLLOW_LINKS).getName();
                    if (!currentUser.equals(fileOwner)) {
                        System.out.printf("Path \"%s\" has wrong owner \"%s\" required. Please adjust it to \"%s\"%n", requiredDirectoryString, fileOwner, currentUser);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        System.exit(returnCode);
    }

}