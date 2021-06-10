import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controller script to start the Keycloak environment.
 *
 * <h2>Running Keycloak with http</h2>
 * <pre>{@code
 *  java start.java
 * }</pre>
 *
 * <h2>Running Keycloak with https</h2>
 * <pre>{@code
 *  java start.java --https
 * }</pre>
 *
 * <h2>Running Keycloak with https and openldap</h2>
 * <pre>{@code
 *  java start.java --https --openldap
 * }</pre>
 */
class start {

    public static void main(String[] args) {

        var argList = Arrays.asList(args);

        boolean useHttps = argList.contains("--https") || argList.contains("--https=true");
        boolean useOpenLdap = argList.contains("--openldap") || argList.contains("--openldap=true");

        System.out.println("### Starting Keycloak Environment with HTTP" + (useHttps ? "S" : ""));

        var dockerComposeCommandLine = new ArrayList<String>();
        dockerComposeCommandLine.add("docker-compose");
        dockerComposeCommandLine.add("--env-file");
        dockerComposeCommandLine.add("keycloak-common.env");
        dockerComposeCommandLine.add("--file");
        dockerComposeCommandLine.add("docker-compose.yml");

        if (useHttps) {
            dockerComposeCommandLine.add("--file");
            dockerComposeCommandLine.add("docker-compose-tls.yml");
        }

        if (useOpenLdap) {
            dockerComposeCommandLine.add("--file");
            dockerComposeCommandLine.add("docker-compose-openldap.yml");
        }

        dockerComposeCommandLine.add("up");
        dockerComposeCommandLine.add("--remove-orphans");

        var pb = new ProcessBuilder(dockerComposeCommandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        try {
            Process process = pb.start();
            process.waitFor();
        } catch (Exception ex) {
            System.err.println("Could not start docker-compose.");
            ex.printStackTrace();
            System.exit(1);
        }
    }
}