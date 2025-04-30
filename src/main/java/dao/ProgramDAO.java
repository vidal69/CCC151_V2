package main.java.dao;

import main.java.model.Program;

import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Data Access Object for Program entities.
 * Provides CRUD operations, search, and sorting.
 */
public class ProgramDAO {
    private final Connection conn;

    public ProgramDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Fetch all programs.
     */
    public List<Program> getAllPrograms() throws SQLException {
        String sql = "SELECT `programCode`, `programName`, `collegeCode` FROM `programs`";
        List<Program> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Program(
                    rs.getString("programCode"),
                    rs.getString("programName"),
                    rs.getString("collegeCode")
                ));
            }
        }
        return list;
    }

    /**
     * Fetch all programs sorted by the given field.
     * @param field "programCode", "programName", or "collegeCode"
     */
    public List<Program> getAllProgramsSorted(String field) throws SQLException {
        String column = mapFieldToColumn(field);
        String sql = "SELECT `programCode`, `programName`, `collegeCode` FROM `programs` ORDER BY " + column;
        List<Program> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Program(
                    rs.getString("programCode"),
                    rs.getString("programName"),
                    rs.getString("collegeCode")
                ));
            }
        }
        return list;
    }

    /**
     * Search programs by a given field using LIKE.
     */
    public List<Program> searchPrograms(String field, String keyword) throws SQLException {
        String column = mapFieldToColumn(field);
        String sql = "SELECT `programCode`, `programName`, `collegeCode` FROM `programs` WHERE " + column + " LIKE ?";
        List<Program> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Program(
                        rs.getString("programCode"),
                        rs.getString("programName"),
                        rs.getString("collegeCode")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Insert a new program.
     */
    public void addProgram(Program p) throws SQLException {
        String sql = "INSERT INTO `programs` (`programCode`, `programName`, `collegeCode`) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProgramCode());
            ps.setString(2, p.getProgramName());
            ps.setString(3, p.getCollegeCode());
            ps.executeUpdate();
        }
    }

    /**
     * Update an existing program identified by oldCode.
     */
    public void updateProgram(String oldCode, Program p) throws SQLException {
        String sql = "UPDATE `programs` SET `programCode` = ?, `programName` = ?, `collegeCode` = ? WHERE `programCode` = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProgramCode());
            ps.setString(2, p.getProgramName());
            ps.setString(3, p.getCollegeCode());
            ps.setString(4, oldCode);
            ps.executeUpdate();
        }
    }

    /**
     * Delete a program by its code and null out any student references first.
     */
    public void deleteProgram(String code) throws SQLException {
        // 1) Nullify any students referencing this program
        String nullifyStudentsSql =
            "UPDATE `students` SET `programCode` = NULL WHERE `programCode` = ?";
        try (PreparedStatement ps = conn.prepareStatement(nullifyStudentsSql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
        // 2) Delete the program itself
        String deleteProgramSql =
            "DELETE FROM `programs` WHERE `programCode` = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteProgramSql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    /**
     * Fetch all programs belonging to a given college.
     */
    public List<Program> getProgramsByCollege(String collegeCode) throws SQLException {
        String sql = "SELECT `programCode`, `programName`, `collegeCode` FROM `programs` WHERE `collegeCode` = ?";
        List<Program> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, collegeCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Program(
                        rs.getString("programCode"),
                        rs.getString("programName"),
                        rs.getString("collegeCode")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Maps property names to actual database column identifiers (with backticks).
     */

    private String mapFieldToColumn(String field) {
        switch (field) {
            case "programCode":  return "`programCode`";
            case "programName":  return "`programName`";
            case "collegeCode":  return "`collegeCode`";
            default:              return "`" + field + "`";
        }
    }
    /**
     * Fetch a single program by its code.
     */
    public Program getProgramByCode(String programCode) throws SQLException {
        String sql = "SELECT programCode, programName, collegeCode FROM programs WHERE programCode = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, programCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Program(
                        rs.getString("programCode"),
                        rs.getString("programName"),
                        rs.getString("collegeCode")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Deletes multiple programs in one go.
     */
    public void deleteProgramsBatch(List<String> codes) throws SQLException {
        if (codes == null || codes.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
        String sql = "DELETE FROM `programs` WHERE `programCode` IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < codes.size(); i++) {
                ps.setString(i + 1, codes.get(i));
            }
            ps.executeUpdate();
        }
    }

}