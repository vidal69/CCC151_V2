package main.java.model;


public class Student {
    private String id;
    private String firstName;
    private String lastName;
    private String yearLevel;
    private String gender;
    private String programCode;

    public Student(String id, String firstName, String lastName,
                   String yearLevel, String gender, String programCode) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.yearLevel = yearLevel;
        this.gender = gender;
        this.programCode = programCode;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getYearLevel() {
        return yearLevel;
    }
    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProgramCode() {
        return programCode;
    }
    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }
}
