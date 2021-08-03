import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

class keycloakConfigCli {

    public static final String CONFIG_PATH_IN_CONTAINER = "/config";
    static final String HELP_CMD = "--help";
    static final String IMPORT_OPT = "--import";
    static final String IMPORT_ENV = "IMPORT";
    static final String ENV_FILE_OPT = "--env-file";
    static final String ENV_FILE_ENV = "ENV_FILE";
    static final String ENV_FILE_DEFAULT = "bin/keycloakConfigCli.default.env";
    static final String KEYCLOAK_URL_OPT = "--keycloak-url";
    static final String KEYCLOAK_URL_ENV = "KEYCLOAK_FRONTEND_URL";
    static final String KEYCLOAOK_CONFIG_CLI_VERSION_OPT = "--cli-version";
    static final String KEYCLOAOK_CONFIG_CLI_VERSION_ENV = "KEYCLOAOK_CONFIG_CLI_VERSION";
    static final String KEYCLOAOK_CONFIG_CLI_VERSION_DEFAULT = "latest";

    public static void main(String[] args) throws Exception {
        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Execute a defined keycloak-config-cli folder or file against a running keycloak instance");
            System.out.printf("%s will support the following options as command line parameters: %n", "keycloakConfigCli.java");
            System.out.println("");
            System.out.printf("Options can be set by environment-variables %s, %s, %s and %s", IMPORT_ENV, ENV_FILE_ENV, KEYCLOAK_URL_ENV, KEYCLOAOK_CONFIG_CLI_VERSION_ENV);
            System.out.println("");
            System.out.printf("%s: %s%n", IMPORT_OPT, "override file or folder (all files inside will be used) to import");
            System.out.printf("%s: %s%n", ENV_FILE_OPT, "override default env file for further options");
            System.out.printf("%s: %s%n", KEYCLOAK_URL_OPT, "override default keycloak url to apply config to");
            System.out.printf("%s: %s%n", KEYCLOAOK_CONFIG_CLI_VERSION_OPT, "override default version of keycloak-config-cli");
            System.out.println("");
            System.out.printf("Example: %s=%s %s=%s %s=%s %s=%s", IMPORT_OPT, "my-config-cli.yaml", KEYCLOAK_URL_OPT, "http://localhost:8080/auth", ENV_FILE_OPT, ENV_FILE_DEFAULT, KEYCLOAOK_CONFIG_CLI_VERSION_OPT, KEYCLOAOK_CONFIG_CLI_VERSION_DEFAULT);
            System.out.println("");
            System.exit(0);
        }

        var configFileOrFolder = Optional.ofNullable(System.getenv(IMPORT_ENV)).orElse(argList.stream().filter(s -> s.startsWith(IMPORT_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElseThrow(() -> new IllegalStateException("Please provide a keycloak-config-cli file or folder to import with "+IMPORT_OPT)));
        var keycloakUrl = Optional.ofNullable(System.getenv(KEYCLOAK_URL_ENV)).orElse(argList.stream().filter(s -> s.startsWith(KEYCLOAK_URL_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElseThrow(() -> new IllegalStateException("Please provide a keycloak-url to apply import to "+KEYCLOAK_URL_OPT)));
        var envFile = Optional.ofNullable(System.getenv(ENV_FILE_ENV)).orElse(argList.stream().filter(s -> s.startsWith(ENV_FILE_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(ENV_FILE_DEFAULT));
        var keycloakConfigCliVersion = Optional.ofNullable(System.getenv(KEYCLOAOK_CONFIG_CLI_VERSION_ENV)).orElse(argList.stream().filter(s -> s.startsWith(KEYCLOAOK_CONFIG_CLI_VERSION_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(KEYCLOAOK_CONFIG_CLI_VERSION_DEFAULT));

        var configFileOrFolderAsFile = Path.of(configFileOrFolder).toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
        var pathToConfig;
        var fileOrDirectoryNameOfConfig;
        if (configFileOrFolderAsFile.isFile()) {
            pathToConfig = configFileOrFolderAsFile.getParent();
            fileOrDirectoryNameOfConfig = CONFIG_PATH_IN_CONTAINER + "/" + configFileOrFolderAsFile.getName();
        } else {
            pathToConfig = configFileOrFolderAsFile.getPath();
            fileOrDirectoryNameOfConfig = CONFIG_PATH_IN_CONTAINER + "/";
        }

        var commandLine = new ArrayList<String>();
        commandLine.add("docker");
        commandLine.add("run");
        commandLine.add("--rm");
        commandLine.add("--network");
        commandLine.add("host");
        commandLine.add("--env-file");
        commandLine.add(envFile);
        commandLine.add("-e");
        commandLine.add("IMPORT_PATH=" + fileOrDirectoryNameOfConfig);
        commandLine.add("-e");
        commandLine.add("KEYCLOAK_URL=" + keycloakUrl);
        commandLine.add("-v");
        commandLine.add(pathToConfig + ":" + CONFIG_PATH_IN_CONTAINER);

        commandLine.add("quay.io/adorsys/keycloak-config-cli:" + keycloakConfigCliVersion);

        var pb = new ProcessBuilder(commandLine);
        pb.inheritIO();
        var process = pb.start();
        System.exit(process.waitFor());
    }
}