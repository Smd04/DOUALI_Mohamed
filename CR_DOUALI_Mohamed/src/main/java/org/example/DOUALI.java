package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import com.google.gson.*;
import java.sql.*;

public class DOUALI extends Thread {
    private static final String DB_URL = "jdbc:mysql://localhost:3308/Compte_rendue";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private static final int SLEEP_TIME = 3600 * 1000; // 1 hour

    private Connection conn;

    public DOUALI() {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
            System.err.println("Error creating database connection: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                processOrders();
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                System.err.println("Error sleeping: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private synchronized void processOrders() {
        File inputDir = new File("/Users/mohameddouali/IdeaProjects/CR_DOUALI_Mohamed/src/main/resources/Data/input");
        File outputDir = new File("/Users/mohameddouali/IdeaProjects/CR_DOUALI_Mohamed/src/main/resources/Data/output");
        File errorDir = new File("/Users/mohameddouali/IdeaProjects/CR_DOUALI_Mohamed/src/main/resources/Data/errors");

        ensureDirectoryExists(inputDir.getAbsolutePath());
        ensureDirectoryExists(outputDir.getAbsolutePath());
        ensureDirectoryExists(errorDir.getAbsolutePath());

        File[] files = inputDir.listFiles();
        if (files != null) {
            System.out.println("Processing " + files.length + " files ...");
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    System.out.println("Processing JSON file: " + file.getAbsolutePath().replace("\\", "/"));
                    List<Order> orders = readOrdersFromFile(file.toPath().toString().replace("\\", "/"));

                    List<Order> invalidOrders = new ArrayList<>();
                    try {
                        for (Order order : orders) {
                            if (!customerExists(order.getCustomerId())) {
                                invalidOrders.add(order);
                            } else {
                                addOrderToDatabase(order);
                            }
                        }

                        // If there are any invalid orders, move the file to error directory
                        if (!invalidOrders.isEmpty()) {
                            moveFile(file, errorDir);
                            System.err.println("File " + file.getName() + " moved to the errors directory due to invalid customer IDs.");
                        } else {
                            moveFile(file, outputDir);
                            System.out.println("File " + file.getName() + " moved to the output directory.");
                        }

                    } catch (SQLException e) {
                        System.err.println("Error processing orders: " + e.getMessage());
                        moveFile(file, errorDir);
                    }
                }
            }
        }
    }

    private List<Order> readOrdersFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("The file " + filePath + " does not exist. Please ensure it is available.");
            return Collections.emptyList();
        }
        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            return Arrays.asList(gson.fromJson(reader, Order[].class));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean customerExists(int customerId) throws SQLException {
        String query = "SELECT 1 FROM customer WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Customer " + customerId + " exists");
                return true;
            } else {
                System.out.println("Customer " + customerId + " does not exist");
                return false;
            }
        }
    }

    private void addOrderToDatabase(Order order) throws SQLException {
        // Insert query for the orders table (no id as it's auto-incremented)
        String insertQuery = "INSERT INTO orders (date, amount, customer_id, status) VALUES (?, ?, ?, true)";

        try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            // Set the parameters for the INSERT query
            stmt.setString(1, order.getDate());  // Set the date
            stmt.setDouble(2, order.getAmount());  // Set the amount
            stmt.setInt(3, order.getCustomerId());  // Set the customer ID

            // Execute the insert and get the generated id
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 1) {
                // Retrieve the auto-generated id
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);  // Get the auto-generated id
                        System.out.println("Order with id " + generatedId + " added to the database");

                        // Now update the status (although it's already true, let's do it as per the original query)
                        updateOrderStatus(generatedId);
                    }
                }
            }
        }
    }

    private void updateOrderStatus(int orderId) throws SQLException {
        String updateQuery = "UPDATE orders SET status = true WHERE id = ?";

        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
            updateStmt.setInt(1, orderId);  // Set the generated id for the update query

            if (updateStmt.executeUpdate() == 1) {
                System.out.println("Order status for id " + orderId + " updated to true");
            }
        }
    }

    private void ensureDirectoryExists(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private void moveFile(File file, File dir) {
        try {
            File newFile = new File(dir, file.getName());
            if (!newFile.exists()) {
                newFile.createNewFile();
            }
            Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error moving file: " + e.getMessage());
        }
    }
}