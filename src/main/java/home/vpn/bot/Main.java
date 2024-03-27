package home.vpn.bot;

import org.json.JSONObject;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.BufferedReader;
import java.io.FileReader;

public class Main {
    public static void main(String[] args) {

        JSONObject jConfig = null;
        try {
            BufferedReader fReader = new BufferedReader(new FileReader("/config/vpn.json"));
            String line;
            StringBuilder config = new StringBuilder();
            while ((line = fReader.readLine()) != null)
                config.append(line);
            jConfig = new JSONObject(config.toString());
            fReader.close();
        } catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }

        Environment.router_hostname = jConfig.getString("router_IP");
        Environment.router_port = jConfig.getInt("router_PORT");
        Environment.router_username = jConfig.getString("router_USER");
        Environment.router_password = jConfig.getString("router_PASSWOD");
        Environment.routeAddCommand = jConfig.getString("template_ADD_ROUTE");
        Environment.routeDelCommand = jConfig.getString("template_DELETE_ROUTE");
        Environment.routeShowCommand = jConfig.getString("template_SHOW_ROUTES");
        Environment.routeShowParseFlag = jConfig.getString("template_SHOW_ROUTES_PARSE_FLAG");
        Environment.ovpn_server_ip = jConfig.getString("openvpn_server_ip");
        Environment.ovpn_port = jConfig.getString("openvpn_port");
        Environment.ovpn_protocol = jConfig.getString("openvpn_protocol");
        Environment.database_address = jConfig.getString("database_address");
        Environment.database_port = jConfig.getInt("database_port");
        Environment.database_user = jConfig.getString("database_user");
        Environment.database_password = jConfig.getString("database_password");
        Environment.bot_name = jConfig.getString("bot_name");
        Environment.bot_token = jConfig.getString("bot_token");
        Environment.dns_resolver_address = jConfig.getString("dns_resolver_address");
        Environment.whois_resolver_address = jConfig.getString("whois_resolver_address");

        SSH router = new SSH(
                                Environment.router_username,
                                Environment.router_password,
                                Environment.router_hostname,
                                Environment.router_port
                            );

        DatabaseConnector db = new DatabaseConnector(
                                                        Environment.database_address,
                                                        Environment.database_port,
                                                        Environment.database_user,
                                                        Environment.database_password
                                                    );

        if (!db.check_DB_exists())
            db.migrateDB();

        LogCollector collector = new LogCollector();
        new Thread(collector::startCollect).start();
        collector.initServer();

        Worker worker = new Worker(db, router);
        try {
            TelegramBotsApi bot = new TelegramBotsApi(DefaultBotSession.class);
            Sender botSender = new Sender();
            botSender.setWorker(worker);
            bot.registerBot(botSender);

            while (true) {
                Thread.sleep(10000);
                worker.checkConsistency();
                Tools.logMessage("Routes synchronized!");
            }

        } catch (TelegramApiException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}