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

class runDefinedKeycloakConfigCli {

    static final String HELP_CMD = "--help";

    static final String IMPORT_OPT = "--import";
    static final String IMPORT_ENV = "IMPORT";
    static final String IMPORT_DEFAULT = "config/stage/dev/realms";
    static final String ENV_FILE_OPT = "--env-file";
    static final String ENV_FILE_ENV = "ENV_FILE";
    static final String ENV_FILE_DEFAULT = "bin/runDefinedKeycloakConfigCli.env";

    public static final String CONFIG_PATH_IN_CONTAINER = "/config";


    public static void main(String[] args) throws IOException, InterruptedException {
        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Execute a defined keycloak-config-cli folder or file against a running keycloak instance");
            System.out.printf("%s will support the following options as command line parameters: %n", "runDefinedKeycloakConfigCli.java");
            System.out.println("");
            System.out.printf("Options can be set by environment-variables %s and %s", IMPORT_ENV, ENV_FILE_ENV);
            System.out.println("");
            System.out.printf("%s: %s%n", IMPORT_OPT, "override file or folder (all files inside will be used) to import");
            System.out.printf("%s: %s%n", ENV_FILE_OPT, "override default env file for further options");
            System.out.println("");
            System.out.printf("Example: %s=%s %s=%s", IMPORT_OPT, IMPORT_DEFAULT, ENV_FILE_OPT, ENV_FILE_DEFAULT);
            System.out.println("");
            System.exit(0);
        }

        var configFileOrFolder = Optional.ofNullable(System.getenv(IMPORT_ENV)).orElse(argList.stream().filter(s -> s.startsWith(IMPORT_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(IMPORT_DEFAULT));
        var envFile = Optional.ofNullable(System.getenv(ENV_FILE_ENV)).orElse(argList.stream().filter(s -> s.startsWith(ENV_FILE_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(ENV_FILE_DEFAULT));

        var configFileOrFolderAsFile = Path.of(configFileOrFolder).toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
        final String pathToConfig;
        final String fileOrDirectoryNameOfConfig;
        if(configFileOrFolderAsFile.isFile()) {
            pathToConfig = configFileOrFolderAsFile.getParent();
            fileOrDirectoryNameOfConfig = CONFIG_PATH_IN_CONTAINER + "/" + configFileOrFolderAsFile.getName();
        } else {
            pathToConfig = configFileOrFolderAsFile.getPath();
            fileOrDirectoryNameOfConfig = CONFIG_PATH_IN_CONTAINER + "/";
        }

        var provisioningCommand = new ArrayList<String>();
        provisioningCommand.add("docker");
        provisioningCommand.add("run");
        provisioningCommand.add("--rm");
        provisioningCommand.add("--network");
        provisioningCommand.add("host");
        provisioningCommand.add("--env-file");
        provisioningCommand.add(envFile);
        provisioningCommand.add("-e");
        provisioningCommand.add("IMPORT_PATH=" + fileOrDirectoryNameOfConfig);
        provisioningCommand.add("-v");
        provisioningCommand.add(pathToConfig + ":" + CONFIG_PATH_IN_CONTAINER);

        provisioningCommand.add("adorsys/keycloak-config-cli:latest");

        System.out.println(provisioningCommand.toString());

        var pb = new ProcessBuilder(provisioningCommand);
        pb.inheritIO();
        var process = pb.start();
        System.exit(process.waitFor());
    }
}