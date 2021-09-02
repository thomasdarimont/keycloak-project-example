import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MitigateCves {

    public static void main(String[] args) {

        System.out.println("### Begin CVE mitigation...");

        String keycloakHomeFolder = Optional.ofNullable(System.getenv("KEYCLOAK_HOME")).orElse("/opt/jboss/keycloak");
        String baseModulesFolder = keycloakHomeFolder + "/modules/system/layers/base";

        List<CveFix> cveFixes = Arrays.asList(

                new CveFixDeleteLibrary(baseModulesFolder + "/org/hornetq"),

                new CveFixDeleteLibrary(baseModulesFolder + "/org/apache/activemq"),
                new CveFixDeleteLibrary(baseModulesFolder + "/org/wildfly/extension/messaging-activemq"),
                new CveFixReplaceInFile(baseModulesFolder + "/org/jboss/jts/main/module.xml"
                        , "<module name=\"org.apache.activemq.artemis.journal\"/>"
                        , ""
                ),

                new CveFixDeleteLibrary(keycloakHomeFolder + "/bin/client/jboss-client.jar"),

                new CveFixReplaceLibrary(baseModulesFolder + "/org/apache/sshd/main"
                        , "sshd-common-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "sshd-common-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/sshd/sshd-common/${fixedVersion}/sshd-common-${fixedVersion}.jar"
                        , "2.4.0"
                        , "2.7.0"

                ),
                new CveFixReplaceLibrary(baseModulesFolder + "/org/apache/sshd/main"
                        , "sshd-core-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "sshd-core-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/sshd/sshd-core/${fixedVersion}/sshd-core-${fixedVersion}.jar"
                        , "2.4.0"
                        , "2.7.0"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/apache/commons/io/main"
                        , "commons-io-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "commons-io-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=commons-io/commons-io/${fixedVersion}/commons-io-${fixedVersion}.jar"
                        , "2.5"
                        , "2.7"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/codehaus/jackson/jackson-core-asl/main"
                        , "jackson-core-asl-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "jackson-core-asl-${fixedVersion}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-core-asl/${fixedVersion}/jackson-core-asl-${fixedVersion}.jar"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/codehaus/jackson/jackson-jaxrs/main"
                        , "jackson-jaxrs-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "jackson-jaxrs-${fixedVersion}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-jaxrs/${fixedVersion}/jackson-jaxrs-${fixedVersion}.jar"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/codehaus/jackson/jackson-mapper-asl/main"
                        , "jackson-mapper-asl-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "jackson-mapper-asl-${fixedVersion}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-mapper-asl/${fixedVersion}/jackson-mapper-asl-${fixedVersion}.jar"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/codehaus/jackson/jackson-xc/main"
                        , "jackson-xc-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "jackson-xc-${fixedVersion}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-xc/${fixedVersion}/jackson-xc-${fixedVersion}.jar"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )


                , new CveFixReplaceLibrary(baseModulesFolder + "/org/picketlink/common/main"
                        , "picketlink-common-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "picketlink-common-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/picketlink/picketlink-common/${fixedVersion}/picketlink-common-${fixedVersion}.jar"
                        , "2.5.5.SP12-redhat-00009"
                        , "2.7.1.Final"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/picketlink/federation/main"
                        , "picketlink-federation-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "picketlink-federation-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/picketlink/picketlink-federation/${fixedVersion}/picketlink-federation-${fixedVersion}.jar"
                        , "2.5.5.SP12-redhat-00009"
                        , "2.7.1.Final"
                )


                , new CveFixReplaceLibrary(baseModulesFolder + "/io/undertow/core/main"
                        , "undertow-core-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "undertow-core-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-core/${fixedVersion}/undertow-core-${fixedVersion}.jar"
                        , "2.2.5.Final"
                        , "2.2.10.Final"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/io/undertow/websocket/main"
                        , "undertow-websockets-jsr-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "undertow-websockets-jsr-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-websockets-jsr/${fixedVersion}/undertow-websockets-jsr-${fixedVersion}.jar"
                        , "2.2.5.Final"
                        , "2.2.10.Final"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/io/undertow/servlet/main"
                        , "undertow-servlet-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "undertow-servlet-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-servlet/${fixedVersion}/undertow-servlet-${fixedVersion}.jar"
                        , "2.2.5.Final"
                        , "2.2.10.Final"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/jsoup/main"
                        , "jsoup-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "jsoup-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/jsoup/jsoup/${fixedVersion}/jsoup-${fixedVersion}.jar"
                        , "1.8.3"
                        , "1.14.2"
                )

                , new CveFixReplaceLibrary(baseModulesFolder + "/org/apache/thrift/main"
                        , "libthrift-${vulnerableVersion}.jar"
                        , "module.xml"
                        , "libthrift-${fixedVersion}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/thrift/libthrift/${fixedVersion}/libthrift-${fixedVersion}.jar"
                        , "0.13.0"
                        , "0.14.2"
                )
        );

        for (var cveFix : cveFixes) {
            System.out.println("### Applying: " + cveFix);
            Result result = cveFix.apply();
            switch (result) {
                case FIXED:
                    System.out.println("## Successfully applied: " + cveFix);
                    break;
                case FAILED:
                    System.out.println("## Failed to apply: " + cveFix);
                    break;
                case IGNORED:
                    System.out.println("## Ignored: " + cveFix);
                    break;
            }
        }

        System.out.println("### Mitigation completed.");
    }

    interface CveFix {
        Result apply();
    }

    enum Result {
        FIXED,
        IGNORED,
        FAILED
    }

    static class CveFixReplaceInFile implements CveFix {

        private final String fileToPatch;

        private final String searchRegex;

        private final String replacement;

        public CveFixReplaceInFile(String fileToPatch, String searchRegex, String replacement) {
            this.fileToPatch = fileToPatch;
            this.searchRegex = searchRegex;
            this.replacement = replacement;
        }

        @Override
        public Result apply() {

            if (fileToPatch == null) {
                return Result.IGNORED;
            }

            if (Utils.replacePatternInFile(fileToPatch, searchRegex, replacement)) {
                System.out.println("Patched file: " + fileToPatch);
                return Result.FIXED;
            }

            return Result.FAILED;
        }
    }

    static class CveFixDeleteLibrary implements CveFix {

        final String fileToDelete;

        public CveFixDeleteLibrary(String fileToDelete) {
            this.fileToDelete = fileToDelete;
        }

        public Result apply() {

            if (fileToDelete == null) {
                return Result.IGNORED;
            }

            File file = new File(fileToDelete);

            if (!file.exists()) {
                System.out.printf("File does not exist: %s%n", file);
                return Result.IGNORED;
            }

            if (file.isDirectory()) {
                return Utils.deleteFolder(file)
                        ? Result.FIXED
                        : Result.FAILED;
            }

            return Utils.deleteFile(file)
                    ? Result.FIXED
                    : Result.FAILED;
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
        final String vulnerableVersion;
        final String fixedVersion;

        public CveFixReplaceLibrary(String moduleFolder, String vulnerableFileName, String fileToPatch, String fixedFileName, String newUrl,
                                    String vulnerableVersion, String fixedVersion) {
            this(moduleFolder, vulnerableFileName, moduleFolder + "/" + fileToPatch, newUrl, fixedFileName, moduleFolder + "/" + vulnerableFileName,
                    vulnerableVersion, fixedVersion);
        }

        public CveFixReplaceLibrary(
                String moduleFolder, String vulnerableFileName, String fileToPatch, String newUrl, String fixedFileName, String fileToDelete,
                String vulnerableVersion, String fixedVersion) {
            this.moduleFolder = moduleFolder;
            this.vulnerableVersion = vulnerableVersion;
            this.fixedVersion = fixedVersion;
            this.fileToPatch = fileToPatch;
            this.newUrl = replaceVersions(newUrl);
            this.vulnerableFileName = replaceVersions(vulnerableFileName);
            this.fixedFileName = replaceVersions(fixedFileName);
            this.fileToDelete = replaceVersions(fileToDelete);
        }

        private String replaceVersions(String input) {

            if (input == null) {
                return null;
            }

            String result = input;
            if (input.contains("${vulnerableVersion}") && vulnerableVersion != null) {
                result = result.replace("${vulnerableVersion}", vulnerableVersion);
            }

            if (input.contains("${fixedVersion}") && fixedVersion != null) {
                result = result.replace("${fixedVersion}", fixedVersion);
            }

            return result;
        }

        public Result apply() {

            if (fileToDelete != null && !new File(fileToDelete).exists()) {
                System.out.println("Skipping patch, because vulnerable file does not exist: " + fileToDelete);
                return Result.IGNORED;
            }

            if (newUrl != null) {
                boolean downloaded = Utils.downloadFile(newUrl, moduleFolder + "/" + fixedFileName);
                if (downloaded) {
                    System.out.println("Downloaded fixed file: " + fixedFileName);
                } else {
                    System.out.println("Failed to download fixed file: " + fixedFileName);
                }
            }

            if (fileToPatch != null && Utils.replaceInFile(fileToPatch, vulnerableFileName, fixedFileName)) {
                System.out.println("Patched file: " + fileToPatch);
            }

            if (fileToDelete != null && Utils.deleteFile(new File(fileToDelete))) {
                System.out.println("Vulnerable file deleted: " + fileToDelete);
            }

            return Result.FIXED;
        }

        public String toString() {
            return "CveFixReplaceLibrary: " + fileToDelete + "\nVulnerable file: " + vulnerableFileName + "\nFixed file: " + fixedFileName;
        }
    }

    static class Utils {

        private static boolean deleteFile(File file) {
            System.out.printf("Delete file=%s%n", file);
            boolean deleted = file.delete();
            if (deleted) {
                System.out.printf("Successfully deleted file=%s%n", file);
            }
            return deleted;
        }

        static boolean deleteFolder(File file) {

            try {
                Files.walk(file.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(Utils::deleteFile);
                return true;
            } catch (Exception ex) {
                System.err.printf("Could not delete folder %s%n", file);
                return false;
            }

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

        static boolean replacePatternInFile(String location, String searchPattern, String replace) {

            try {
                System.out.printf("Replace text in file=%s search=%s replace=%s%n", location, searchPattern, replace);
                Path path = new File(location).toPath();
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String replaced = content.replaceAll(searchPattern, replace);
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
}