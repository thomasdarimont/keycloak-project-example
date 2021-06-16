import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class deployExtensions {

    public static void main(String[] args) throws IOException, InterruptedException {
        var commandLine = new ArrayList<String>();
        commandLine.add("docker-compose");
        commandLine.add("--file");
        commandLine.add("deployments/local/dev/docker-compose.yml");
        commandLine.add("exec");
        commandLine.add("-T");
        commandLine.add("acme-keycloak");
        commandLine.add("touch");
        commandLine.add("/opt/jboss/keycloak/standalone/deployments/extensions.jar.dodeploy");

        var pb = new ProcessBuilder(commandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        var process = pb.start();
        var deployTriggerReturnCode = process.waitFor();

        System.out.println("Deployment triggered");

        System.exit(deployTriggerReturnCode);
    }

}