package com.github.steinein.pixelwarzone.config;

public class DatabaseConfig {

    private final String poolName;
    private final String ip;
    private final int port;
    private final String username;
    private final String password;
    private final String database;

    public DatabaseConfig(String poolName, String ip, int port, String username, String password, String database) {
        this.poolName = poolName;
        this.ip       = ip;
        this.port     = port;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public String getPoolName() { return poolName; }
    public String getIp()       { return ip; }
    public int    getPort()     { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
}
