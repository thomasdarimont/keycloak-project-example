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

class exportRealm {

    static final String HELP_CMD = "--help";

    static final String EXPORT_REALM_OPT = "--realm";
    static final String EXPORT_REALM_ENV = "REALM";
    static final String EXPORT_REALM_DEFAULT = "custom";

    static final String ADDITIONAL_OPTIONS_OPT = "--options";
    static final String ADDITIONAL_OPTIONS_ENV = "OPTIONS";
    static final String ADDITIONAL_OPTIONS_DEFAULT = "";

    static final String VERBOSE_CMD = "--verbose";

    public static void main(String[] args) throws IOException, InterruptedException {
        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Realm exporter for keycloak environment");
            System.out.printf("%s will support the following options as command line parameters: %n", "exportRealm.java");
            System.out.println("");
            System.out.printf("Options can be set by environment-variables %s and %s", EXPORT_REALM_ENV, ADDITIONAL_OPTIONS_ENV);
            System.out.println("");
            System.out.printf("%s: %s%n", EXPORT_REALM_OPT, "override the realm to export");
            System.out.printf("%s: %s%n", ADDITIONAL_OPTIONS_OPT, "override the target folder to place the certificates in");
            System.out.printf("%s: %s%n", VERBOSE_CMD, "make the output of the export process visible on stdout");
            System.out.println("");
            System.out.printf("Example: %s=%s %s=%s", EXPORT_REALM_OPT, EXPORT_REALM_DEFAULT, ADDITIONAL_OPTIONS_OPT, ADDITIONAL_OPTIONS_DEFAULT);
            System.out.println("");
            System.exit(0);
        }

        var realmName = Optional.ofNullable(System.getenv(EXPORT_REALM_ENV)).orElse(argList.stream().filter(s -> s.startsWith(EXPORT_REALM_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(EXPORT_REALM_DEFAULT));
        var additionalOptions = Optional.ofNullable(System.getenv(ADDITIONAL_OPTIONS_ENV)).orElse(argList.stream().filter(s -> s.startsWith(ADDITIONAL_OPTIONS_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(ADDITIONAL_OPTIONS_DEFAULT));;
        var verbose = argList.contains(VERBOSE_CMD);

        var deployTriggerCommand = new ArrayList<String>();
        deployTriggerCommand.add("docker-compose");
        deployTriggerCommand.add("exec");
        deployTriggerCommand.add("-T");
        deployTriggerCommand.add("acme-keycloak");
        deployTriggerCommand.add("/opt/jboss/keycloak/bin/standalone.sh");
        deployTriggerCommand.add("-c");
        deployTriggerCommand.add("standalone.xml");
        deployTriggerCommand.add("-Djboss.socket.binding.port-offset=10000");
        deployTriggerCommand.add("-Dkeycloak.migration.action=export");
        deployTriggerCommand.add("-Dkeycloak.migration.file=/opt/jboss/imex/" + realmName + "-realm.json");
        deployTriggerCommand.add("-Dkeycloak.migration.provider=singleFile");
        deployTriggerCommand.add("-Dkeycloak.migration.realmName="+realmName);
        if(additionalOptions != null && !"".equals(additionalOptions.trim())) {
            deployTriggerCommand.add(additionalOptions);
        }

        var pb = new ProcessBuilder(deployTriggerCommand);
        pb.redirectErrorStream(true);
        var process = pb.start();
        InputStream stdIn = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(stdIn);
        BufferedReader br = new BufferedReader(isr);

        String line;
        while ((line = br.readLine ()) != null) {
            if(line.contains("KC-SERVICES0034")) {
                System.out.println(line);
                continue;
            }
            if(line.contains("KC-SERVICES0035")) {
                System.out.println(line);
                process.destroy();
                System.exit(0);
            }
            if(verbose) {
                System.out.println(line);
            }
        }

        System.out.printf("Something went wrong, please check output with %s%n", VERBOSE_CMD);
        System.exit(process.waitFor());
    }
}