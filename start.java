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

    static final String HELP_CMD = "--help";
    static final String HTTPS_OPT = "--https";
    static final String OPENLDAP_OPT = "--openldap";
    static final String POSTGRES_OPT = "--database=postgres";

    public static void main(String[] args) {

        var argList = Arrays.asList(args);

        var useHttps = argList.contains(HTTPS_OPT) || argList.contains(HTTPS_OPT + "=true");
        var useOpenLdap = argList.contains(OPENLDAP_OPT) || argList.contains(OPENLDAP_OPT + "=true");
        var usePostgres = argList.contains(POSTGRES_OPT);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Keycloak Environment starter");
            System.out.printf("%s supports the following options: %n", "start.java");
            System.out.println("");
            System.out.printf("%s: %s%n", HTTPS_OPT, "enables HTTPS support. (Optional) If not provided, plain HTTPS is used");
            System.out.printf("%s: %s%n", OPENLDAP_OPT, "enables OpenLDAP support. (Optional)");
            System.out.printf("%s: %s%n", POSTGRES_OPT, "enables postgrase database support. (Optional) If no other database is provided, H2 database is used");
            System.exit(0);
        }

        createFolderIfMissing("run/keycloak/data");

        System.out.println("### Starting Keycloak Environment with HTTP" + (useHttps ? "S" : ""));

        System.out.printf("# Keycloak:       %s%n", useHttps ? "https://id.acme.test:8443/auth" : "http://localhost:8080/auth");
        System.out.printf("# MailHog:        %s%n", "http://localhost:1080");
        if (useOpenLdap) {
            System.out.printf("# PhpMyLdapAdmin: %s%n", "http://localhost:17080");
        }

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

        if (usePostgres) {
            dockerComposeCommandLine.add("--file");
            dockerComposeCommandLine.add("docker-compose-postgres.yml");

            createFolderIfMissing("run/postgres/data/");
        }

        dockerComposeCommandLine.add("up");
        dockerComposeCommandLine.add("--remove-orphans");

        var pb = new ProcessBuilder(dockerComposeCommandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        try {
            var process = pb.start();
            System.exit(process.waitFor());
        } catch (Exception ex) {
            System.err.println("Could not start docker-compose.");
            ex.printStackTrace();
            System.exit(1);
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