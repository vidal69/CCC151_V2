package main.java.dao;

import main.java.model.Student;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for Student entities.
 * Provides CRUD operations and pagination on the students table.
 */
public class StudentDAO {
    private final Connection conn;

    /**
     * Constructs the DAO with an existing database connection.
     * @param conn JDBC Connection to the studentsdb schema
     */
    public StudentDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Retrieves all students from the database.
     * @return List of all Student objects
     * @throws SQLException on database errors
     */
    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT id, firstName, lastName, yearLevel, gender, programCode FROM students";
        List<Student> students = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                students.add(new Student(
                        rs.getString("id"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("yearLevel"),
                        rs.getString("gender"),
                        rs.getString("programCode")
                ));
            }
        }
        return students;
    }

    /**
     * Inserts a new student record.
     * @param s Student object to add
     * @throws SQLException on database errors
     */
    public void addStudent(Student s) throws SQLException {
        String sql = "INSERT INTO students (id, firstName, lastName, yearLevel, gender, programCode) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getFirstName());
            ps.setString(3, s.getLastName());
            ps.setString(4, s.getYearLevel());
            ps.setString(5, s.getGender());
            ps.setString(6, s.getProgramCode());
            ps.executeUpdate();
        }
    }

    /**
     * Updates an existing student record.
     * @param s Student object containing updated data (identified by id)
     * @throws SQLException on database errors
     */
    public void updateStudent(Student s) throws SQLException {
        String sql = "UPDATE students SET firstName=?, lastName=?, yearLevel=?, gender=?, programCode=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getFirstName());
            ps.setString(2, s.getLastName());
            ps.setString(3, s.getYearLevel());
            ps.setString(4, s.getGender());
            ps.setString(5, s.getProgramCode());
            ps.setString(6, s.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a student by ID.
     * @param studentId the ID of the student to delete
     * @throws SQLException on database errors
     */
    public void deleteStudent(String studentId) throws SQLException {
        String sql = "DELETE FROM students WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns the total number of student records.
     * @return total row count
     * @throws SQLException on database errors
     */
    public int getTotalCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Retrieves a specific page of students.
     * @param page 1-based page number
     * @param pageSize number of records per page
     * @return List of Student for the given page
     * @throws SQLException on database errors
     */
    public List<Student> getStudentsPage(int page, int pageSize) throws SQLException {
        int offset = (page - 1) * pageSize;
        String sql = "SELECT id, firstName, lastName, yearLevel, gender, programCode " +
                "FROM students ORDER BY id LIMIT ? OFFSET ?";
        List<Student> students = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(new Student(
                            rs.getString("id"),
                            rs.getString("firstName"),
                            rs.getString("lastName"),
                            rs.getString("yearLevel"),
                            rs.getString("gender"),
                            rs.getString("programCode")
                    ));
                }
            }
        }
        return students;
    }

    /**
     * Searches students by a given column using SQL LIKE.
     * @param column the DB column to search (e.g. "id", "firstName", "lastName")
     * @param keyword substring to match
     * @return list of matching Student objects
     * @throws SQLException on DB errors
     */
    public List<Student> searchStudents(String column, String keyword) throws SQLException {
        String sql = "SELECT id, firstName, lastName, yearLevel, gender, programCode " +
                "FROM students WHERE " + column + " LIKE ?";
        List<Student> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Student(
                            rs.getString("id"),
                            rs.getString("firstName"),
                            rs.getString("lastName"),
                            rs.getString("yearLevel"),
                            rs.getString("gender"),
                            rs.getString("programCode")
                    ));
                }
            }
        }
        return list;
    }

    public void deleteStudentsBatch(List<String> studentIds) throws SQLException {
        if (studentIds == null || studentIds.isEmpty()) return;

        // build "(?, ?, …)" placeholders
        String placeholders = String.join(",", Collections.nCopies(studentIds.size(), "?"));
        String sql = "DELETE FROM students WHERE id IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < studentIds.size(); i++) {
                ps.setString(i + 1, studentIds.get(i));
            }
            ps.executeUpdate();
        }
    }

    public int getStudentCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

}