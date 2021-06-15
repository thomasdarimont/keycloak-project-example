import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controller script to start the Keycloak environment.
 *
 * <h2>Run jboss-cli in Keycloak container</h2>
 * <pre>{@code
 *  java bin/cli.java
 * }</pre>
 */
class cli {

    static final String HELP_CMD = "help";

    public static void main(String[] args) {

        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Runs jboss-cli.sh in the Keycloak container");
            System.out.printf("%n%s supports the following options: %n", "cli.java");
            System.out.println("");

            System.out.printf("%n%s supports the following commands: %n", "start.java");
            System.out.println("");
            System.out.printf("  %s: %s%n", HELP_CMD, "Shows this help message");

            System.out.printf("%n Usage examples: %n");
            System.out.println("");
            System.out.printf("  %s %s%n", "java cli.java", "# Start Keycloak jboss-cli in Environment");
            System.exit(0);
            return;
        }

        var commandLine = new ArrayList<String>();
        commandLine.add("docker-compose");
        commandLine.add("exec");
        commandLine.add("acme-keycloak");
        commandLine.add("/opt/jboss/keycloak/bin/jboss-cli.sh");
        commandLine.add("--connect");
        System.exit(runCommandAndWait(commandLine));
    }

    private static int runCommandAndWait(ArrayList<String> commandLine) {
        var pb = new ProcessBuilder(commandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        try {
            var process = pb.start();
            return process.waitFor();
        } catch (Exception ex) {
            System.err.printf("Could not run command: %s.", commandLine);
            ex.printStackTrace();
            return 1;
        }
    }
}