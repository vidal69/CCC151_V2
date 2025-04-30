package main.java.app;

import main.java.database.DBHandler;
import main.java.dao.CollegeDAO;
import main.java.dao.ProgramDAO;
import main.java.model.College;
import main.java.model.Program;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProgramManager extends JFrame {
    private final DBHandler dbHandler;
    private final ProgramDAO programDao;
    private final CollegeDAO collegeDao;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField programCodeField;
    private final JTextField programNameField;
    private final JComboBox<String> collegeCodeComboBox;
    private final JTextField searchField;
    private final JComboBox<String> searchCriteriaComboBox;
    private final JComboBox<String> sortCriteriaComboBox;

    public ProgramManager() {
        super("Program Manager");
        // Initialize DB and DAOs
        try {
            dbHandler = new DBHandler("root", "mysql123!");
            Connection conn = dbHandler.getConnection();
            programDao = new ProgramDAO(conn);
            collegeDao = new CollegeDAO(conn);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database initialization failed:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(ex);
        }

        // Window setup
        setSize(600, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panels
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Program Details"));
        inputPanel.add(new JLabel("Program Code:"));
        programCodeField = new JTextField();
        inputPanel.add(programCodeField);
        inputPanel.add(new JLabel("Program Name:"));
        programNameField = new JTextField();
        inputPanel.add(programNameField);
        inputPanel.add(new JLabel("College Code:"));
        collegeCodeComboBox = new JComboBox<>();
        inputPanel.add(collegeCodeComboBox);
        topPanel.add(inputPanel);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Programs"));
        searchCriteriaComboBox = new JComboBox<>(new String[]{"programCode", "programName", "collegeCode"});
        searchField = new JTextField(15);
        searchCriteriaComboBox.addActionListener(e -> performSearch());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) {
                performSearch();
            }
        });
        JButton clearSearchBtn = new JButton("Clear Search");
        clearSearchBtn.addActionListener(e -> {
            searchField.setText("");
            loadProgramData();
        });
        searchPanel.add(new JLabel("Search by:"));
        searchPanel.add(searchCriteriaComboBox);
        searchPanel.add(searchField);
        searchPanel.add(clearSearchBtn);
        topPanel.add(searchPanel);

        // Sort panel
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sortPanel.setBorder(BorderFactory.createTitledBorder("Sort Programs"));
        sortCriteriaComboBox = new JComboBox<>(new String[]{"programCode", "programName", "collegeCode"});
        sortCriteriaComboBox.addActionListener(e -> performSort());
        sortPanel.add(new JLabel("Sort by:"));
        sortPanel.add(sortCriteriaComboBox);
        topPanel.add(sortPanel);

        add(topPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new Object[]{"Program Code", "Program Name", "College Code"}, 0);
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (javax.swing.SwingUtilities.isLeftMouseButton(e) && e.isMetaDown()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row < 0) return;
                    javax.swing.ListSelectionModel sel = table.getSelectionModel();
                    if (sel.isSelectedIndex(row)) {
                        sel.removeSelectionInterval(row, row);
                    } else {
                        sel.addSelectionInterval(row, row);
                    }
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton deleteSelectedBtn = new JButton("Delete Selected");
        JButton saveAllBtn = new JButton("Save All");
        JButton refreshBtn = new JButton("Refresh");
        addBtn.addActionListener(e -> addEntry());
        updateBtn.addActionListener(e -> updateEntry());
        deleteBtn.addActionListener(e -> deleteEntry());
        deleteSelectedBtn.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                JOptionPane.showMessageDialog(this, "No programs selected!", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete the selected programs?",
                "Confirm Batch Delete",
                JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;
            try {
                List<String> codes = new ArrayList<>();
                for (int row : rows) {
                    Object codeObj = tableModel.getValueAt(row, 0);
                    if (codeObj != null) codes.add(codeObj.toString());
                }
                programDao.deleteProgramsBatch(codes);
                loadProgramData();
                clearFields();
            } catch (SQLException ex) {
                showError(ex);
            }
        });
        saveAllBtn.addActionListener(e -> saveAll());
        refreshBtn.addActionListener(e -> {
            loadCollegeCodes();
            loadProgramData();
        });
        btnPanel.add(addBtn); btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(deleteSelectedBtn);
        btnPanel.add(saveAllBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Row select listener
        table.getSelectionModel().addListSelectionListener((ListSelectionListener)e -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                programCodeField.setText(tableModel.getValueAt(r, 0).toString());
                programNameField.setText(tableModel.getValueAt(r, 1).toString());
                collegeCodeComboBox.setSelectedItem(tableModel.getValueAt(r, 2));
            }
        });

        // Load initial data
        loadCollegeCodes();
        loadProgramData();
        setVisible(true);
    }

    private void loadCollegeCodes() {
        collegeCodeComboBox.removeAllItems();
        try {
            for (College c : collegeDao.getAllColleges()) {
                collegeCodeComboBox.addItem(c.getCollegeCode());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadProgramData() {
        tableModel.setRowCount(0);
        try {
            List<Program> list = programDao.getAllPrograms();
            for (Program p : list) {
                tableModel.addRow(new Object[]{p.getProgramCode(), p.getProgramName(), p.getCollegeCode()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void performSearch() {
        tableModel.setRowCount(0);
        try {
            String field = (String) searchCriteriaComboBox.getSelectedItem();
            String kw = searchField.getText().trim();
            List<Program> res = programDao.searchPrograms(field, kw);
            for (Program p : res) {
                tableModel.addRow(new Object[]{p.getProgramCode(), p.getProgramName(), p.getCollegeCode()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void performSort() {
        tableModel.setRowCount(0);
        try {
            String field = (String) sortCriteriaComboBox.getSelectedItem();
            List<Program> sorted = programDao.getAllProgramsSorted(field);
            for (Program p : sorted) {
                tableModel.addRow(new Object[]{p.getProgramCode(), p.getProgramName(), p.getCollegeCode()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addEntry() {
        String code = programCodeField.getText().trim();
        String name = programNameField.getText().trim();
        String cc = (String) collegeCodeComboBox.getSelectedItem();
        if (code.isEmpty() || name.isEmpty() || cc==null) {
            JOptionPane.showMessageDialog(this, "All fields required!");
            return;
        }
        try {
            programDao.addProgram(new Program(code,name,cc));
            loadProgramData();
            clearFields();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateEntry() {
        int r = table.getSelectedRow();
        if (r<0) { JOptionPane.showMessageDialog(this,"Select a row!"); return; }
        String old = tableModel.getValueAt(r,0).toString();
        String code = programCodeField.getText().trim();
        String name = programNameField.getText().trim();
        String cc = (String) collegeCodeComboBox.getSelectedItem();
        try {
            programDao.updateProgram(old,new Program(code,name,cc));
            loadProgramData();
            clearFields();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteEntry() {
        int r = table.getSelectedRow();
        if (r<0) return;
        String code = tableModel.getValueAt(r,0).toString();
        if (JOptionPane.showConfirmDialog(this,"Delete "+code+"?","Confirm",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try {
            programDao.deleteProgram(code);
            loadProgramData();
            clearFields();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void saveAll() {
        // Null-safe batch save: skip rows with missing code/name and allow null college
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            List<Program> all = programDao.getAllPrograms();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object codeObj    = model.getValueAt(i, 0);
                Object nameObj    = model.getValueAt(i, 1);
                Object collegeObj = model.getValueAt(i, 2);

                String code    = codeObj    != null ? codeObj.toString().trim()    : "";
                String name    = nameObj    != null ? nameObj.toString().trim()    : "";
                String college = (collegeObj != null && !collegeObj.toString().trim().isEmpty())
                                 ? collegeObj.toString().trim()
                                 : null;

                // Skip rows missing required fields
                if (code.isEmpty() || name.isEmpty()) {
                    continue;
                }

                Program p = new Program(code, name, college);
                boolean exists = all.stream().anyMatch(x -> x.getProgramCode().equals(code));
                if (exists) {
                    programDao.updateProgram(code, p);
                } else {
                    programDao.addProgram(p);
                }
            }
            loadProgramData();
            JOptionPane.showMessageDialog(this, "All saved!");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void clearFields() {
        programCodeField.setText("");
        programNameField.setText("");
        collegeCodeComboBox.setSelectedIndex(-1);
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProgramManager::new);
    }
}
