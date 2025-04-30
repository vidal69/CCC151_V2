package main.java.database;

import java.sql.*;

/**
 * Bootstraps the MySQL schema and hands out a JDBC Connection.
 */
public class DBHandler {
    private final Connection conn;

    /**
     * Connects to MySQL, creates & switches to the studentsdb database,
     * and ensures required tables exist.
     *
     * @param user your MySQL username
     * @param pass your MySQL password
     * @throws SQLException on any SQL error
     */
    public DBHandler(String user, String pass) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }

        // 1) Connect to the server (no default DB yet)
        String url = "jdbc:mysql://localhost:3306?useSSL=false&serverTimezone=UTC";
        conn = DriverManager.getConnection(url, user, pass);

        // 2) Bootstrap our schema and tables
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS studentsdb");
            stmt.executeUpdate("USE studentsdb");

            // Example: create colleges table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS colleges (" +
                            "  collegeCode VARCHAR(10) NOT NULL," +
                            "  collegeName VARCHAR(100) NOT NULL," +
                            "  PRIMARY KEY(collegeCode)," +
                            "  UNIQUE KEY uq_college_name(collegeName)" +
                            ")"
            );

            // Example: create programs table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS programs (" +
                            "  programCode VARCHAR(20) NOT NULL," +
                            "  programName VARCHAR(100) NOT NULL," +
                            "  collegeCode VARCHAR(10) DEFAULT NULL," +
                            "  PRIMARY KEY(programCode)," +
                            "  UNIQUE KEY uq_program_name(programName)," +
                            "  INDEX idx_prog_college(collegeCode)," +
                            "  CONSTRAINT fk_program_college FOREIGN KEY(collegeCode) " +
                            "    REFERENCES colleges(collegeCode) " +
                            "    ON DELETE SET NULL ON UPDATE CASCADE" +
                            ")"
            );

            // Example: create students table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS students (" +
                            "  id CHAR(9) NOT NULL," +
                            "  firstName VARCHAR(50) NOT NULL," +
                            "  lastName VARCHAR(50) NOT NULL," +
                            "  yearLevel ENUM('1','2','3','4') NOT NULL," +
                            "  gender ENUM('Male','Female') NOT NULL," +
                            "  programCode VARCHAR(20) DEFAULT NULL," +
                            "  PRIMARY KEY(id)," +
                            "  INDEX idx_student_program(programCode)," +
                            "  CONSTRAINT fk_student_program FOREIGN KEY(programCode) " +
                            "    REFERENCES programs(programCode) " +
                            "    ON DELETE SET NULL ON UPDATE CASCADE," +
                            "  CONSTRAINT chk_id_format CHECK (REGEXP_LIKE(id, '^[0-9]{4}-[0-9]{4}$'))" +
                            ")"
            );
        }
    }

    /** @return a live JDBC Connection to studentsdb */
    public Connection getConnection() {
        return conn;
    }

    /** Close the underlying JDBC Connection. */
    public void close() throws SQLException {
        if (conn != null) conn.close();
    }
}