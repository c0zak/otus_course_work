package home.vpn.bot;

import java.util.regex.Pattern;

public class Environment {
    public static String router_hostname;
    public static Integer router_port;
    public static String router_username;
    public static String router_password;
    public static String routeAddCommand;
    public static String routeDelCommand;
    public static String routeShowCommand;
    public static String routeShowParseFlag;
    public static String ovpn_server_ip;
    public static String ovpn_port;
    public static String ovpn_protocol;
    public static String database_address;
    public static Integer database_port;
    public static String database_user;
    public static String database_password;
    public static String bot_name;
    public static String bot_token;
    public static String dns_resolver_address;
    public static String whois_resolver_address;
    public static final Pattern ipv4_pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$");
    public static final Pattern ipv4_mask_pattern = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])/([0-9]|[1-2][0-9]|3[0-2])$");
}
