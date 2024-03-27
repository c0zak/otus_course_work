package home.vpn.bot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseConnector {
    private final String dbAddress;
    private final Integer dbPort;
    private final String dbUser;
    private final String dbPassword;
    private Connection connector;

    public DatabaseConnector(String dbAddress, Integer dbPort, String dbUser, String dbPassword) {
        this.dbAddress = dbAddress;
        this.dbPort = dbPort;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.connect();
    }

    public void connect() {
        try {
            connector = DriverManager.getConnection("jdbc:postgresql://" + dbAddress + ":" + dbPort + "/postgres", dbUser, dbPassword);
        } catch (SQLException ignored) {}
    }

    public ResultSet executeQuery(String query) {
        try {
            Statement statement = connector.createStatement();
            return statement.executeQuery(query);
        } catch (Exception e) {
            return null;
        }
    }

    public String executeTransaction(String functions) {
        StringBuilder result = new StringBuilder();
        StringBuilder transaction = new StringBuilder();
        transaction.append("BEGIN;\n");
        transaction.append(functions);
        transaction.append("COMMIT;\n");
        try {
            try (PreparedStatement stmt = connector.prepareStatement(transaction.toString())) {
                stmt.execute();
                result.append("Transaction completed!");
            } catch (SQLException ex) {
                try (PreparedStatement rollbackStmt = connector.prepareStatement("ROLLBACK;")) {
                    rollbackStmt.execute();
                }
                result.append("Error message: ").append(ex.getMessage()).append("\n");
                result.append("SQL state: ").append(ex.getSQLState()).append("\n");
                result.append("Error code: ").append(ex.getErrorCode()).append("\n");
                throw ex;
            }
        } catch (SQLException ex) {
            result.append("Global error: ").append(ex).append("\n");
        }
        return result.toString();
    }

    public boolean check_DB_exists() {
        ResultSet check = executeQuery("SELECT * FROM lastUpdate");
        return check != null;
    }

    public void migrateDB() {
        StringBuilder transaction = new StringBuilder();
        try {
            BufferedReader fReader = new BufferedReader(new FileReader("/sql/lastUpdate.sql"));
            String line;
            while ((line = fReader.readLine()) != null) {
                transaction.append('\n').append(line);
            }
            fReader.close();
            fReader = new BufferedReader(new FileReader("/sql/routes.sql"));
            while ((line = fReader.readLine()) != null) {
                transaction.append('\n').append(line);
            }
            fReader.close();
            fReader = new BufferedReader(new FileReader("/sql/users.sql"));
            while ((line = fReader.readLine()) != null) {
                transaction.append('\n').append(line);
            }
            fReader.close();
            Tools.logMessage("Transaction: " + transaction);
            Tools.logMessage(executeTransaction(transaction.toString()));
            executeQuery("INSERT INTO lastUpdate (Date) VALUES ('" + Timestamp.valueOf(LocalDateTime.now()) + "')");
        } catch (Exception e) {
            Tools.logMessage("Error: " + e);
            System.exit(1);
        }
    }

}
