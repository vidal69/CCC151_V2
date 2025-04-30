package main.java.dao;

import main.java.model.College;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.List;
import java.sql.PreparedStatement;
/**
 * Data Access Object for College entities.
 * Provides CRUD, list‐all, search and sort operations.
 */
public class CollegeDAO {
    private final Connection conn;

    public CollegeDAO(Connection conn) {
        this.conn = conn;
    }

    /** Fetch all colleges. */
    public List<College> getAllColleges() throws SQLException {
        String sql = "SELECT `collegeCode`, `collegeName` FROM `colleges`";
        List<College> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new College(
                        rs.getString("collegeCode"),
                        rs.getString("collegeName")
                ));
            }
        }
        return list;
    }

    /** Fetch all colleges sorted by the given field. */
    public List<College> getAllCollegesSorted(String field) throws SQLException {
        String col = mapFieldToColumn(field);
        String sql = "SELECT `collegeCode`, `collegeName` FROM `colleges` ORDER BY " + col;
        List<College> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new College(
                        rs.getString("collegeCode"),
                        rs.getString("collegeName")
                ));
            }
        }
        return list;
    }

    /** Search colleges by a given field using LIKE. */
    public List<College> searchColleges(String field, String keyword) throws SQLException {
        String col = mapFieldToColumn(field);
        String sql = "SELECT `collegeCode`, `collegeName` FROM `colleges` WHERE " + col + " LIKE ?";
        List<College> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new College(
                            rs.getString("collegeCode"),
                            rs.getString("collegeName")
                    ));
                }
            }
        }
        return list;
    }

    /** Insert a new college. */
    public void addCollege(College c) throws SQLException {
        String sql = "INSERT INTO `colleges` (`collegeCode`, `collegeName`) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCollegeCode());
            ps.setString(2, c.getCollegeName());
            ps.executeUpdate();
        }
    }

    /** Update an existing college, identified by oldCode. */
    public void updateCollege(String oldCode, College c) throws SQLException {
        String sql = "UPDATE `colleges` SET `collegeCode` = ?, `collegeName` = ? WHERE `collegeCode` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCollegeCode());
            ps.setString(2, c.getCollegeName());
            ps.setString(3, oldCode);
            ps.executeUpdate();
        }
    }

    /**
     * Delete a college by its code.
     * Because `programs.collegeCode` is declared
     * WITH `ON DELETE SET NULL`, MySQL will automatically
     * set that column to NULL on all child rows in `programs`.
     */
    public void deleteCollege(String code) throws SQLException {
        String sql = "DELETE FROM `colleges` WHERE `collegeCode` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    /** Map UI field names to back‐ticked SQL column names. */
    private String mapFieldToColumn(String field) {
        switch (field) {
            case "College Code": return "`collegeCode`";
            case "College Name": return "`collegeName`";
            // if you ever use raw column names:
            case "collegeCode":  return "`collegeCode`";
            case "collegeName":  return "`collegeName`";
            default:              return "`" + field + "`";
        }
    }

    /**
     * Deletes multiple colleges in one go.
     */
    public void deleteCollegesBatch(List<String> codes) throws SQLException {
        if (codes == null || codes.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
        String sql = "DELETE FROM colleges WHERE college_code IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < codes.size(); i++) {
                ps.setString(i + 1, codes.get(i));
            }
            ps.executeUpdate();
        }
    }

}