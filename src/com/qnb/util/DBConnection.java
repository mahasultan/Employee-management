package com.qnb.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Fill these in locally - never commit real credentials to GitHub
    private static final String URL = "jdbc:oracle:thin:@//HOST:PORT/SERVICE_NAME";
    private static final String USER = "YOUR_USER";
    private static final String PASSWORD = "YOUR_PASSWORD";

    // connection where DAO method will call
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connected successfully!");
        } catch (SQLException e) {
            System.out.println("Connection failed:");
            e.printStackTrace();
        }
    }
}
