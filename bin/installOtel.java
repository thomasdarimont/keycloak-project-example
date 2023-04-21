import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class installOtel {

    public static void main(String[] args) throws Exception {

        var otelFilePath = Paths.get("bin/opentelemetry-javaagent.jar");
        var otelAlreadyPresent = Files.exists(otelFilePath);
        if (otelAlreadyPresent) {
            System.out.println("OpenTelemetry javaagent already installed at " + otelFilePath);
            System.exit(0);
            return;
        }

        var otelVersion = args.length == 0 ? "v1.25.0" : args[0];
        System.out.println("Downloading OpenTelemetry javaagent version " + otelVersion);
        downloadFile("https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/" + otelVersion + "/opentelemetry-javaagent.jar", otelFilePath);
        System.out.println("OpenTelemetry javaagent saved to " + otelFilePath);
    }

    public static void downloadFile(String url, Path filePath) {
        try {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage(), e);
        }
    }
}
