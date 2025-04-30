package main.java.model;

/**
 * Model class representing a College entity.
 */
public class College {
    private String collegeCode;
    private String collegeName;

    /**
     * Constructs a College with the specified code and name.
     *
     * @param collegeCode the unique code of the college
     * @param collegeName the human-readable name of the college
     */
    public College(String collegeCode, String collegeName) {
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    @Override
    public String toString() {
        return String.format("College{code='%s', name='%s'}", collegeCode, collegeName);
    }
}
