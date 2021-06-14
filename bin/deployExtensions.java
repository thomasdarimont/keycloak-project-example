import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class deployExtensions {

    public static void main(String[] args) throws IOException, InterruptedException {
        var dockerComposeCommandLine = new ArrayList<String>();
        dockerComposeCommandLine.add("docker-compose");
        dockerComposeCommandLine.add("exec");
        dockerComposeCommandLine.add("-T");
        dockerComposeCommandLine.add("acme-keycloak");
        dockerComposeCommandLine.add("touch");
        dockerComposeCommandLine.add("/opt/jboss/keycloak/standalone/deployments/extensions.jar.dodeploy");

        var pb = new ProcessBuilder(dockerComposeCommandLine);
        pb.directory(new File("."));
        pb.inheritIO();
        var process = pb.start();
        var deployTriggerReturnCode = process.waitFor();
        System.out.println("Deployment triggered");

        System.exit(deployTriggerReturnCode);
    }

}