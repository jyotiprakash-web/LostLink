package model;

import java.util.Objects;

/**
 * Abstract base class representing a person reporting or managing lost/found items.
 */
public abstract class User {
    private final String name;
    private final String contact;

    public User(String name, String contact) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank.");
        }
        this.name = name.trim();
        this.contact = (contact != null) ? contact.trim() : "";
    }

    public String getName() {
        return name;
    }

    public String getContact() {
        return contact;
    }

    /**
     * Polymorphic display string representing the user's role.
     */
    public abstract String displayRole();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(name, user.name) && Objects.equals(contact, user.contact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, contact);
    }

    @Override
    public String toString() {
        return name + " [" + displayRole() + "] - Contact: " + contact;
    }
}
