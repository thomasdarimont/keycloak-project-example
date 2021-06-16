import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

/**
 * Controller script to generate valid tls certs locally.
 *
 * <h2>Generate certificates/h2>
 * <pre>{@code
 *  java createTlsCerts.java
 * }</pre>
 *
 * <h2>Show help</h2>
 * <pre>{@code
 *  java createTlsCerts.java --help
 * }</pre>
 * <p>
 * Hint:
 */

class createTlsCerts {

    static final String HELP_CMD = "--help";

    static final String DOMAIN_OPT = "--domain";
    static final String DOMAIN_ENV = "DOMAIN";
    static final String DOMAIN_DEFAULT = "acme.test";

    static final String TARGET_DIR_OPT = "--target";
    static final String TARGET_DIR_ENV = "TARGET_DIR";
    static final String TARGET_DIR_DEFAULT = "./config/stage/dev/tls";

    static final String PEM_FILE_GLOB = "glob:**/*.pem";

    public static void main(String[] args) throws IOException, InterruptedException {
        var argList = Arrays.asList(args);

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
            System.out.printf("Example: %s=%s %s=%s", DOMAIN_OPT, DOMAIN_DEFAULT, TARGET_DIR_OPT, TARGET_DIR_DEFAULT);
            System.out.println("");
            System.exit(0);
        }

        /* Set options from env, commandline or default */
        var domain = Optional.ofNullable(System.getenv(DOMAIN_ENV)).orElse(argList.stream().filter(s -> s.startsWith(DOMAIN_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(DOMAIN_DEFAULT));
        var targetDir = Optional.ofNullable(System.getenv(TARGET_DIR_ENV)).orElse(argList.stream().filter(s -> s.startsWith(TARGET_DIR_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(TARGET_DIR_DEFAULT));

        /* Assure required folder exists */
        var folder = new File(targetDir);
        if (!folder.exists()) {
            System.out.printf("Creating missing %s folder at %s success:%s%n"
                    , targetDir, folder.getAbsolutePath(), folder.mkdirs());
        }
        /* Delete existing cert-files */
        Files.list(Paths.get(targetDir)).filter(p -> FileSystems.getDefault().getPathMatcher(PEM_FILE_GLOB).matches(p)).forEach(f -> f.toFile().delete());

        /* Create mkcert command */
        var command = new ArrayList<String>();
        command.add("mkcert");
        command.add("-install");
        command.add(domain);
        command.add("*." + domain);

        /* Execute mkcert command */
        final var pb = new ProcessBuilder(command);
        pb.directory(new File(targetDir));
        pb.inheritIO();
        var process = pb.start();
        var mkCertCommandsReturnCode = process.waitFor();

        /* List created files */
        Files.list(Paths.get(targetDir)).filter(p -> FileSystems.getDefault().getPathMatcher(PEM_FILE_GLOB).matches(p)).forEach(System.out::println);

        System.exit(mkCertCommandsReturnCode);
    }
}
