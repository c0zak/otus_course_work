package home.vpn.bot;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class LogCollector {
    private Config kubeConfig;
    private KubernetesClient client;
    private LogWatch shadowsocks;
    private LogWatch vpnBot;

    private Set<Integer> ports;
    private Integer configsSent;

    public LogCollector() {
        try {
            ports = new HashSet<>();
            configsSent = 0;
            ArrayList<String> test = Tools.shExecute("echo $HOME");
            String path = "";
            assert test != null;
            for (String s : test) {
                path = s;
            }
            this.kubeConfig = Config.fromKubeconfig(Files.readString(Path.of(path + "/.kube/config")));
            client = new DefaultKubernetesClient(kubeConfig);
        } catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }
    }

    public void startCollect() {
        List<Pod> pods = client.pods().inNamespace("vpn").list().getItems();


        for (Pod pod : pods) {
            if (pod.getMetadata().getName().contains("shadowsocks"))
                shadowsocks = client.pods().inNamespace("vpn").withName(pod.getMetadata().getName()).watchLog();

            if (pod.getMetadata().getName().contains("vpn-bot"))
                vpnBot = client.pods().inNamespace("vpn").withName(pod.getMetadata().getName()).watchLog();

        }

        new Thread(() -> this.parseShadowsocksLogs(shadowsocks)).start();
        new Thread(() -> this.parseVpnBotLogs(vpnBot)).start();
    }

    private void parseShadowsocksLogs(LogWatch shadowsocks) {
        try {
            BufferedReader logReader = new BufferedReader(new InputStreamReader(shadowsocks.getOutput()));
            String line;
            while ((line = logReader.readLine()) != null) {
                if (line.contains("peer:")) {
                    line = line.replaceAll(".*:", "");
                    ports.add(Integer.valueOf(line.replaceAll("\s|\t", "")));
                }
                if (line.contains("closed")) {
                    line = line.replaceAll(".*tunnel ", "");
                    line = line.replaceAll(" <->.*", "");
                    line = line.replaceAll(".*:", "");
                    ports.remove(Integer.valueOf(line.replaceAll("\s|\t", "")));
                }
            }
        } catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }
    }

    private void parseVpnBotLogs(LogWatch vpnBot) {
        try {
            BufferedReader logReader = new BufferedReader(new InputStreamReader(vpnBot.getOutput()));
            String line;
            while ((line = logReader.readLine()) != null) {
                if (line.contains("Config sent to "))
                    configsSent++;
            }
        } catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }
    }


    public void initServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/", new request());
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }
    }

    class request implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            String path = exchange.getRequestURI().getPath();
            if (path.contains("metrics")) {
                String metrics = "connects_count " + ports.size() + "\nconfigs_sent_count " + configsSent;
                sentResponse(exchange, metrics);
            }
        }
    }

    private void sentResponse(HttpExchange exchange, String response) {
        try {
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception ignored) {}
    }
}
