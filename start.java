import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

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
 */
class start {

    static final String HELP_CMD = "--help";

    static final String CI_OPT = "--ci";
    static final String HTTP_OPT = "--http";
    static final String HTTPS_OPT = "--https";
    static final String PROVISION_OPT = "--provision";
    static final String OPENLDAP_OPT = "--openldap";
    static final String POSTGRES_OPT = "--database=postgres";
    static final String GRAYLOG_OPT = "--logging=graylog";
    static final String GRAFANA_OPT = "--grafana";
    static final String PROMETHEUS_OPT = "--metrics=prometheus";
    static final String EXTENSIONS_OPT = "--extensions=";
    static final String EXTENSIONS_OPT_CLASSES = "classes";
    static final String EXTENSIONS_OPT_JAR = "jar";
    static final String DETACH_OPT = "--detach";

    public static void main(String[] args) throws Exception {

        var argList = Arrays.asList(args);

        var useHttp = !argList.contains(HTTP_OPT + "=false"); // --http is implied by default
        var useHttps = argList.contains(HTTPS_OPT) || argList.contains(HTTPS_OPT + "=true");
        var useProvision = !argList.contains(PROVISION_OPT + "=false");
        var useOpenLdap = argList.contains(OPENLDAP_OPT) || argList.contains(OPENLDAP_OPT + "=true");
        var usePostgres = argList.contains(POSTGRES_OPT);
        var useGraylog = argList.contains(GRAYLOG_OPT);
        var useGrafana = argList.contains(GRAFANA_OPT);
        var usePrometheus = argList.contains(PROMETHEUS_OPT);
        var extension = argList.stream().filter(s -> s.startsWith(EXTENSIONS_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(EXTENSIONS_OPT_CLASSES);
        var ci = argList.stream().filter(s -> s.startsWith(CI_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(null);
        var useDetach = argList.contains(DETACH_OPT);

        var showHelp = argList.contains(HELP_CMD) || argList.isEmpty();
        if (showHelp) {
            System.out.println("Keycloak Environment starter");
            System.out.printf("%n%s supports the following options: %n", "start.java");
            System.out.println("");
            System.out.printf("  %s: %s%n", HTTP_OPT, "enables HTTP support.");
            System.out.printf("  %s: %s%n", HTTPS_OPT, "enables HTTPS support. (Optional) Implies --http. If not provided, plain HTTP is used");
            System.out.printf("  %s: %s%n", PROVISION_OPT, "enables provisioning via keycloak-config-cli.");
            System.out.printf("  %s: %s%n", OPENLDAP_OPT, "enables OpenLDAP support. (Optional)");
            System.out.printf("  %s: %s%n", POSTGRES_OPT, "enables PostgreSQL database support. (Optional) If no other database is provided, H2 database is used");
            System.out.printf("  %s: %s%n", GRAYLOG_OPT, "enables Graylog database support. (Optional)");
            System.out.printf("  %s: %s%n", EXTENSIONS_OPT, "choose dynamic extensions extension based on \"classes\" or static based on \"jar\"");
            System.out.printf("  %s: %s%n", DETACH_OPT, "Detached mode: Run containers in the background, print ew container name.. (Optional)");

            System.out.printf("%n%s supports the following commands: %n", "start.java");
            System.out.println("");
            System.out.printf("  %s: %s%n", HELP_CMD, "Shows this help message");

            System.out.printf("%n Usage examples: %n");
            System.out.println("");
            System.out.printf("  %s %s%n", "java start.java", "# Start Keycloak Environment with http");
            System.out.printf("  %s %s%n", "java start.java --https", "# Start Keycloak Environment with https");
            System.out.printf("  %s %s%n", "java start.java --provision=false", "# Start Keycloak Environment without provisioning");
            System.out.printf("  %s %s%n", "java start.java --https --database=postgres", "# Start Keycloak Environment with PostgreSQL database");
            System.out.printf("  %s %s%n", "java start.java --https --openldap --database=postgres", "# Start Keycloak Environment with PostgreSQL database and OpenLDAP");
            System.out.printf("  %s %s%n", "java start.java --extensions=classes", "# Start Keycloak with extensions mounted from classes folder. Use --extensions=jar to mount the jar file into the container");
            System.exit(0);
            return;
        }

        createFolderIfMissing("deployments/local/dev/run/keycloak/logs");
        createFolderIfMissing("deployments/local/dev/run/keycloak/data");
        createFolderIfMissing("deployments/local/dev/run/keycloak/perf");

        System.out.println("### Starting Keycloak Environment with HTTP" + (useHttps ? "S" : ""));

        System.out.printf("# Keycloak:       %s%n", useHttps ? "https://id.acme.test:8443/auth" : "http://localhost:8080/auth");
        System.out.printf("# MailHog:        %s%n", "http://localhost:1080");
        if (useOpenLdap) {
            System.out.printf("# PhpMyLdapAdmin: %s%n", "http://localhost:17080");
        }

        var envFiles = new ArrayList<String>();

        var commandLine = new ArrayList<String>();
        commandLine.add("docker-compose");
        envFiles.add("keycloak.env");
        envFiles.add("deployments/local/dev/keycloak-common.env");

        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose.yml");

        if ("github".equals(ci)) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-ci-github.yml");
        }

        if (useHttp) {
            envFiles.add("deployments/local/dev/keycloak-http.env");
        }

        if (useHttps) {
            envFiles.remove("deployments/local/dev/keycloak-http.env");

            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-tls.yml");
            envFiles.add("deployments/local/dev/keycloak-tls.env");
        }

        if (useOpenLdap) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-openldap.yml");
            envFiles.add("deployments/local/dev/keycloak-openldap.env");
        }

        if (EXTENSIONS_OPT_CLASSES.equals(extension)) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-extensions-classes.yml");
        } else if (EXTENSIONS_OPT_JAR.equals(extension)) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-extensions-jar.yml");
        } else {
            System.err.printf("Unkown extension include option %s, valid ones are %s and %s%n", extension, EXTENSIONS_OPT_CLASSES, EXTENSIONS_OPT_JAR);
            System.exit(-1);
        }

        if (usePostgres) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-postgres.yml");
            createFolderIfMissing("deployments/local/dev/run/postgres/data/");
        }

        var requiresBuild = false;

        if (useGraylog) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-graylog.yml");
            createFolderIfMissing("deployments/local/dev/run/graylog/data/mongodb");
            requiresBuild = true;
        }

        if (useGrafana) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-grafana.yml");
            createFolderIfMissing("deployments/local/dev/run/grafana");
        }

        if (usePrometheus) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-prometheus.yml");
            createFolderIfMissing("deployments/local/dev/run/prometheus");
        }
        if (useProvision) {
            commandLine.add("--file");
            commandLine.add("deployments/local/dev/docker-compose-provisioning.yml");
            envFiles.add("deployments/local/dev/keycloak-provisioning.env");
        }

        StringBuilder envVariables = new StringBuilder();
        for (String envFile : envFiles) {
            envVariables.append(Files.readString(Paths.get(envFile))).append("\n");
        }

        if (!envVariables.toString().isBlank()) {
            String generatedEnvFile = "generated.env.tmp";
            Files.writeString(Paths.get(generatedEnvFile), envVariables.toString());
            commandLine.add("--env-file");
            commandLine.add(generatedEnvFile);
        }

        commandLine.add("up");
        if (useDetach) {
            commandLine.add("--detach");
        }

        if (requiresBuild) {
            commandLine.add("--build");
        }

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