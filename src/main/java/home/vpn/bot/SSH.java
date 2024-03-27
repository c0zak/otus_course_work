package home.vpn.bot;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SSH {
    private String user;
    private String password;
    private String host;
    private int port;
    private Session session;
    private int attempts;

    public SSH(String user, String password, String host, int port) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;
        attempts = 0;
        connect();
    }


    private void connect() {
        JSch jsch = new JSch();
        try {
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("PreferredAuthentications", "password");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        }
        catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }
    }

    public ArrayList<String> executeCommand(String command) {
        try {
            if (!session.isConnected())
                connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            InputStream input = channel.getInputStream();
            channel.connect();

            BufferedReader cReader = new BufferedReader(new InputStreamReader(input));
            ArrayList<String> result = new ArrayList<>();
            String line;
            while ((line = cReader.readLine()) != null) {
                result.add(line);
            }
            channel.disconnect();
            attempts = 0;
            return result;
        }
        catch (Exception e) {
            if (attempts > 20) {
                Tools.logMessage("Can't execute command after 20 attempts!\nCommand: \"" + command + "\"\nError: " + e);
                System.exit(1);
            }
            attempts++;
            return executeCommand(command);
        }
    }

}