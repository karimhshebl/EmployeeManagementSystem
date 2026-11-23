package ems.model;

/** A company department: an ID and a name. */
public class Department {
    private int id;
    private String name;

    public Department(int id, String name) {
        setId(id);
        setName(name);
    }

    public int getId() {
        return id;
    }

    public final void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Department ID must be a positive integer.");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name must not be empty.");
        }
        this.name = name.trim();
    }

    @Override
    public String toString() {
        return name;
    }
}
