import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class importCertificateIntoTruststore {

    static final String HELP_CMD = "--help";

    static final String FILE_OPT = "--file";

    static final String ALIAS_OPT = "--alias";

    static final String TRUST_STORE_OPT = "--truststore";
    static final String TRUST_STORE_PASSWORD_OPT = "--password";

    public static void main(String[] args) throws IOException, InterruptedException {

        /*
            keytool  \
              -import  \
              -file config/stage/dev/tls/acme.test+1.pem \
              -cacerts \
              -alias id.acme.test -noprompt \
              -storepass changeit
         */

        var argList = Arrays.asList(args);

        var showHelp = argList.contains(HELP_CMD);
        if (showHelp) {
            System.out.println("Imports the given certificate in the given truststore");
            System.out.printf("%s will support the following options as command line parameters: %n", "importCertIntoTruststore.java");
            System.out.println();
            System.out.printf("%s: %s%n", FILE_OPT, "Path to the certificate file");
            System.out.printf("%s: %s%n", ALIAS_OPT, "Alias for import");
            System.out.printf("%s: %s%n", TRUST_STORE_OPT, "Path to the truststore or cacerts for the JVM truststore");
            System.out.printf("%s: %s%n", TRUST_STORE_PASSWORD_OPT, "Oasswird for truststore. cacerts default password is changeit");
            System.out.println();
            System.out.println("Example: java bin/importCertificateIntoTruststore.java --file=config/stage/dev/tls/acme.test+1.pem --alias=id.acme.test --truststore=cacerts --password=changeit");
            System.exit(0);
        }

        String file = argList.stream().filter(arg -> arg.matches(FILE_OPT + "=[^ ]+"))
                .findFirst().map(arg -> arg.split("=")[1])
                .orElseThrow(() -> new IllegalArgumentException("Missing --file parameter"));

        String alias = argList.stream().filter(arg -> arg.matches(ALIAS_OPT + "=[^ ]+"))
                .findFirst().map(arg -> arg.split("=")[1])
                .orElseThrow(() -> new IllegalArgumentException("Missing --alias parameter"));

        String truststorePath = argList.stream().filter(arg -> arg.matches(TRUST_STORE_OPT + "=[^ ]+"))
                .findFirst().map(arg -> arg.split("=")[1])
                .orElseThrow(() -> new IllegalArgumentException("Missing --truststore parameter"));

        String password = argList.stream().filter(arg -> arg.matches(TRUST_STORE_PASSWORD_OPT + "=[^ ]+"))
                .findFirst().map(arg -> arg.split("=")[1])
                .orElseThrow(() -> new IllegalArgumentException("Missing --password parameter"));



        var commandLine = new ArrayList<String>();
        commandLine.add("keytool");
        commandLine.add("-import");
        commandLine.add("-file");
        commandLine.add(file); // "config/stage/dev/tls/acme.test+1.pem"
        if ("cacerts".equals(truststorePath)) {
            commandLine.add("-cacerts");
        } else {
            commandLine.add("-keystore");
            commandLine.add(truststorePath);
        }
        commandLine.add("-alias");
        commandLine.add(alias);
        commandLine.add("-storepass");
        commandLine.add(password);

        var pb = new ProcessBuilder(commandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        var process = pb.start();
        var exitCode = process.waitFor();

        System.out.println("Certificate imported");

        System.exit(exitCode);
    }
}