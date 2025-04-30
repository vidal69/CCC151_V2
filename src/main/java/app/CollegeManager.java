package main.java.app;

import main.java.database.DBHandler;
import main.java.dao.CollegeDAO;
import main.java.model.College;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * GUI for managing colleges with MySQL CRUD, search, and sort.
 */
public class CollegeManager extends JFrame {
    private DBHandler dbHandler;
    private CollegeDAO collegeDao;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField collegeCodeField;
    private JTextField collegeNameField;
    private JTextField searchField;
    private JComboBox<String> searchCriteriaComboBox;
    private JComboBox<String> sortCriteriaComboBox;
    private List<College> collegeList;

    public CollegeManager() {
        super("College Manager");
        // Initialize DB and DAO
        try {
            dbHandler = new DBHandler("root", "mysql123!");
            Connection conn = dbHandler.getConnection();
            collegeDao = new CollegeDAO(conn);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database initialization failed:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(600, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Top panel (input, search, sort)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("College Details"));
        inputPanel.add(new JLabel("College Code:"));
        collegeCodeField = new JTextField();
        inputPanel.add(collegeCodeField);
        inputPanel.add(new JLabel("College Name:"));
        collegeNameField = new JTextField();
        inputPanel.add(collegeNameField);
        topPanel.add(inputPanel);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Colleges"));
        searchCriteriaComboBox = new JComboBox<>(new String[]{"College Code", "College Name"});
        searchField = new JTextField(15);
        // Re-run search when criteria or text changes
        searchCriteriaComboBox.addActionListener(e -> performSearch());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { performSearch(); }
            public void removeUpdate(DocumentEvent e) { performSearch(); }
            public void changedUpdate(DocumentEvent e) { performSearch(); }
        });
        JButton clearSearchButton = new JButton("Clear Search");
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadCollegeData();
        });
        searchPanel.add(new JLabel("Search by:"));
        searchPanel.add(searchCriteriaComboBox);
        searchPanel.add(searchField);
        searchPanel.add(clearSearchButton);
        topPanel.add(searchPanel);

        // Sort panel
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sortPanel.setBorder(BorderFactory.createTitledBorder("Sort Colleges"));
        sortPanel.add(new JLabel("Sort by:"));
        sortCriteriaComboBox = new JComboBox<>(new String[]{"College Code", "College Name"});
        sortCriteriaComboBox.addActionListener(e -> performSort());
        sortPanel.add(sortCriteriaComboBox);
        topPanel.add(sortPanel);

        add(topPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new Object[]{"College Code", "College Name"}, 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton saveAllButton = new JButton("Save All");
        JButton refreshButton = new JButton("Refresh");
        addButton.addActionListener(e -> addEntry());
        updateButton.addActionListener(e -> updateEntry());
        deleteButton.addActionListener(e -> deleteEntry());
        saveAllButton.addActionListener(e -> saveAll());
        refreshButton.addActionListener(e -> loadCollegeData());
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveAllButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Table selection listener to populate fields
        table.getSelectionModel().addListSelectionListener((ListSelectionListener) e -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                collegeCodeField.setText(tableModel.getValueAt(r, 0).toString());
                collegeNameField.setText(tableModel.getValueAt(r, 1).toString());
            }
        });

        // Initial load
        loadCollegeData();
        setVisible(true);
    }

    private void loadCollegeData() {
        try {
            collegeList = collegeDao.getAllColleges();
            tableModel.setRowCount(0);
            for (College c : collegeList) {
                tableModel.addRow(new Object[]{c.getCollegeCode(), c.getCollegeName()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void performSearch() {
        String kw = searchField.getText().trim();
        String crit = (String) searchCriteriaComboBox.getSelectedItem();
        String col = crit.equals("College Name") ? "Name" : "Code";
        try {
            List<College> results = collegeDao.searchColleges(col, kw);
            tableModel.setRowCount(0);
            for (College c : results) {
                tableModel.addRow(new Object[]{c.getCollegeCode(), c.getCollegeName()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void performSort() {
        String crit = (String) sortCriteriaComboBox.getSelectedItem();
        String col = crit.equals("College Name") ? "Name" : "Code";
        try {
            List<College> sorted = collegeDao.getAllCollegesSorted(col);
            tableModel.setRowCount(0);
            for (College c : sorted) {
                tableModel.addRow(new Object[]{c.getCollegeCode(), c.getCollegeName()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addEntry() {
        String code = collegeCodeField.getText().trim();
        String name = collegeNameField.getText().trim();
        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both fields are required!");
            return;
        }
        try {
            collegeDao.addCollege(new College(code, name));
            loadCollegeData();
            clearFields();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateEntry() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to update!");
            return;
        }
        String oldCode = collegeList.get(r).getCollegeCode();
        String newCode = collegeCodeField.getText().trim();
        String newName = collegeNameField.getText().trim();
        if (newCode.isEmpty() || newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both fields are required!");
            return;
        }
        try {
            collegeDao.updateCollege(oldCode, new College(newCode, newName));
            loadCollegeData();
            clearFields();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteEntry() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        String code = tableModel.getValueAt(r, 0).toString();
        int ans = JOptionPane.showConfirmDialog(this,
                "Delete college " + code + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        try {
            collegeDao.deleteCollege(code);
            loadCollegeData();
            clearFields();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void saveAll() {
        try {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String code = tableModel.getValueAt(i, 0).toString();
                String name = tableModel.getValueAt(i, 1).toString();
                College c = new College(code, name);
                boolean exists = collegeDao.getAllColleges()
                        .stream().anyMatch(x -> x.getCollegeCode().equals(code));
                if (exists) collegeDao.updateCollege(code, c);
                else collegeDao.addCollege(c);
            }
            loadCollegeData();
            JOptionPane.showMessageDialog(this, "All rows saved!");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void clearFields() {
        collegeCodeField.setText("");
        collegeNameField.setText("");
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}