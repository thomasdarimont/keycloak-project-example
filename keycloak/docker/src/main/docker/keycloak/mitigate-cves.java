import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MitigateCves {
    public static void main(String[] args) {

        System.out.println("### Begin CVE mitigation...");

        String modulesFolder = Optional.ofNullable(System.getenv("MODULES_FOLDER")).orElse("/opt/jboss/keycloak/modules");
        String baseFolder = modulesFolder + "/system/layers/base";

        List<CveFix> cveFixes = Arrays.asList(
                new CveFixReplaceLibrary(baseFolder + "/io/undertow/core/main"
                        , "undertow-core-2.2.5.Final.jar"
                        , "module.xml"
                        , "undertow-core-2.2.8.Final.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-core/2.2.8.Final/undertow-core-2.2.8.Final.jar"
                )
                , new CveFixReplaceLibrary(baseFolder + "/org/apache/commons/io/main"
                        , "commons-io-2.5.jar"
                        , "module.xml"
                        , "commons-io-2.7.jar"
                        , "https://search.maven.org/remotecontent?filepath=commons-io/commons-io/2.7/commons-io-2.7.jar"
                )
                , new CveFixReplaceLibrary(baseFolder + "/org/apache/activemq/artemis/main"
                        , "artemis-server-2.16.0.jar"
                        , "module.xml"
                        , "artemis-server-2.17.0.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/activemq/artemis-server/2.17.0/artemis-server-2.17.0.jar"
                )
                , new CveFixReplaceLibrary(baseFolder + "/org/apache/thrift/main"
                        , "libthrift-0.13.0.jar"
                        , "module.xml"
                        , "libthrift-0.14.2.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/thrift/libthrift/0.14.2/libthrift-0.14.2.jar"
                )
        );

        for (var cveFix : cveFixes) {
            boolean mitigated = cveFix.apply();
            if (mitigated) {
                System.out.println("Successfully applied: " + cveFix);
            } else {
                System.out.println("Failed to apply: " + cveFix);
            }
        }

        System.out.println("### Mitigation completed.");
    }

    static interface CveFix {
        boolean apply();
    }

    static class CveFixDeleteLibrary implements CveFix {

        final String fileToDelete;

        public CveFixDeleteLibrary(String fileToDelete) {
            this.fileToDelete = fileToDelete;
        }

        public boolean apply() {
            return fileToDelete != null && deleteFile(fileToDelete);
        }

        public String toString() {
            return "CveFixDeleteFile " + fileToDelete;
        }
    }

    static class CveFixReplaceLibrary implements CveFix {

        final String moduleFolder;
        final String vulnerableFileName;
        final String fileToPatch;
        final String newUrl;
        final String fixedFileName;
        final String fileToDelete;

        public CveFixReplaceLibrary(String moduleFolder, String vulnerableFileName, String fileToPatch, String fixedFileName, String newUrl) {
            this(moduleFolder, vulnerableFileName, moduleFolder + "/" + fileToPatch, newUrl, fixedFileName, moduleFolder + "/" + vulnerableFileName);
        }

        public CveFixReplaceLibrary(String moduleFolder, String vulnerableFileName, String fileToPatch, String newUrl, String fixedFileName, String fileToDelete) {
            this.moduleFolder = moduleFolder;
            this.vulnerableFileName = vulnerableFileName;
            this.fileToPatch = fileToPatch;
            this.newUrl = newUrl;
            this.fixedFileName = fixedFileName;
            this.fileToDelete = fileToDelete;
        }

        public boolean apply() {

            if (newUrl != null && downloadFile(newUrl, moduleFolder + "/" + fixedFileName)) {
                System.out.println("Downloaded fixed file: " + fixedFileName);
            }

            if (fileToPatch != null && replaceInFile(fileToPatch, vulnerableFileName, fixedFileName)) {
                System.out.println("Patched file: " + fileToPatch);
            }

            if (fileToDelete != null && deleteFile(fileToDelete)) {
                System.out.println("Vulnerable file deleted: " + fileToDelete);
            }

            return true;
        }

        public String toString() {
            return "CveFixReplaceLibrary: " + fileToDelete + " vulerable file:" + vulnerableFileName + " fixed file:" + fixedFileName;
        }
    }

    private static boolean deleteFile(String location) {
        System.out.printf("Delete file=%s%n", location);
        boolean deleted = new File(location).delete();
        if (deleted) {
            System.out.printf("Successfully deleted file=%s%n", location);
        }
        return deleted;
    }

    static boolean replaceInFile(String location, String search, String replace) {

        try {
            System.out.printf("Replace text in file=%s search=%s replace=%s%n", location, search, replace);
            Path path = new File(location).toPath();
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String replaced = content.replace(search, replace);
            Files.writeString(path, replaced, StandardCharsets.UTF_8);
            return true;
        } catch (Exception ex) {
            System.err.println("Could not replace file: " + location);
            return false;
        }
    }

    static boolean downloadFile(String url, String targetFile) {
        try {
            System.out.println("Downloading file: " + url);
            URL fileUrl = new URL(url);
            Files.copy(fileUrl.openStream(), new File(targetFile).toPath());
            return true;
        } catch (Exception ex) {
            System.err.println("Could not download file from url: " + url);
        }
        return false;
    }
}