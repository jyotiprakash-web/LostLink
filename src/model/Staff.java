package model;

import java.util.Objects;

/**
 * Staff / operator user representation.
 */
public class Staff extends User {
    private final String staffId;

    public Staff(String name, String contact, String staffId) {
        super(name, contact);
        this.staffId = (staffId != null) ? staffId.trim() : "";
    }

    public Staff(String name, String contact) {
        this(name, contact, "");
    }

    public String getStaffId() {
        return staffId;
    }

    @Override
    public String displayRole() {
        if (!staffId.isEmpty()) {
            return "Staff (ID: " + staffId + ")";
        }
        return "Staff";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Staff)) return false;
        if (!super.equals(o)) return false;
        Staff staff = (Staff) o;
        return Objects.equals(staffId, staff.staffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), staffId);
    }
}
