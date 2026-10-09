package model;

import java.util.Objects;

/**
 * Student user representation.
 */
public class Student extends User {
    private final String studentId;

    public Student(String name, String contact, String studentId) {
        super(name, contact);
        this.studentId = (studentId != null) ? studentId.trim() : "";
    }

    public Student(String name, String contact) {
        this(name, contact, "");
    }

    public String getStudentId() {
        return studentId;
    }

    @Override
    public String displayRole() {
        if (!studentId.isEmpty()) {
            return "Student (ID: " + studentId + ")";
        }
        return "Student";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        if (!super.equals(o)) return false;
        Student student = (Student) o;
        return Objects.equals(studentId, student.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), studentId);
    }
}
