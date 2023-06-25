package io.credable.reconapi.util;

import io.credable.reconapi.dto.DatabaseConfigRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

@Log4j2
public class DatabaseToCSV {

    public static MultipartFile extractData(DatabaseConfigRequest databaseConfigRequest, String dateField, String startDate, String endDate) throws IOException {
        String jdbcUrl = "jdbc:%s://%s:%s/%s".formatted(databaseConfigRequest.getDatabaseType(), databaseConfigRequest.getHost(), databaseConfigRequest.getPort(), databaseConfigRequest.getDatabaseName());
        String username = databaseConfigRequest.getUsername();
        String password = databaseConfigRequest.getPassword();
        String tableName = databaseConfigRequest.getSchemaName();
        String csvFilePath = "output.csv";

        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            // Create a connection to the database
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);

            // Prepare the SQL query
            String query = "SELECT * FROM " + tableName + " WHERE %s BETWEEN ? AND ?".formatted(dateField);
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, startDate);
            statement.setString(2, endDate);

            // Execute the query
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();

            // Get the number of columns in the result set
            int columnCount = metaData.getColumnCount();

            // Create a FileWriter to write data to the CSV file
            FileWriter csvWriter = new FileWriter(csvFilePath);

            // Write column headers to the CSV file
            for (int i = 1; i <= columnCount; i++) {
                csvWriter.append(metaData.getColumnName(i));
                if (i < columnCount) {
                    csvWriter.append(",");
                }
            }
            csvWriter.append("\n");

            // Iterate through the result set and write data to the CSV file
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnValue = resultSet.getString(i);
                    csvWriter.append(columnValue);
                    if (i < columnCount) {
                        csvWriter.append(",");
                    }
                }
                csvWriter.append("\n");
            }

            // Close the CSV writer
            csvWriter.close();

            // Close the database connection
            connection.close();

            log.info("Data extracted and saved to " + csvFilePath);
        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
        }
        File file = new File(csvFilePath);
        FileInputStream input = new FileInputStream(file);

        MultipartFile multipartFile = new MockMultipartFile(UUID.randomUUID() + file.getName(),
                null, "text/csv", IOUtils.toByteArray(input));
        log.info("Deleted created db mirror file: " + file.delete());
        return multipartFile;

    }
//    public static void main(String[] args) {
//        String jdbcUrl = "jdbc:mysql://localhost:3306/your_database";
//        String username = "your_username";
//        String password = "your_password";
//        String tableName = "your_table";
//        String startDate = "2023-01-01";
//        String endDate = "2023-01-31";
//        String csvFilePath = "output.csv";
//
//        try {
//            // Load the MySQL JDBC driver
//            Class.forName("com.mysql.jdbc.Driver");
//
//            // Create a connection to the database
//            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
//
//            // Prepare the SQL query
//            String query = "SELECT * FROM " + tableName + " WHERE date_column BETWEEN ? AND ?";
//            PreparedStatement statement = connection.prepareStatement(query);
//            statement.setString(1, startDate);
//            statement.setString(2, endDate);
//
//            // Execute the query
//            ResultSet resultSet = statement.executeQuery();
//            ResultSetMetaData metaData = resultSet.getMetaData();
//
//            // Get the number of columns in the result set
//            int columnCount = metaData.getColumnCount();
//
//            // Create a FileWriter to write data to the CSV file
//            FileWriter csvWriter = new FileWriter(csvFilePath);
//
//            // Write column headers to the CSV file
//            for (int i = 1; i <= columnCount; i++) {
//                csvWriter.append(metaData.getColumnName(i));
//                if (i < columnCount) {
//                    csvWriter.append(",");
//                }
//            }
//            csvWriter.append("\n");
//
//            // Iterate through the result set and write data to the CSV file
//            while (resultSet.next()) {
//                for (int i = 1; i <= columnCount; i++) {
//                    String columnValue = resultSet.getString(i);
//                    csvWriter.append(columnValue);
//                    if (i < columnCount) {
//                        csvWriter.append(",");
//                    }
//                }
//                csvWriter.append("\n");
//            }
//
//            // Close the CSV writer
//            csvWriter.close();
//
//            // Close the database connection
//            connection.close();
//
//            log.info("Data extracted and saved to " + csvFilePath);
//        } catch (ClassNotFoundException | SQLException | IOException e) {
//            e.printStackTrace();
//        }
//    }
}
