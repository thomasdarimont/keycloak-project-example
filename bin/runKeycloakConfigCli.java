import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

class runKeycloakConfigCli {

    public static void main(String[] args) throws IOException, InterruptedException {
        var rerunProvisioning = new ArrayList<String>();
        rerunProvisioning.add("docker-compose");
        rerunProvisioning.add("restart");
        rerunProvisioning.add("acme-keycloak-provisioning");

        var pb = new ProcessBuilder(rerunProvisioning);
        pb.inheritIO();
        var process = pb.start();
        System.exit(process.waitFor());
    }
}