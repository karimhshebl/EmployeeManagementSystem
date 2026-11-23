package ems.model;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Abstract base class for every employee (ABSTRACTION).
 * Common data is kept private and protected by validating setters (ENCAPSULATION).
 * Subclasses supply their own salary rules (POLYMORPHISM) and inherit all the
 * common behaviour (INHERITANCE).
 */
public abstract class Employee {
    public static final int MAX_AGE = 120;

    private int id;
    private String fullName;
    private int age;
    private String department;
    private String position;

    protected Employee(int id, String fullName, int age, String department, String position) {
        setId(id);
        setFullName(fullName);
        setAge(age);
        setDepartment(department);
        setPosition(position);
    }

    // ---------- getters / validating setters (encapsulation) ----------

    public final int getId() {
        return id;
    }

    public final void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee ID must be a positive integer.");
        }
        this.id = id;
    }

    public final String getFullName() {
        return fullName;
    }

    public final void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name must not be empty.");
        }
        this.fullName = fullName.trim();
    }

    public final int getAge() {
        return age;
    }

    public final void setAge(int age) {
        if (age <= 0 || age > MAX_AGE) {
            throw new IllegalArgumentException("Age must be between 1 and " + MAX_AGE + ".");
        }
        this.age = age;
    }

    public final String getDepartment() {
        return department;
    }

    public final void setDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department must not be empty.");
        }
        this.department = department.trim();
    }

    public final String getPosition() {
        return position;
    }

    public final void setPosition(String position) {
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position must not be empty.");
        }
        this.position = position.trim();
    }

    // ---------- abstract behaviour (each subclass decides) ----------

    /** The type of this employee. */
    public abstract EmployeeType getType();

    /** Monthly salary, calculated with the rule of the actual employee type. */
    public abstract double calculateSalary();

    /** Human readable description of the salary inputs, e.g. "Basic 15,000.00 + Bonus 2,000.00". */
    public abstract String getSalaryDetails();

    /** The type specific salary inputs as plain strings, used for the file and for the edit form. */
    public abstract String[] getSalaryData();

    // ---------- helpers for subclasses ----------

    protected static double requireNonNegative(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(field + " must be a non-negative number.");
        }
        return value;
    }

    /** Number as plain text without exponent or useless trailing zeros (15000.0 -> "15000"). */
    protected static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /** Money with thousands separator and two decimals, independent of the computer's locale. */
    protected static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    @Override
    public String toString() {
        return "#" + id + " " + fullName + " (" + getType() + ")";
    }
}
