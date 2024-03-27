package home.vpn.bot;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Tools {
    public static String getFormattedDateTime() {
        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");
        String formattedNow = now.format(formatter);
        return formattedNow + " UTC+" + now.getOffset().getTotalSeconds() / 3600;
    }
    public static void logMessage(String message) {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("Date", getFormattedDateTime());
        try {
            JSONObject msg = new JSONObject(message);
            jsonMessage.put("Message", msg);
        }
        catch (Exception e) {
            jsonMessage.put("Message", message);
        }
        System.out.println(jsonMessage);
    }

    public static ArrayList<String> shExecute(String command) {
        ArrayList<String> result = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("/bin/sh", "-c", command);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            int exitCode = process.waitFor();
            reader.close();
            if (exitCode == 0) {
                return result;
            }
            else
                return null;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String generateConfig(String userID) {
        shExecute("openssl genrsa -out /clients/" + userID + ".key 4096");
        shExecute("openssl req -new -key /clients/" + userID + ".key -out /clients/" + userID + ".csr -config /ssl/config.cnf");
        shExecute("openssl x509 -req -in /clients/" + userID + ".csr -CA /ssl/ca.crt -CAkey /ssl/ca.key -out /clients/" + userID + ".crt -days  36500 -extfile /ssl/config.cnf -extensions v3_req");
        shExecute("cp /ssl/client.ovpn /clients/" + userID + ".ovpn");
        shExecute("sed -i 's/SERVER_PROTO/" + Environment.ovpn_protocol + "/g' /clients/" + userID + ".ovpn");
        shExecute("sed -i 's/SERVER_IP/" + Environment.ovpn_server_ip + "/g' /clients/" + userID + ".ovpn");
        shExecute("sed -i 's/SERVER_PORT/" + Environment.ovpn_port + "/g' /clients/" + userID + ".ovpn");
        shExecute("echo \"<ca>\" >> /clients/" + userID + ".ovpn");
        shExecute("cat /ssl/ca.crt >> /clients/" + userID + ".ovpn");
        shExecute("echo \"</ca>\" >> /clients/" + userID + ".ovpn");
        shExecute("echo \"<cert>\" >> /clients/" + userID + ".ovpn");
        shExecute("cat /clients/" + userID + ".crt >> /clients/" + userID + ".ovpn");
        shExecute("echo \"</cert>\" >> /clients/" + userID + ".ovpn");
        shExecute("echo \"<key>\" >> /clients/" + userID + ".ovpn");
        shExecute("cat /clients/" + userID + ".key >> /clients/" + userID + ".ovpn");
        shExecute("echo \"</key>\" >> /clients/" + userID + ".ovpn");
        shExecute("echo \"<tls-auth>\" >> /clients/" + userID + ".ovpn");
        shExecute("cat /ssl/tls.static >> /clients/" + userID + ".ovpn");
        shExecute("echo \"</tls-auth>\" >> /clients/" + userID + ".ovpn");
        shExecute("rm -f /clients/" + userID + ".key");
        shExecute("rm -f /clients/" + userID + ".csr");
        shExecute("rm -f /clients/" + userID + ".crt");

        Tools.logMessage("Config created! Client: " + userID);

        return "/clients/" + userID + ".ovpn";
    }

    public static String getMD5Hash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            BigInteger no = new BigInteger(1, digest);
            StringBuilder hash = new StringBuilder(no.toString(16));
            while (hash.length() < 32) {
                hash.insert(0, "0");
            }
            return hash.toString();
        } catch (Exception e) {
            System.out.println("ERROR:");
            System.out.println("\t" + e);
            System.exit(1);
        }
        return null;
    }
}
