package main.java.app;
import java.sql.SQLIntegrityConstraintViolationException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import main.java.app.StudentGenerator;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.util.regex.Pattern;


import main.java.database.DBHandler;
import main.java.dao.StudentDAO;
import main.java.dao.CollegeDAO;
import main.java.dao.ProgramDAO;
import main.java.model.Student;
import main.java.model.College;
import main.java.model.Program;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import java.awt.Component;

// Main GUI for student management
public class MainGUI extends JFrame {
    private final DBHandler dbHandler;
    private final StudentDAO studentDao;
    private final CollegeDAO collegeDao;
    private final ProgramDAO programDao;

    private JTextField idField, firstNameField, lastNameField, searchField;
    private JComboBox<String> yearLevelBox, genderBox, collegeBox, programBox;
    private JTable studentTable;
    private StudentTableModel tableModel;
    private JComboBox<String> searchCriteriaComboBox;
    private JLabel studentCountLabel;
    private JComboBox<String> sortComboBox, filterCollegeBox, filterProgramBox;
    private boolean sortAscending = true;

    private List<Student> studentList = new ArrayList<>();
    private List<Student> filteredList = new ArrayList<>();


    private int currentPage = 1;
    private int totalPages = 1;
    private static final int PAGE_SIZE = 50;
    private JButton prevBtn, nextBtn;
    private JTextField pageField;
    private JLabel totalPagesLabel;

    private static final Pattern ID_PATTERN = Pattern.compile("\\d{4}-\\d{4}");

