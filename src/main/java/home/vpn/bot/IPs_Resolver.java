package home.vpn.bot;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPs_Resolver {
    public static TreeSet<String> getIpSetByName(String domainName) {
        try {
            TreeSet<String> result = new TreeSet<>();
            ArrayList<String> ns_result = Tools.shExecute("nslookup " + domainName + " " + Environment.dns_resolver_address);
            if (ns_result == null) {
                Tools.logMessage("Can't get info from dns resolver " + Environment.dns_resolver_address + "!");
                System.exit(1);
            }
            for (String s : ns_result) {
                if (s.contains("Address: ")) {
                    s = s.split(" ")[1];
                    Matcher matcher = Environment.ipv4_pattern.matcher(s);
                    if (matcher.matches())
                        result.add(s);
                }
            }
            Tools.logMessage("Address " + domainName + " resolved!");
            return result;
        }
        catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
            return null;
        }
    }

    public static TreeSet<String> getIpSetByAS(String AS) {
        try {
            TreeSet<String> result = new TreeSet<>();
            ArrayList<String> wh_result = Tools.shExecute("whois -h " + Environment.whois_resolver_address + " -- '-i origin " + AS + "' | grep route:");
            if (wh_result == null) {
                Tools.logMessage("Can't get info from whois resolver " + Environment.whois_resolver_address + "!");
                System.exit(1);
            }
            for (String s : wh_result) {
                s = s.replaceAll("route:", "");
                s = s.replaceAll(" ", "");
                s = s.replaceAll("\t", "");
                result.add(s);
            }
            Tools.logMessage("AS " + AS + " resolved!");
            return result;
        }
        catch (Exception e) {
            return null;
        }
    }

}

