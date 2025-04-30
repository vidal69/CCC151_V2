package main.java.app;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import main.java.app.StudentGenerator;

import main.java.dao.StudentDAO;
import main.java.dao.ProgramDAO;
import main.java.model.Student;
import main.java.model.Program;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Utility to generate and insert random student records.
 */
public class StudentGenerator {
    private final StudentDAO studentDao;
    private final ProgramDAO programDao;
    private final Random rand = new Random();

    // Sample name pools for random generation
    private static final String[] FIRST_NAMES = {
        "Alice", "Bob", "Carol", "David", "Eve",
        "Frank", "Grace", "Heidi", "Ivan", "Judy",
        "Oliver", "Emma", "Liam", "Olivia", "Noah",
        "Ava", "Elijah", "Charlotte", "William", "Sophia",
        "James", "Amelia", "Benjamin", "Isabella", "Lucas",
        "Mia", "Henry", "Harper", "Alexander", "Evelyn"
    };
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones",
        "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Lee", "Perez", "Thompson", "White",
        "Harris", "Martin", "Clark", "Lewis", "Robinson",
        "Walker", "Young", "Allen", "King", "Wright"
    };

    // Possible genders matching DB ENUM
    private static final String[] GENDERS = {"Male", "Female"};

    public StudentGenerator(StudentDAO studentDao, ProgramDAO programDao) {
        this.studentDao = studentDao;
        this.programDao = programDao;
    }

    /**
     * Generates and adds the specified number of students.
     */
    public void generate(int count) {
        try {
            // Load existing IDs to avoid duplicates
            Set<String> existingIds = new HashSet<>();
            for (Student s : studentDao.getAllStudents()) {
                existingIds.add(s.getId());
            }

            // Fetch available programs
            List<Program> programs = programDao.getAllPrograms();
            if (programs.isEmpty()) {
                throw new IllegalStateException("No programs available to assign.");
            }

            // Prepare batch of new students
            List<Student> batch = new ArrayList<>(count);
            while (batch.size() < count) {
                // Generate unique ID
                String id = String.format("%04d-%04d",
                        rand.nextInt(9000) + 1000,
                        rand.nextInt(9000) + 1000
                );
                if (existingIds.contains(id)) {
                    continue;  // skip duplicate
                }
                existingIds.add(id);

                // Random name
                String firstName = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)];
                String lastName  = LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
                String progCode  = programs.get(rand.nextInt(programs.size()))
                                            .getProgramCode();

                // Randomly choose a valid gender
                String gender = GENDERS[rand.nextInt(GENDERS.length)];

                // Randomly choose a year level between 1 and 4
                int year = rand.nextInt(4) + 1;
                String yearLevel = String.valueOf(year);

                batch.add(new Student(id, firstName, lastName, yearLevel, gender, progCode));
            }

            // Insert all in one go
            for (Student s : batch) {
                studentDao.addStudent(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
