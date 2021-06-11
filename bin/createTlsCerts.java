import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller script to generate valid tls certs locally.
 *
 * <h2>Generate certificates/h2>
 * <pre>{@code
 *  java createTlsCerts.java
 * }</pre>
 *
 * <h2>Generate certificates with custom domain</h2>
 * <pre>{@code
 *  java start.java --domain=mydomain
 * }</pre>
 */

class createTlsCerts {
    static final String HELP_CMD = "--help";

    static final String DOMAIN_OPT = "--domain";
    public static final String DOMAIN_ENV = "DOMAIN";
    public static final String DOMAIN_DEFAULT = "acme.test";

    static final String TARGET_DIR_OPT = "--target";
    public static final String TARGET_DIR_ENV = "TARGET_DIR";
    public static final String TARGET_DIR_DEFAULT = "./config/stage/dev/tls";
    public static final String PEM_FILE_GLOB = "glob:**/*.pem";

    public static void main(String[] args) {
        var argList = Arrays.asList(args);

        showHelpIfInstructedToAndExitOrNothing(argList);

        var tlsCertsOptions = buildMkCertOptions(argList);

        createFolderIfMissing(tlsCertsOptions.getTargetDir());
        cleanUpExistingPemFiles(tlsCertsOptions.getTargetDir());

        var tlsCertCommand = buildMkCertCommand(tlsCertsOptions);

        var commandResult = runCommand(tlsCertCommand, tlsCertsOptions.getTargetDir());

        printResultFiles(tlsCertsOptions.getTargetDir());

        System.exit(commandResult);
    }

    private static void cleanUpExistingPemFiles(String directory) {
        runCommand(List.of("rm","-f", PEM_FILE_GLOB),directory);
    }

    private static void printResultFiles(String directory) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(PEM_FILE_GLOB);
        try {
            Files.list(Paths.get(directory)).filter(pathMatcher::matches).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract fromEnvironment and/or command-line
     * @param argList
     * @return
     */
    private static Options buildMkCertOptions(List<String> argList) {
        Options options = new Options();

        options.setDomain(Optional.ofNullable(System.getenv(DOMAIN_ENV)).orElse(DOMAIN_DEFAULT));

        options.setTargetDir(Optional.ofNullable(System.getenv(TARGET_DIR_ENV)).orElse(TARGET_DIR_DEFAULT));

        return options;
    }

    private static List<String> buildMkCertCommand(Options tlsCertsOptions) {
        List<String> command = new ArrayList<String>();
        command.add("mkcert");
        command.add("-install");
        command.add(tlsCertsOptions.getDomain());
        command.add("*." + tlsCertsOptions.getDomain());
        return command;
    }

    private static void showHelpIfInstructedToAndExitOrNothing(List<String> argList) {
        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Certificates generator for keycloak environment");
            System.out.printf("%s will support the following options as command line parameters: %n", "createTlsCerts.java");
            System.out.println("");
            System.out.printf("Options can be set by environment-variables %s and %s", DOMAIN_ENV, TARGET_DIR_ENV);
            System.out.println("");
            System.out.printf("%s: %s%n", DOMAIN_OPT, "override the domain used for certificats");
            System.out.printf("%s: %s%n", TARGET_DIR_OPT, "override the target folder to place the certificates in");
            System.out.println("");
            System.out.printf("Example: %s=%s %s=%s",DOMAIN_OPT, DOMAIN_DEFAULT, TARGET_DIR_OPT, TARGET_DIR_DEFAULT);
            System.exit(0);
        }
    }

    private static void createFolderIfMissing(String directory) {
        var folder = new File(directory);
        if (!folder.exists()) {
            System.out.printf("Creating missing %s folder at %s success:%s%n"
                    , directory, folder.getAbsolutePath(), folder.mkdirs());
        }
    }

    private static int runCommand(List<String> command, String directory) {
        var pb = new ProcessBuilder(command);
        pb.directory(new File(directory));
        pb.inheritIO();
        try {
            var process = pb.start();
            return process.waitFor();
        } catch (Exception ex) {
            System.err.printf("Could not execute command %s%n",command);
            ex.printStackTrace();
            System.exit(1);
        }
        return 0;
    }
}

class Options {
    private String domain;
    private String targetDir;

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public String getDomain() {
        return domain;
    }

    public String getTargetDir() {
        return targetDir;
    }

}