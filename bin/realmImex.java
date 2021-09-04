import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

class realmImex {

    static final String HELP_CMD = "--help";

    static final String MIGRATION_REALM_OPT = "--realm";
    static final String MIGRATION_REALM_ENV = "REALM";
    static final String MIGRATION_REALM_DEFAULT = "custom";

    static final String MIGRATION_ACTION_OPT = "--action";
    static final String MIGRATION_ACTION_ENV = "ACTION";
    static final String MIGRATION_ACTION_DEFAULT = "export";

    static final String ADDITIONAL_OPTIONS_OPT = "--options";
    static final String ADDITIONAL_OPTIONS_ENV = "OPTIONS";
    static final String ADDITIONAL_OPTIONS_DEFAULT = "";

    static final String VERBOSE_CMD = "--verbose";

    public static void main(String[] args) throws IOException, InterruptedException {
        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Realm import/export for keycloak environment");
            System.out.printf("%s will support the following options as command line parameters: %n", "realmImex.java");
            System.out.println("");
            System.out.printf("Options can be set by environment-variables %s,%s and %s", MIGRATION_REALM_ENV, MIGRATION_ACTION_ENV, ADDITIONAL_OPTIONS_ENV);
            System.out.println("");
            System.out.printf("%s: %s%n", MIGRATION_REALM_OPT, "override the realm to migrate");
            System.out.printf("%s: %s%n", MIGRATION_ACTION_OPT, "override migration action: import or export");
            System.out.printf("%s: %s%n", ADDITIONAL_OPTIONS_OPT, "override the target folder to place the certificates in");
            System.out.printf("%s: %s%n", VERBOSE_CMD, "make the output of the migrate process visible on stdout");
            System.out.println("");
            System.out.printf("Example: %s=%s %s=%s %s=%s", MIGRATION_REALM_OPT, MIGRATION_REALM_DEFAULT, MIGRATION_ACTION_OPT, MIGRATION_ACTION_DEFAULT, ADDITIONAL_OPTIONS_OPT, ADDITIONAL_OPTIONS_DEFAULT);
            System.out.println("");
            System.exit(0);
        }

        var realmName = Optional.ofNullable(System.getenv(MIGRATION_REALM_ENV)).orElse(argList.stream().filter(s -> s.startsWith(MIGRATION_REALM_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(MIGRATION_REALM_DEFAULT));
        var additionalOptions = Optional.ofNullable(System.getenv(ADDITIONAL_OPTIONS_ENV)).orElse(argList.stream().filter(s -> s.startsWith(ADDITIONAL_OPTIONS_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(ADDITIONAL_OPTIONS_DEFAULT));

        var verbose = argList.contains(VERBOSE_CMD);
        var migrationAction = Optional.ofNullable(System.getenv(MIGRATION_ACTION_ENV)).orElse(argList.stream().filter(s -> s.startsWith(MIGRATION_ACTION_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(MIGRATION_ACTION_DEFAULT));

        var commandLine = new ArrayList<String>();
        commandLine.add("docker-compose");
        commandLine.add("--env-file");
        commandLine.add("keycloak.env");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose.yml");
        commandLine.add("exec");
        commandLine.add("-T");
        commandLine.add("acme-keycloak");
        commandLine.add("/opt/jboss/keycloak/bin/standalone.sh");
        commandLine.add("-c");
        commandLine.add("standalone.xml");
        commandLine.add("-Djboss.socket.binding.port-offset=10000");
        commandLine.add("-Dkeycloak.migration.action=" + migrationAction);
        commandLine.add("-Dkeycloak.migration.file=/opt/jboss/imex/" + realmName + "-realm.json");
        commandLine.add("-Dkeycloak.migration.provider=singleFile");
        commandLine.add("-Dkeycloak.migration.realmName=" + realmName);
        if (additionalOptions != null && !"".equals(additionalOptions.trim())) {
            commandLine.add(additionalOptions);
        }

        System.out.printf("Starting realm %s.%n", migrationAction);
        var pb = new ProcessBuilder(commandLine);
        pb.redirectErrorStream(true);
        var process = pb.start();
        try (var scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                if (line.contains("KC-SERVICES0034") || line.contains("KC-SERVICES0031")) {
                    System.out.println(line);
                    continue;
                }
                if (line.contains("KC-SERVICES0035") || line.contains("KC-SERVICES0032")) {
                    System.out.println(line);
                    process.destroy();
                    System.exit(0);
                }
                if (verbose) {
                    System.out.println(line);
                }
            }
            System.out.printf("Something went wrong, please check output with %s%n", VERBOSE_CMD);
            System.exit(process.waitFor());
        }
    }
}