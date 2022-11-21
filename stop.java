import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controller script to stop the Keycloak environment.
 *
 * <h2>Stop Keycloak</h2>
 * <pre>{@code
 *  java stop.java
 * }</pre>
 */
class start {

    static final String HELP_CMD = "help";

    public static void main(String[] args) {

        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Keycloak Environment stopper");
            System.out.println("");
            System.exit(0);
        }

        System.out.println("### Stopping Keycloak Environment");

        var commandLine = new ArrayList<String>();
        commandLine.add("docker");
        commandLine.add("compose");
        var envFile = Paths.get("generated.env.tmp");
        if (Files.exists(envFile)) {
            commandLine.add("--env-file");
            commandLine.add("generated.env.tmp");
        }
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-keycloak.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-tls.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-openldap.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-postgres.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-provisioning.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-graylog.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-prometheus.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-grafana.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-tracing.yml");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose-tracing-tls.yml");
        commandLine.add("down");
        commandLine.add("--remove-orphans");

        var pb = new ProcessBuilder(commandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        try {
            var process = pb.start();
            System.exit(process.waitFor());
        } catch (Exception ex) {
            System.err.println("Could not run docker compose down.");
            ex.printStackTrace();
            System.exit(1);
        }
    }
}