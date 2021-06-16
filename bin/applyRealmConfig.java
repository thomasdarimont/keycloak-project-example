import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

class runKeycloakConfigCli {

    public static void main(String[] args) throws IOException, InterruptedException {
        var commandLine = new ArrayList<String>();
        commandLine.add("docker-compose");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose.yml");
        commandLine.add("restart");
        commandLine.add("acme-keycloak-provisioning");

        var pb = new ProcessBuilder(rerunProvisioning);
        pb.inheritIO();
        var process = pb.start();
        System.exit(process.waitFor());
    }
}