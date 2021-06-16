import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

class scanImage {
    static final String HELP_CMD = "--help";

    static final String IMAGE_NAME_OPT = "--image-name";
    static final String IMAGE_NAME_ENV = "IMAGE_NAME";

    static final String TRIVY_VERSION_OPT = "--trivy-version";
    static final String TRIVY_VERSION_ENV = "TRIVY_VERSION";
    static final String TRIVY_VERSION_DEFAULT = "0.18.3";

    static final String VERBOSE_CMD = "--verbose";

    public static void main(String[] args) throws Exception {
        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Scan image for security issues");
            System.out.printf("%s will support the following options as command line parameters: %n", "scanImage.java");
            System.out.println("");
            System.out.printf("Options can be set by environment-variables %s and %s", IMAGE_NAME_ENV, TRIVY_VERSION_ENV);
            System.out.println("");
            System.out.printf("%s: %s%n", IMAGE_NAME_OPT, "override the image to scan");
            System.out.printf("%s: %s%n", TRIVY_VERSION_OPT, "override the version of trivy use for scanning");
            System.out.printf("%s: %s%n", VERBOSE_CMD, "make the output of the export process visible on stdout");
            System.out.println("");
            System.out.printf("Example: %s=%s %s=%s", IMAGE_NAME_OPT, "<some image>", TRIVY_VERSION_OPT, TRIVY_VERSION_DEFAULT);
            System.out.println("");
            System.exit(0);
        }

        var trivyVersion = Optional.ofNullable(System.getenv(TRIVY_VERSION_ENV)).orElse(argList.stream().filter(s -> s.startsWith(TRIVY_VERSION_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElse(TRIVY_VERSION_DEFAULT));
        var imageName = Optional.ofNullable(System.getenv(IMAGE_NAME_ENV)).orElse(argList.stream().filter(s -> s.startsWith(IMAGE_NAME_OPT)).map(s -> s.substring(s.indexOf("=") + 1)).findFirst().orElseThrow(() -> new IllegalStateException("Please provide image name to scan with "+IMAGE_NAME_OPT)));

        var scanImageCommand = new ArrayList<String>();
        scanImageCommand.add("docker");
        scanImageCommand.add("run");
        scanImageCommand.add("--privileged");
        scanImageCommand.add("--rm");
        scanImageCommand.add("-v");
        scanImageCommand.add("/var/run/docker.sock:/var/run/docker.sock:z");
        scanImageCommand.add("aquasec/trivy:" + trivyVersion);
        scanImageCommand.add(imageName);

        var pb = new ProcessBuilder(scanImageCommand);
        pb.inheritIO();
        var process = pb.start();
        System.exit(process.waitFor());
    }
}