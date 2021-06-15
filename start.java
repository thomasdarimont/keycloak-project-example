import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controller script to start the Keycloak environment.
 *
 * <h2>Run Keycloak with http</h2>
 * <pre>{@code
 *  java start.java
 * }</pre>
 *
 * <h2>Run Keycloak with https</h2>
 * <pre>{@code
 *  java start.java --https
 * }</pre>
 *
 * <h2>Run Keycloak with https and openldap</h2>
 * <pre>{@code
 *  java start.java --https --openldap
 * }</pre>
 *
 * <h2>Run Keycloak with https, openldap and postgres database</h2>
 * <pre>{@code
 *  java start.java --https --openldap --database=postgres
 * }</pre>
 *
 */
class start {

    static final String HELP_CMD = "help";

    static final String HTTP_OPT = "--http";
    static final String HTTPS_OPT = "--https";
    static final String OPENLDAP_OPT = "--openldap";
    static final String POSTGRES_OPT = "--database=postgres";

    public static void main(String[] args) {

        var argList = Arrays.asList(args);

        var useHttp = !argList.contains(HTTP_OPT + "=false"); // --http is implied by default
        var useHttps = argList.contains(HTTPS_OPT) || argList.contains(HTTPS_OPT + "=true");
        var useOpenLdap = argList.contains(OPENLDAP_OPT) || argList.contains(OPENLDAP_OPT + "=true");
        var usePostgres = argList.contains(POSTGRES_OPT);

        var showHelp = argList.contains(HELP_CMD) || argList.isEmpty();
        if (showHelp) {
            System.out.println("Keycloak Environment starter");
            System.out.printf("%n%s supports the following options: %n", "start.java");
            System.out.println("");
            System.out.printf("  %s: %s%n", HTTP_OPT, "enables HTTP support.");
            System.out.printf("  %s: %s%n", HTTPS_OPT, "enables HTTPS support. (Optional) Implies --http. If not provided, plain HTTP is used");
            System.out.printf("  %s: %s%n", OPENLDAP_OPT, "enables OpenLDAP support. (Optional)");
            System.out.printf("  %s: %s%n", POSTGRES_OPT, "enables postgrase database support. (Optional) If no other database is provided, H2 database is used");

            System.out.printf("%n%s supports the following commands: %n", "start.java");
            System.out.println("");
            System.out.printf("  %s: %s%n", HELP_CMD, "Shows this help message");

            System.out.printf("%n Usage examples: %n");
            System.out.println("");
            System.out.printf("  %s %s%n", "java start.java", "# Start Keycloak Environment with http");
            System.out.printf("  %s %s%n", "java start.java --https", "# Start Keycloak Environment with https");
            System.out.printf("  %s %s%n", "java start.java --https --database=postgres", "# Start Keycloak Environment with PostgreSQL database");
            System.out.printf("  %s %s%n", "java start.java --https --openldap --database=postgres", "# Start Keycloak Environment with PostgreSQL database and OpenLDAP");
            System.exit(0);
            return;
        }

        createFolderIfMissing("run/keycloak/data");

        System.out.println("### Starting Keycloak Environment with HTTP" + (useHttps ? "S" : ""));

        System.out.printf("# Keycloak:       %s%n", useHttps ? "https://id.acme.test:8443/auth" : "http://localhost:8080/auth");
        System.out.printf("# MailHog:        %s%n", "http://localhost:1080");
        if (useOpenLdap) {
            System.out.printf("# PhpMyLdapAdmin: %s%n", "http://localhost:17080");
        }

        var commandLine = new ArrayList<String>();
        commandLine.add("docker-compose");
        commandLine.add("--env-file");
        commandLine.add("keycloak-common.env");
        commandLine.add("--file");
        commandLine.add("docker-compose.yml");

        if (useHttps) {
            commandLine.add("--file");
            commandLine.add("docker-compose-tls.yml");
        }

        if (useOpenLdap) {
            commandLine.add("--file");
            commandLine.add("docker-compose-openldap.yml");
        }

        if (usePostgres) {
            commandLine.add("--file");
            commandLine.add("docker-compose-postgres.yml");

            createFolderIfMissing("run/postgres/data/");
        }

        commandLine.add("up");
        commandLine.add("--remove-orphans");

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

    private static void createFolderIfMissing(String folderPath) {
        var folder = new File(folderPath);
        if (!folder.exists()) {
            System.out.printf("Creating missing %s folder at %s success:%s%n"
                    , folderPath, folder.getAbsolutePath(), folder.mkdirs());
        }
    }
}