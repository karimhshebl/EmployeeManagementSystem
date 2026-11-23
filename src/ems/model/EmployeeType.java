package ems.model;

/**
 * The employee types supported by the system.
 * Adding a new type means adding a constant here, a new Employee subclass,
 * and one case in EmployeeFactory.
 */
public enum EmployeeType {
    FULL_TIME("Full-Time"),
    PART_TIME("Part-Time");

    private final String label;

    EmployeeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Parses the stored name (e.g. "FULL_TIME"), ignoring case. */
    public static EmployeeType fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Employee type is missing.");
        }
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported employee type: " + name);
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
