import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.util.List;

import static java.nio.file.Files.getOwner;

class prerequisites {

    public static void main(String[] args) throws IOException, InterruptedException {

        var returnCode = 0;

        /* Check required tools: maven */
        final var pbMaven = new ProcessBuilder(List.of("mvn","-version"));
        pbMaven.inheritIO();
        var processMaven = pbMaven.start();
        returnCode += processMaven.waitFor();
        if(returnCode > 0) {
            System.out.println("Please install maven.");
        }

        /* Check required tools: docker-compose */
        final var pbDockerComposer = new ProcessBuilder(List.of("docker-compose","-v"));
        pbDockerComposer.inheritIO();
        var processDockerComposer = pbDockerComposer.start();
        returnCode += processDockerComposer.waitFor();
        if(returnCode > 0) {
            System.out.println("Please install docker-compose.");
        }

        /*Check directories exist */
        List<String> requiredDirectories = List.of("./keycloak/extensions/target/classes");
        requiredDirectories.forEach(requiredDirectoryString ->
        {
            var requiredDirectory = new File(requiredDirectoryString);
            if(!requiredDirectory.exists()) {
                System.out.printf("Path \"%s\" required. Please create it or run the appropriate tool.%n",requiredDirectoryString);
            } else {
                try {
                    var currentUser = System.getProperty("user.name");
                    var fileOwner = getOwner(requiredDirectory.toPath(), LinkOption.NOFOLLOW_LINKS).getName();
                    if(!currentUser.equals(fileOwner)) {
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