    public MainGUI() throws SQLException {
        super("Student Database Management");
        // 1) Connect & bootstrap
        dbHandler = new DBHandler("root", "mysql123!");
        Connection conn = dbHandler.getConnection();
        studentDao = new StudentDAO(conn);
        collegeDao = new CollegeDAO(conn);
        programDao = new ProgramDAO(conn);

        // Load data
        loadAllStudents();
        // Build UI
        initComponents();
        initLookup();
        ensureCollegesAndProgramsExist();
        refreshPage();

        setSize(800, 915);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void loadAllStudents() {
        try {
            studentList = studentDao.getAllStudents();
            filteredList = new ArrayList<>(studentList);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void initLookup() {
        // populate main college dropdown
        collegeBox.removeAllItems();
        try {
            for (College c : collegeDao.getAllColleges()) {
                collegeBox.addItem(c.getCollegeCode());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        // ensure programs update on college change
        if (collegeBox.getItemCount() > 0) {
            collegeBox.setSelectedIndex(0);
            loadPrograms();
        }

        // Update programBox when collegeBox changes, but do not auto-reset programBox selection
        collegeBox.addActionListener(e -> {
            loadPrograms();
        });

        // populate filter college dropdown
        filterCollegeBox.removeAllItems();
        filterCollegeBox.addItem("All");
        try {
            for (College c : collegeDao.getAllColleges()) {
                filterCollegeBox.addItem(c.getCollegeCode());
            }
        } catch (SQLException ex) {
            showError(ex);
        }

        // reset filter program dropdown
        filterProgramBox.removeAllItems();
        filterProgramBox.addItem("All");

        // apply filter when dropdowns change
        filterCollegeBox.addActionListener(e -> {
            updateFilterPrograms();
            refreshPage();
        });
        filterProgramBox.addActionListener(e -> refreshPage());
    }

    private void loadPrograms() {
        programBox.removeAllItems();
        try {
            String code = (String) collegeBox.getSelectedItem();
            for (Program p : programDao.getProgramsByCollege(code)) {
                programBox.addItem(p.getProgramCode());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateFilterPrograms() {
        filterProgramBox.removeAllItems();
        filterProgramBox.addItem("All");
        try {
            String code = (String) filterCollegeBox.getSelectedItem();
            if (!"All".equals(code)) {
                for (Program p : programDao.getProgramsByCollege(code)) {
                    filterProgramBox.addItem(p.getProgramCode());
                }
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        refreshPage();
    }

    private void initComponents() {
        collegeBox = new JComboBox<>();
        programBox = new JComboBox<>();
        // Input Panel
        idField = new JTextField();
        // ID format filter
        ((AbstractDocument) idField.getDocument()).setDocumentFilter(new DocumentFilter() {
            private final Pattern FILTER_PATTERN = Pattern.compile("\\d{0,4}(-?\\d{0,4})?");
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset, string);
                if (FILTER_PATTERN.matcher(sb.toString()).matches()) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.replace(offset, offset + length, text);
                if (FILTER_PATTERN.matcher(sb.toString()).matches()) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.delete(offset, offset + length);
                if (FILTER_PATTERN.matcher(sb.toString()).matches()) {
                    super.remove(fb, offset, length);
                }
            }
        });
        firstNameField = new JTextField();
        // Name input filter
        ((AbstractDocument) firstNameField.getDocument()).setDocumentFilter(new DocumentFilter() {
            private final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z ]*");
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (LETTER_PATTERN.matcher(string).matches()) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (LETTER_PATTERN.matcher(text).matches()) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        lastNameField = new JTextField();
        // Name input filter
        ((AbstractDocument) lastNameField.getDocument()).setDocumentFilter(new DocumentFilter() {
            private final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z ]*");
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (LETTER_PATTERN.matcher(string).matches()) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (LETTER_PATTERN.matcher(text).matches()) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        yearLevelBox = new JComboBox<>(new String[]{"1","2","3","4"});
        genderBox = new JComboBox<>(new String[]{"Male","Female"});

        JPanel inputPanel = new JPanel(new GridLayout(7,2,5,5));
        inputPanel.add(new JLabel("ID (YYYY-NNNN):")); inputPanel.add(idField);
        inputPanel.add(new JLabel("First Name:")); inputPanel.add(firstNameField);
        inputPanel.add(new JLabel("Last Name:")); inputPanel.add(lastNameField);
        inputPanel.add(new JLabel("Year Level:")); inputPanel.add(yearLevelBox);
        inputPanel.add(new JLabel("Gender:")); inputPanel.add(genderBox);
        inputPanel.add(new JLabel("College:")); inputPanel.add(collegeBox);
        inputPanel.add(new JLabel("Program:")); inputPanel.add(programBox);

        JButton addBtn = new JButton("Add"); addBtn.addActionListener(e->addStudent());
        JButton updateBtn = new JButton("Update"); updateBtn.addActionListener(e->updateStudent());
        JButton deleteBtn = new JButton("Delete"); deleteBtn.addActionListener(e->deleteStudent());
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveAll());
        JButton manageCollegesBtn = new JButton("Manage Colleges");
        JButton manageProgramsBtn = new JButton("Manage Programs");

        manageCollegesBtn.addActionListener(e -> {
            CollegeManager cm = new CollegeManager();
            cm.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    initLookup();
                    loadPrograms();
                    updateFilterPrograms();
                    refreshPage();
                }
            });
            cm.setVisible(true);
        });
        manageProgramsBtn.addActionListener(e -> {
            ProgramManager pm = new ProgramManager();
            pm.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    loadPrograms();
                    updateFilterPrograms();
                    refreshPage();
                }
            });
            pm.setVisible(true);
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(manageCollegesBtn);
        buttonPanel.add(manageProgramsBtn);
        // Insert Refresh button after manageCollegesBtn and manageProgramsBtn
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            loadAllStudents();
            initLookup();
            loadPrograms();
            updateFilterPrograms();
            refreshPage();
        });
        buttonPanel.add(refreshBtn);
        buttonPanel.revalidate();
        buttonPanel.repaint();

        // Table
        tableModel = new StudentTableModel(filteredList);
        studentTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(studentTable);
        // --- Pagination Controls ---
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        prevBtn = new JButton("Prev");
        nextBtn = new JButton("Next");
        pageField = new JTextField(3);
        pageField.setHorizontalAlignment(JTextField.CENTER);
        ((AbstractDocument) pageField.getDocument()).setDocumentFilter(new DocumentFilter() {
            private final Pattern REG = Pattern.compile("[1-9]\\d*");
            @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset, string);
                if (REG.matcher(sb.toString()).matches()) super.insertString(fb, offset, string, attr);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.replace(offset, offset + length, text);
                if (REG.matcher(sb.toString()).matches()) super.replace(fb, offset, length, text, attrs);
            }
        });
        pageField.setText(String.valueOf(currentPage));
        pageField.addActionListener(e -> {
            try {
                int page = Integer.parseInt(pageField.getText().trim());
                if (page < 1) throw new NumberFormatException();
                currentPage = Math.min(page, totalPages);
                refreshPage();
            } catch (NumberFormatException ex) {
                pageField.setText(String.valueOf(currentPage));
            }
        });
        totalPagesLabel = new JLabel(" of " + totalPages);
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                refreshPage();
            }
        });
        paginationPanel.add(prevBtn);
        paginationPanel.add(new JLabel("Page "));
        paginationPanel.add(pageField);
        paginationPanel.add(totalPagesLabel);
        paginationPanel.add(nextBtn);
        // Register a selection listener to populate fields and reload program dropdown
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int r = studentTable.getSelectedRow();
                if (r >= 0) {
                    Student s = tableModel.getStudentAt(r);
                    idField.setText(s.getId());
                    firstNameField.setText(s.getFirstName());
                    lastNameField.setText(s.getLastName());
                    yearLevelBox.setSelectedItem(s.getYearLevel());
                    genderBox.setSelectedItem(s.getGender());
                    try {
                        // Lookup program to find its college
                        Program p = programDao.getProgramByCode(s.getProgramCode());
                        if (p != null) {
                            collegeBox.setSelectedItem(p.getCollegeCode());
                            loadPrograms();
                            programBox.setSelectedItem(p.getProgramCode());
                        } else {
                            programBox.removeAllItems();
                        }
                    } catch (SQLException ex) {
                        showError(ex);
                    }
                }
            }
        });

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchCriteriaComboBox = new JComboBox<>(new String[]{"ID", "Last Name", "First Name"});
        searchField = new JTextField(15);
        // Re-run search when criteria or text changes
        searchCriteriaComboBox.addActionListener(e -> refreshPage());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshPage();
            }
        });
        searchPanel.add(new JLabel("Search by:"));
        searchPanel.add(searchCriteriaComboBox);
        searchPanel.add(searchField);

        // Sort & filter
        sortComboBox = new JComboBox<>(new String[]{"ID", "First Name", "Last Name", "Year", "Gender", "College", "Program"});
        sortComboBox.addActionListener(e -> refreshPage());
        JButton sortBtn = new JButton("Sort");
        sortBtn.addActionListener(e -> {
            sortAscending = !sortAscending;
            refreshPage();
        });

        filterCollegeBox = new JComboBox<>();
        filterCollegeBox.addItem("All");
        try {
            for (College c : collegeDao.getAllColleges()) {
                filterCollegeBox.addItem(c.getCollegeCode());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        filterCollegeBox.addActionListener(e -> {
            updateFilterPrograms();
            refreshPage();
        });

        filterProgramBox = new JComboBox<>();
        filterProgramBox.addItem("All");
        filterProgramBox.addActionListener(e -> refreshPage());

        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sortPanel.add(new JLabel("Sort:"));
        sortPanel.add(sortComboBox);
        sortPanel.add(sortBtn);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter College:"));
        filterPanel.add(filterCollegeBox);
        filterPanel.add(new JLabel("Program:"));
        filterPanel.add(filterProgramBox);

        studentCountLabel = new JLabel();
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(studentCountLabel);

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(searchPanel);
        bottomPanel.add(tableScroll);
        bottomPanel.add(paginationPanel);
        bottomPanel.add(statusPanel);
        bottomPanel.add(sortPanel);
        bottomPanel.add(filterPanel);
        // Generate Students button
        JPanel genPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton genBtn = new JButton("Generate Students");
        genBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(
                this,
                "How many students to generate?",
                "1000"
            );
            if (input == null) return;
            int count;
            try {
                count = Integer.parseInt(input.trim());
                if (count <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a positive integer.",
                    "Invalid Number",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            new Thread(() -> {
                StudentGenerator generator = new StudentGenerator(studentDao, programDao);
                generator.generate(count);
                SwingUtilities.invokeLater(() -> {
                    currentPage = 1;
                    refreshPage();
                    JOptionPane.showMessageDialog(
                        this,
                        "Generated " + count + " students."
                    );
                });
            }).start();
        });
        genPanel.add(genBtn);
        bottomPanel.add(genPanel);

        // Main panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        add(panel);
        setVisible(true);
    }

    /**
     * Reloads data: search, filter, sort, paginate, and update UI.
     */
    private void refreshPage() {
        try {
            if (PAGE_SIZE <= 0) throw new IllegalStateException("Invalid page size: " + PAGE_SIZE);

            // 1) Search or load all students
            String keyword = searchField.getText().trim();
            String criteria = (String) searchCriteriaComboBox.getSelectedItem();
            String column = "id";
            if ("First Name".equals(criteria)) column = "firstName";
            else if ("Last Name".equals(criteria)) column = "lastName";
            List<Student> base = keyword.isEmpty()
                ? studentDao.getAllStudents()
                : studentDao.searchStudents(column, keyword);

            // Cache program→college mapping to speed up filtering
            Map<String,String> programCollegeMap = new HashMap<>();
            for (Program p : programDao.getAllPrograms()) {
                programCollegeMap.put(p.getProgramCode(), p.getCollegeCode());
            }

            // 2) Filter by college/program using cached map
            String coll = (String) filterCollegeBox.getSelectedItem();
            String prog = (String) filterProgramBox.getSelectedItem();
            List<Student> filtered = new ArrayList<>();
            for (Student s : base) {
                String code = s.getProgramCode();
                String colCode = programCollegeMap.get(code);
                if (coll != null && !"All".equals(coll) && (colCode == null || !coll.equals(colCode))) {
                    continue;
                }
                if (prog != null && !"All".equals(prog) && !prog.equals(code)) {
                    continue;
                }
                filtered.add(s);
            }

            // 3) Sort
            Comparator<Student> cmp;
            String sortKey = (String) sortComboBox.getSelectedItem();
            switch (sortKey) {
                case "First Name": cmp = Comparator.comparing(Student::getFirstName); break;
                case "Last Name":  cmp = Comparator.comparing(Student::getLastName);  break;
                case "Year":       cmp = Comparator.comparing(Student::getYearLevel); break;
                case "Gender":     cmp = Comparator.comparing(Student::getGender);    break;
                case "College":    cmp = Comparator.comparing(s -> {
                                         try {
                                             Program p = programDao.getProgramByCode(s.getProgramCode());
                                             return p != null ? p.getCollegeCode() : "";
                                         } catch (SQLException ex) {
                                             showError(ex);
                                             return "";
                                         }
                                       }); break;
                case "Program":    cmp = Comparator.comparing(Student::getProgramCode); break;
                default:           cmp = Comparator.comparing(Student::getId);        break;
            }
            if (!sortAscending) cmp = cmp.reversed();

            List<Student> sorted = new ArrayList<>(filtered);
            sorted.sort(cmp);

            // 4) Pagination
            int total = sorted.size();
            totalPages = Math.max(1, (int)Math.ceil(total / (double)PAGE_SIZE));
            currentPage = Math.min(Math.max(currentPage, 1), totalPages);
            int start = (currentPage - 1) * PAGE_SIZE;
            List<Student> page = sorted.subList(start, Math.min(start + PAGE_SIZE, total));

            // 5) Update UI
            tableModel.setStudentList(page);
            studentCountLabel.setText("Total: " + total);
            pageField.setText(String.valueOf(currentPage));
            totalPagesLabel.setText(" of " + totalPages);
            prevBtn.setEnabled(currentPage > 1);
            nextBtn.setEnabled(currentPage < totalPages);
        } catch (Exception ex) {
            showError(ex);
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            pageField.setText("Err");
            totalPagesLabel.setText(" of ?");
        }
    }

    private void addStudent() {
        // Prevent adding a student if no colleges or programs are defined
        if (collegeBox.getItemCount() == 0 || programBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "You must define at least one college and one program before adding a student.",
                "Missing Data",
                JOptionPane.WARNING_MESSAGE
            );
            ensureCollegesAndProgramsExist();
            return;
        }
        try {
            String id = idField.getText().trim();
            if (!ID_PATTERN.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid ID format (YYYY-NNNN)");
            }
            Student s = new Student(
                id,
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                (String) yearLevelBox.getSelectedItem(),
                (String) genderBox.getSelectedItem(),
                (String) programBox.getSelectedItem()
            );
            studentDao.addStudent(s);
            loadAllStudents();
            refreshPage();
        } catch (SQLIntegrityConstraintViolationException dupEx) {
            JOptionPane.showMessageDialog(this,
                "A student with ID '" + idField.getText().trim() + "' already exists.",
                "Duplicate ID",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(this,
                iae.getMessage(),
                "Input Error",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (SQLException sqlEx) {
            showError(sqlEx);
        }
    }

    private void updateStudent() {
        int r = studentTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "No student selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Student old = tableModel.getStudentAt(r);
        try {
            String newId = idField.getText().trim();
            Student s = new Student(
                newId,
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                (String) yearLevelBox.getSelectedItem(),
                (String) genderBox.getSelectedItem(),
                (String) programBox.getSelectedItem()
            );
            studentDao.updateStudent(s);
            loadAllStudents();
            refreshPage();
        } catch (SQLException ex) {
            // Detect duplicate primary-key violation (MySQL error code 1062 or SQLState 23000)
            if (ex instanceof SQLIntegrityConstraintViolationException
                    || "23000".equals(ex.getSQLState())
                    || ex.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(
                    this,
                    "A student with ID '" + idField.getText().trim() + "' already exists.",
                    "Duplicate ID",
                    JOptionPane.ERROR_MESSAGE
                );
            } else {
                showError(ex);
            }
        } catch (IllegalArgumentException iae) {
            JOptionPane.showMessageDialog(
                this,
                iae.getMessage(),
                "Input Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteStudent() {
        int[] rows = studentTable.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "No student selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the selected students?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            List<String> idsToDelete = new ArrayList<>();
            for (int row : rows) {
                Student s = tableModel.getStudentAt(row);
                idsToDelete.add(s.getId());
            }
            studentDao.deleteStudentsBatch(idsToDelete);

            // reload & refresh
            loadAllStudents();
            refreshPage();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /**
     * Saves the form fields to the database.
     * If the ID already exists, performs update; otherwise inserts new record.
     */
    private void saveToDB() {
        try {
            String id = idField.getText().trim();
            if (!ID_PATTERN.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid ID format (YYYY-NNNN)");
            }
            Student s = new Student(
                id,
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                (String) yearLevelBox.getSelectedItem(),
                (String) genderBox.getSelectedItem(),
                (String) programBox.getSelectedItem()
            );
            boolean exists = studentList.stream().anyMatch(st -> st.getId().equals(id));
            if (exists) {
                studentDao.updateStudent(s);
            } else {
                studentDao.addStudent(s);
            }
            loadAllStudents();
            refreshPage();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /**
     * Saves every row currently in the table back to the database.
     * Inserts new students or updates existing ones.
     */
    private void saveAll() {
        try {
            for (Student s : tableModel.getStudentList()) {
                // upsert logic: if exists, update; else insert
                boolean exists = studentDao.getAllStudents()
                                           .stream()
                                           .anyMatch(db -> db.getId().equals(s.getId()));
                if (exists) {
                    studentDao.updateStudent(s);
                } else {
                    studentDao.addStudent(s);
                }
            }
            loadAllStudents();
            refreshPage();
            JOptionPane.showMessageDialog(this, "All rows saved successfully!");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /**
     * Ensures that at least one college and one program exist.
     * If either dropdown is empty, prompts the user to add entries via the managers.
     */
    private void ensureCollegesAndProgramsExist() {
        if (collegeBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "No colleges found. Please add at least one college.",
                "No Colleges Defined",
                JOptionPane.WARNING_MESSAGE
            );
            CollegeManager cm = new CollegeManager();
            cm.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    initLookup();
                    loadPrograms();
                    updateFilterPrograms();
                    refreshPage();
                    ensureCollegesAndProgramsExist();
                }
            });
            cm.setVisible(true);
            return;
        }
        if (programBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "No programs found. Please add at least one program.",
                "No Programs Defined",
                JOptionPane.WARNING_MESSAGE
            );
            ProgramManager pm = new ProgramManager();
            pm.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    loadPrograms();
                    updateFilterPrograms();
                    refreshPage();
                    ensureCollegesAndProgramsExist();
                }
            });
            pm.setVisible(true);
        }
    }

    // Removed applySearch, applySort, and applyFilter methods as their logic is now unified in refreshPage().

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    // Table model
    private class StudentTableModel extends AbstractTableModel {
        private List<Student> data;
        private final String[] cols = {"ID","First Name","Last Name","Year","Gender","College","Program"};
        StudentTableModel(List<Student> list) { data = list; }
        public int getRowCount() { return data.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            Student s = data.get(r);
            switch(c) {
                case 0: return s.getId();
                case 1: return s.getFirstName();
                case 2: return s.getLastName();
                case 3: return s.getYearLevel();
                case 4: return s.getGender();
                case 5:
                    // Display college via program lookup
                    try {
                        Program p = programDao.getProgramByCode(s.getProgramCode());
                        return p != null ? p.getCollegeCode() : "";
                    } catch (SQLException ex) {
                        showError(ex);
                        return "";
                    }
                case 6:
                    return s.getProgramCode();
                default:
                    return null;
            }
        }
        public void setStudentList(List<Student> list) { data = list; fireTableDataChanged(); }
        public Student getStudentAt(int r) { return data.get(r); }
        /** Returns the current list of students displayed in the table */
        public List<Student> getStudentList() {
            return data;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainGUI();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
