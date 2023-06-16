import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SSHJExample {

    public static void main(String[] args) {
        SSHClient ssh = new SSHClient();
        try {
            // Use a PromiscuousVerifier to trust any host key
            HostKeyVerifier promiscuousVerifier = new PromiscuousVerifier();

            // Configure the SSH client with the PromiscuousVerifier
            ssh.addHostKeyVerifier(promiscuousVerifier);

            // Connect to the remote server
            ssh.connect("54.211.60.151");
          // Specify the private key for authentication
            KeyProvider keyProvider = ssh.loadKeys(new File("/path/to/your/private_key"), null, null);

            // Authenticate using the private key
            ssh.authPublickey("ubuntu", keyProvider);

            // Create a session
            Session session = ssh.startSession();

            try {
                // Execute a command on the remote server
                Session.Command command = session.exec("ls -la");

                // Wait for the command to complete
                command.join(5, TimeUnit.SECONDS);

                // Get the command output
                String output = IOUtils.readFully(command.getInputStream()).toString();

                System.out.println("Command output:");
                System.out.println(output);
            } finally {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

       
    }
}
