package main.java.model;

public class Program {
    private String programCode;
    private String programName;
    private String collegeCode;

    public Program(String programCode, String programName, String collegeCode) {
        this.programCode = programCode;
        this.programName = programName;
        this.collegeCode = collegeCode;
    }

    public String getProgramCode() {
        return programCode;
    }
    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public String getProgramName() {
        return programName;
    }
    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getCollegeCode() {
        return collegeCode;
    }
    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }
}
