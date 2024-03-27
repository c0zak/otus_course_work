package home.vpn.bot;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker {
    private final DatabaseConnector db;
    private final SSH ssh;

    public Sender sender;

    public Worker(DatabaseConnector db, SSH ssh) {
        this.db = db;
        this.ssh = ssh;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    private TreeSet<String> getRouteList() {
        ResultSet routes = db.executeQuery("SELECT route FROM routes;");
        TreeSet<String> result = new TreeSet<>();
        if (routes == null) {
            result.add("Can't receive routes list from DB! Please try again!");
            return result;
        }

        try {
            while (routes.next()) {
                String route = routes.getString("route");
                result.add(route);
            }
            return result;
        }
        catch (Exception e) {
                Tools.logMessage("Error: " + e);
                System.exit(1);
                return null;
        }
    }

    private TreeSet<String> getDescList() {
        ResultSet routes = db.executeQuery("SELECT description FROM routes;");
        TreeSet<String> result = new TreeSet<>();
        if (routes == null) {
            result.add("Can't receive routes list from DB! Please try again!");
            return result;
        }

        try {
            while (routes.next()) {
                String route = routes.getString("description");
                result.add(route);
            }
            return result;
        }
        catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
            return null;
        }
    }

    private String addDomain(String domain) {
        TreeSet<String> routes = IPs_Resolver.getIpSetByName(domain);
        if (routes.isEmpty())
            return "Invalid domain!";
        for (String route : routes) {
            db.executeQuery("INSERT INTO routes (route, description) VALUES ('" + route + "/32', '" + domain + "')");
        }
        Tools.logMessage("Domain " + domain + " successfully added!");
        return "Domain " + domain + " successfully added!";
    }

    private String delDomain(String domain) {
        db.executeQuery("DELETE FROM routes WHERE description = '" + domain + "'");
        Tools.logMessage("Domain " + domain + " successfully deleted!");
        return "Domain " + domain + " successfully deleted!";
    }

    private String addAS(String AS) {
        TreeSet<String> routes = IPs_Resolver.getIpSetByAS(AS);
        if (routes.isEmpty())
            return "Invalid AS!";
        for (String route : routes) {
            db.executeQuery("INSERT INTO routes (route, description) VALUES ('" + route + "', '" + AS + "')");
        }
        Tools.logMessage(AS + " successfully added!");
        return AS + " successfully added!";
    }

    private String delAS(String AS) {
        db.executeQuery("DELETE FROM routes WHERE description = '" + AS + "'");
        Tools.logMessage(AS + " successfully deleted!");
        return AS + " successfully deleted!";
    }

    private String addIP(String IP) {
        Matcher matcher_without_mask = Environment.ipv4_pattern.matcher(IP);
        Matcher matcher_with_mask = Environment.ipv4_mask_pattern.matcher(IP);
        if (matcher_without_mask.matches()) {
            db.executeQuery("INSERT INTO routes (route, description) VALUES ('" + IP + "/32', '" + IP + "/32')");
            Tools.logMessage("IP " + IP + " successfully added!");
            return "IP " + IP + " successfully added!";
        }
        else if (matcher_with_mask.matches()) {
            db.executeQuery("INSERT INTO routes (route, description) VALUES ('" + IP + "', '" + IP + "')");
            Tools.logMessage("IP " + IP + " successfully added!");
            return "IP " + IP + " successfully added!";
        }
        else
            return IP + " - is not ip address!";
    }

    private String delIP(String IP) {
        db.executeQuery("DELETE FROM routes WHERE description LIKE '%" + IP + "%'");
        Tools.logMessage("IP " + IP + " successfully deleted!");
        return "IP " + IP + " successfully deleted!";
    }

    public TreeSet<String> getRoutesFromRouter() {
        ArrayList<String> ssh_routes = ssh.executeCommand(Environment.routeShowCommand);
        TreeSet<String> result = new TreeSet<>();
        for (String sshRoute : ssh_routes) {
            if (sshRoute.contains(Environment.routeShowParseFlag)) {
                String[] iterator = sshRoute.split("\s|\t");
                for (String s : iterator) {
                    Matcher matcher = Environment.ipv4_mask_pattern.matcher(s);
                    if (matcher.matches())
                        result.add(s);
                }
            }
        }
        Tools.logMessage("Routes received from router");
        return result;
    }

    public void checkConsistency() {
        TreeSet<String> db_routes = getRouteList();
        TreeSet<String> ssh_routes = getRoutesFromRouter();
        TreeSet<String> route_for_add = new TreeSet<>();
        TreeSet<String> route_for_del = new TreeSet<>();
        boolean route_exists;
        for (String dbRoute : db_routes) {
            route_exists = false;
            for (String sshRoute : ssh_routes) {
                if (dbRoute.equals(sshRoute)) {
                    route_exists = true;
                }
            }
            if (!route_exists)
                route_for_add.add(dbRoute);
        }
        for (String sshRoute : ssh_routes) {
            route_exists = false;
            for (String dbRoute : db_routes) {
                if (dbRoute.equals(sshRoute))
                    route_exists = true;
            }
            if (!route_exists)
                route_for_del.add(sshRoute);
        }
        for (String s : route_for_del) {
            ssh.executeCommand(Environment.routeDelCommand.replaceAll("IP_MASK", s));
        }
        for (String s : route_for_add) {
            ssh.executeCommand(Environment.routeAddCommand.replaceAll("IP_MASK", s));
        }
    }

    private void approve_request(String userID) {
        ResultSet admins = db.executeQuery("SELECT user_id FROM users WHERE admin = true;");
        if (admins == null) {
            Tools.logMessage("Can't receive admins list from DB! Please try again!");
            sender.sendMessage(Long.valueOf(userID), "Can't receive admins list from DB! Please try again!");
            db.executeQuery("DELETE FROM users WHERE user_id = '" + userID + "';");
            return;
        }
        try {
            int i = 0;
            while (admins.next()) {
                String adminID = admins.getString("user_id");
                sender.sendMessage(Long.valueOf(adminID), "approve " + userID);
                i++;
            }
            if (i == 0)
                throw new Exception();
        }
        catch (Exception e) {
            Tools.logMessage("Error: " + e);
            Tools.logMessage("Can't receive admins list from DB! Please try again!");
            sender.sendMessage(Long.valueOf(userID), "Can't receive admins list from DB! Please try again!");
            db.executeQuery("DELETE FROM users WHERE user_id = '" + userID + "';");
        }
    }

    private String approve(String userID) {
        String transaction = "UPDATE users SET accepted = true WHERE user_id = '" + userID + "';";
        sender.sendMessage(Long.valueOf(userID), "You are approved!");
        String result = db.executeTransaction(transaction);
        Tools.logMessage("Approve query, result: " + result);
        return result;
    }

    private String register(String userID, String password) {
        String transaction = "INSERT INTO users (user_id, user_passwd, last_login, accepted, admin) " +
                "VALUES ('" + userID + "', '" + Tools.getMD5Hash(password) + "', '" + Timestamp.valueOf(LocalDateTime.now()) + "', " +
                "false, false);";
        String result = db.executeTransaction(transaction);
        Tools.logMessage("Register query, result: " + result);
        approve_request(userID);
        return result;
    }
    private String login(String userID, String password) {
        password = Tools.getMD5Hash(password);
        ResultSet get_user = db.executeQuery("SELECT * FROM users WHERE user_id = '" + userID + "'");
        if (get_user == null) {
            Tools.logMessage("Can't receive users list from DB! Please try again!");
            return "Can't receive users list from DB! Please try again!";
        }
        try {
            boolean success = false;
            while (get_user.next()) {
                String db_password = get_user.getString("user_passwd");
                boolean accepted = get_user.getBoolean("accepted");
                if (password.equals(db_password) && accepted)
                    success = true;
            }
            if (success) {
                db.executeTransaction("UPDATE users SET accepted = true WHERE user_id = '" + userID + "';");
                Tools.logMessage("Login query, successful!");
                return "Login successful!";
            }
            else {
                Tools.logMessage("Login query, failed! UserID: " + userID);
                return "Login failed!";
            }
        }
        catch (Exception e) {
            Tools.logMessage("Error: " + e);
            return e.getMessage();
        }
    }

    private boolean isUserAccepted(String userID) {
        ResultSet get_user = db.executeQuery("SELECT accepted FROM users WHERE user_id = '" + userID + "';");
        if (get_user == null) {
            return false;
        }
        try {
            boolean success = false;
            while (get_user.next()) {
                success = get_user.getBoolean("accepted");
            }
            return success;
        }
        catch (Exception e) {
            Tools.logMessage("Error: " + e);
            return false;
        }
    }
    private String help() {
        return "addIP xxx.xxx.xxx.xxx(/mask)\n" +
                "delIP xxx.xxx.xxx.xxx(/mask)\n" +
                "addDomain domainName\n" +
                "delDomain domainName\n" +
                "addAS ASName\n" +
                "delAS ASName\n" +
                "show\n" +
                "login password\n" +
                "register password\n" +
                "getConfig\n";
    }

    public void parse(long chatId, String message) {
        String userID = String.valueOf(chatId);

        if (message.contains("addIP ")) {
            if (isUserAccepted(userID)) {
                message = message.replaceAll("addIP ", "");
                sender.sendMessage(chatId, addIP(message));
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("delIP ")) {
            if (isUserAccepted(userID)) {
                message = message.replaceAll("delIP ", "");
                sender.sendMessage(chatId, delIP(message));
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("addDomain ")) {
            if (isUserAccepted(userID)) {
                message = message.replaceAll("addDomain ", "");
                sender.sendMessage(chatId, addDomain(message));
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("delDomain ")) {
            if (isUserAccepted(userID)) {
                message = message.replaceAll("delDomain ", "");
                sender.sendMessage(chatId, delDomain(message));
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("addAS ")) {
            if (isUserAccepted(userID)) {
                message = message.replaceAll("addAS ", "");
                sender.sendMessage(chatId, addAS(message));
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("delAS ")) {
            if (isUserAccepted(userID)) {
                message = message.replaceAll("delAS ", "");
                sender.sendMessage(chatId, delAS(message));
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("show")) {
            if (isUserAccepted(userID)) {
                TreeSet<String> desc = getDescList();
                StringBuilder result = new StringBuilder();
                for (String s : desc) {
                    result.append(s).append("\n");
                }
                if (result.isEmpty())
                    result.append("Empty list!");
                sender.sendMessage(chatId, result.toString());
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("getConfig")) {
            if (isUserAccepted(userID)) {
                String path = Tools.generateConfig(userID);
                File config = new File(path);
                sender.sendFile(chatId, config);
                Tools.logMessage("Config sent to " + userID + ", file deleted: " + config.delete());
            }
            else
                sender.sendMessage(chatId, "Please register/login before!");
        }

        else if (message.contains("register ")) {
            message = message.replaceAll("register ", "");
            sender.sendMessage(chatId, register(userID, message));
        }

        else if (message.contains("approve ")) {
            message = message.replaceAll("approve ", "");
            sender.sendMessage(chatId, approve(message));
        }

        else if (message.contains("login ")) {
            message = message.replaceAll("login ", "");
            sender.sendMessage(chatId, login(userID, message));
        }

        else if (message.contains("help")) {
            sender.sendMessage(chatId, help());
        }

        else
            sender.sendMessage(chatId, "Unknown command!\n\n" + help());

    }
}

