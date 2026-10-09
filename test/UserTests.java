import java.lang.reflect.Modifier;
import model.Staff;
import model.Student;
import model.User;

/**
 * Acceptance tests for F3: User hierarchy (User, Student, Staff).
 */
public class UserTests {

    public static void run() {
        System.out.println("--- Running F3 User Hierarchy Tests ---");

        // 1. User class cannot be instantiated directly (is abstract)
        TestRunner.check("User class is abstract", Modifier.isAbstract(User.class.getModifiers()));

        // 2. Blank and null names are rejected
        TestRunner.assertThrows("Student rejects null name", IllegalArgumentException.class, () -> {
            new Student(null, "contact@campus.edu", "S1001");
        });
        TestRunner.assertThrows("Student rejects empty name", IllegalArgumentException.class, () -> {
            new Student("", "contact@campus.edu", "S1001");
        });
        TestRunner.assertThrows("Staff rejects blank name with whitespace", IllegalArgumentException.class, () -> {
            new Staff("   ", "staff@campus.edu", "ST2001");
        });

        // 3. Valid construction
        Student studentWithId = new Student("Alice Johnson", "alice@test.edu", "STU-001");
        TestRunner.assertEquals("Student name parsed", "Alice Johnson", studentWithId.getName());
        TestRunner.assertEquals("Student contact parsed", "alice@test.edu", studentWithId.getContact());
        TestRunner.assertEquals("Student ID parsed", "STU-001", studentWithId.getStudentId());

        Staff staffWithId = new Staff("Officer Bob", "bob@security.edu", "EMP-909");
        TestRunner.assertEquals("Staff name parsed", "Officer Bob", staffWithId.getName());
        TestRunner.assertEquals("Staff contact parsed", "bob@security.edu", staffWithId.getContact());
        TestRunner.assertEquals("Staff ID parsed", "EMP-909", staffWithId.getStaffId());

        // 4. Polymorphic calls through User reference
        User user1 = studentWithId;
        User user2 = staffWithId;
        TestRunner.assertEquals("Polymorphic displayRole for Student with ID", "Student (ID: STU-001)", user1.displayRole());
        TestRunner.assertEquals("Polymorphic displayRole for Staff with ID", "Staff (ID: EMP-909)", user2.displayRole());
        TestRunner.check("displayRole returns different text for different user subtypes", !user1.displayRole().equals(user2.displayRole()));

        // 5. Optional ID variants
        User studentNoId = new Student("Charlie Brown", "charlie@test.edu");
        User staffNoId = new Staff("David Lee", "david@test.edu");
        TestRunner.assertEquals("Student without ID displayRole", "Student", studentNoId.displayRole());
        TestRunner.assertEquals("Staff without ID displayRole", "Staff", staffNoId.displayRole());
    }
}
