import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class deployExtensions {

    public static void main(String[] args) throws IOException, InterruptedException {
        var deployTriggerCommand = new ArrayList<String>();
        deployTriggerCommand.add("docker-compose");
        deployTriggerCommand.add("exec");
        deployTriggerCommand.add("-T");
        deployTriggerCommand.add("acme-keycloak");
        deployTriggerCommand.add("touch");
        deployTriggerCommand.add("/opt/jboss/keycloak/standalone/deployments/extensions.jar.dodeploy");

        var pb = new ProcessBuilder(deployTriggerCommand);
        pb.directory(new File("."));
        pb.inheritIO();
        var process = pb.start();
        var deployTriggerReturnCode = process.waitFor();

        System.out.println("Deployment triggered");

        System.exit(deployTriggerReturnCode);
    }

}