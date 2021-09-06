import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class MitigateCves {

    static final boolean VERIFY_CHECKSUM = Boolean.getBoolean("verifyChecksum");

    static final boolean FAIL_ON_FAILED_MITIGATION = Boolean.getBoolean("failOnFailedMitigation");

    public static void main(String[] args) {

        System.out.println("### Begin CVE mitigation...");

        String keycloakHomeFolder = Optional.ofNullable(System.getenv("KEYCLOAK_HOME")).orElse("/opt/jboss/keycloak");
        String baseModulesFolder = keycloakHomeFolder + "/modules/system/layers/base";

        List<CveFix> cveFixes = Arrays.asList(

                // Remove unused hornetq modules with CVEs
                new CveFixDeleteLibrary(baseModulesFolder + "/org/hornetq"),

                // Remove unused activemq modules with CVEs
                new CveFixDeleteLibrary(baseModulesFolder + "/org/apache/activemq"),
                new CveFixDeleteLibrary(baseModulesFolder + "/org/wildfly/extension/messaging-activemq"),
                new CveFixReplaceInFile(baseModulesFolder + "/org/jboss/jts/main/module.xml"
                        , "<module name=\"org.apache.activemq.artemis.journal\"/>"
                        , ""
                ),

                // Remove unused jboss-client.jar library with CVEs
                new CveFixDeleteLibrary(keycloakHomeFolder + "/bin/client/jboss-client.jar"),

                // Replace module jars with CVEs with the latest compatible fixed version.
                new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/apache/sshd/main"
                        , "sshd-common-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/sshd/sshd-common/${fixedVersion}/sshd-common-${fixedVersion}.jar"
                        , "bbd38821c00f4b0d20271d8a4cd89336d7e7ac57458486c1c9c3798a6e4b873d"
                        , "2.4.0"
                        , "2.7.0"

                ),
                new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/apache/sshd/main"
                        , "sshd-core-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/sshd/sshd-core/${fixedVersion}/sshd-core-${fixedVersion}.jar"
                        , "2f23d666dd1fd3317891d784f324542e236d89658c02adc7c02d137aa556e636"
                        , "2.4.0"
                        , "2.7.0"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/apache/commons/io/main"
                        , "commons-io-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=commons-io/commons-io/${fixedVersion}/commons-io-${fixedVersion}.jar"
                        , "4547858fff38bbf15262d520685b184a3dce96897bc1844871f055b96e8f6e95"
                        , "2.5"
                        , "2.7"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/codehaus/jackson/jackson-core-asl/main"
                        , "jackson-core-asl-${version}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-core-asl/${fixedVersion}/jackson-core-asl-${fixedVersion}.jar"
                        , "689ab8fc802693d1780881aac982a820df2585e07ddce0d12a9854a15ba296d5"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/codehaus/jackson/jackson-jaxrs/main"
                        , "jackson-jaxrs-${version}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-jaxrs/${fixedVersion}/jackson-jaxrs-${fixedVersion}.jar"
                        , "59d11daff360654c8ce258dea35b1a2babeb7e71ebe39fa1487be22dba63627d"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/codehaus/jackson/jackson-mapper-asl/main"
                        , "jackson-mapper-asl-${version}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-mapper-asl/${fixedVersion}/jackson-mapper-asl-${fixedVersion}.jar"
                        , "d83e364a0dda7345bef76c3f17b7cf6eff6ac6bbbe31fc6ef24c05a940a0de79"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/codehaus/jackson/jackson-xc/main"
                        , "jackson-xc-${version}.jar"
                        , "https://maven.repository.redhat.com/ga/org/codehaus/jackson/jackson-xc/${fixedVersion}/jackson-xc-${fixedVersion}.jar"
                        , "8911a987be9a38dc1c7e11bae738189d30951d6b5839b04ea70b0c2d0059ee1e"
                        , "1.9.13.redhat-00007"
                        , "1.9.14.jdk17-redhat-00001"
                )


                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/picketlink/common/main"
                        , "picketlink-common-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/picketlink/picketlink-common/${fixedVersion}/picketlink-common-${fixedVersion}.jar"
                        , "f9c4cd25ef29ac571329a36957f4dafc5181a6a6522cf9923149df71934159eb"
                        , "2.5.5.SP12-redhat-00009"
                        , "2.7.1.Final"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/picketlink/federation/main"
                        , "picketlink-federation-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/picketlink/picketlink-federation/${fixedVersion}/picketlink-federation-${fixedVersion}.jar"
                        , "3a2813ea923b5913cee7a34cfa3cf59e8e25993234381025cbff6342ae0705dc"
                        , "2.5.5.SP12-redhat-00009"
                        , "2.7.1.Final"
                )


                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/io/undertow/core/main"
                        , "undertow-core-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-core/${fixedVersion}/undertow-core-${fixedVersion}.jar"
                        , "6f1dd211ce9b8d32b6b1ca2c05a43c38912b674c3bab415dd8ec9ad863f958fc"
                        , "2.2.5.Final"
                        , "2.2.10.Final"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/io/undertow/websocket/main"
                        , "undertow-websockets-jsr-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-websockets-jsr/${fixedVersion}/undertow-websockets-jsr-${fixedVersion}.jar"
                        , "e676456d3f30a3778e0e0975d48aceb5a559db87043fe4f5a022db0d189af39d"
                        , "2.2.5.Final"
                        , "2.2.10.Final"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/io/undertow/servlet/main"
                        , "undertow-servlet-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=io/undertow/undertow-servlet/${fixedVersion}/undertow-servlet-${fixedVersion}.jar"
                        , "99f41ef1cf39b4cb12e26ed598c7c0eb33558bbc2cb39faab1961d7857149ea4"
                        , "2.2.5.Final"
                        , "2.2.10.Final"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/jsoup/main"
                        , "jsoup-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/jsoup/jsoup/${fixedVersion}/jsoup-${fixedVersion}.jar"
                        , "a601ba7ce2e2c6e744d4eac793a76707c0121576170ef717ef7a81f2343ada1a"
                        , "1.8.3"
                        , "1.14.2"
                )

                , new CveFixReplaceModuleLibrary(
                        baseModulesFolder + "/org/apache/thrift/main"
                        , "libthrift-${version}.jar"
                        , "https://search.maven.org/remotecontent?filepath=org/apache/thrift/libthrift/${fixedVersion}/libthrift-${fixedVersion}.jar"
                        , "a10526fe196f6bb3614c85e6c0b5e04a99dfd1bb748b1997586b0d0e8426f159"
                        , "0.13.0"
                        , "0.14.2"
                )
        );

        int fixed = 0;
        int failed = 0;
        int ignored = 0;
        for (var cveFix : cveFixes) {
            System.out.println("### Applying: " + cveFix);
            Result result = cveFix.apply();
            switch (result) {
                case FIXED:
                    fixed++;
                    System.out.println("## Successfully applied: " + cveFix);
                    break;
                case FAILED:
                    failed++;
                    System.out.println("## Failed to apply: " + cveFix);
                    break;
                case IGNORED:
                    ignored++;
                    System.out.println("## Ignored: " + cveFix);
                    break;
            }
        }

        System.out.printf("### Mitigation completed. (fixed=%s failed=%s ignored=%s)%n", fixed, failed, ignored);

        if (failed > 0 && FAIL_ON_FAILED_MITIGATION) {
            System.exit(1);
        }
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

    static class CveFixReplaceModuleLibrary extends CveFixReplaceLibrary {

        public CveFixReplaceModuleLibrary(String moduleFolder, String fileNamePattern,
                                          String newUrl, String newFileChecksum, String vulnerableVersion, String fixedVersion) {
            super(moduleFolder
                    , "module.xml"
                    , fileNamePattern.replace("${version}", "${vulnerableVersion}")
                    , fileNamePattern.replace("${version}", "${fixedVersion}")
                    , newUrl
                    , newFileChecksum
                    , vulnerableVersion
                    , fixedVersion);

        }
    }

    static class CveFixReplaceLibrary implements CveFix {

        final String moduleFolder;
        final String vulnerableFileName;
        final String fileToPatch;
        final String newUrl;
        final String newFileChecksum;
        final String fixedFileName;
        final String fileToDelete;
        final String vulnerableVersion;
        final String fixedVersion;

        public CveFixReplaceLibrary(String moduleFolder, String fileToPatch,
                                    String vulnerableFileName, String fixedFileName, String newUrl,
                                    String newFileChecksum,
                                    String vulnerableVersion, String fixedVersion) {
            this.moduleFolder = moduleFolder;
            this.fileToPatch = moduleFolder + "/" + fileToPatch;
            this.vulnerableVersion = vulnerableVersion;
            this.fixedVersion = fixedVersion;
            this.newUrl = replaceVersions(newUrl);
            this.newFileChecksum = newFileChecksum;
            this.vulnerableFileName = replaceVersions(vulnerableFileName);
            this.fixedFileName = replaceVersions(fixedFileName);
            this.fileToDelete = replaceVersions(moduleFolder + "/" + vulnerableFileName);
        }

        private String replaceVersions(String input) {

            if (input == null) {
                return null;
            }

            var result = input;
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
                var targetFileLocation = moduleFolder + "/" + fixedFileName;
                var downloaded = Utils.downloadFile(newUrl, targetFileLocation);
                if (downloaded) {
                    System.out.println("Downloaded fixed file: " + fixedFileName);

                    try {
                        var fileChecksum = Utils.getFileChecksum(MessageDigest.getInstance("SHA-256"), new File(targetFileLocation));
                        System.out.println("Checksum: " + fileChecksum);

                        if (VERIFY_CHECKSUM) {
                            if (!newFileChecksum.equals(fileChecksum)) {
                                System.out.println("Checksum does not match!");
                                return Result.FAILED;
                            }
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
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
            return getClass().getSimpleName() + "> Replaced: " + vulnerableFileName + " with: " + fixedFileName;
        }
    }

    static class Utils {

        private static boolean deleteFile(File file) {
            System.out.printf("Delete file=%s%n", file);
            var deleted = file.delete();
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
                var path = new File(location).toPath();
                var content = Files.readString(path, StandardCharsets.UTF_8);
                var replaced = content.replace(search, replace);
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
                var path = new File(location).toPath();
                var content = Files.readString(path, StandardCharsets.UTF_8);
                var replaced = content.replaceAll(searchPattern, replace);
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
                var fileUrl = new URL(url);
                Files.copy(fileUrl.openStream(), new File(targetFile).toPath());
                return true;
            } catch (Exception ex) {
                System.err.println("Could not download file from url: " + url);
            }
            return false;
        }

        static String getFileChecksum(MessageDigest digest, File file) throws IOException {

            try (var fis = new FileInputStream(file)) {

                var byteArray = new byte[8192];
                var bytesCount = 0;

                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }

            var bytes = digest.digest();

            //Convert it to hexadecimal format
            var sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        }
    }
}