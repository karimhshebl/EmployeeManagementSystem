package ems.service;

import ems.io.FileHandler;
import ems.model.Department;
import ems.model.Employee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Business logic for employees: add, update, delete, search, filter and payroll.
 * Every change is saved to the file immediately. If saving fails, the change is undone in memory,
 * so the program never shows data that is not stored.
 */
public final class EmployeeManager {
    private final FileHandler fileHandler;
    private final DepartmentManager departmentManager;
    private final List<Employee> employees = new ArrayList<>();
    private final List<String> loadWarnings = new ArrayList<>();

    public EmployeeManager(FileHandler fileHandler, DepartmentManager departmentManager) {
        this.fileHandler = fileHandler;
        this.departmentManager = departmentManager;
        load();
    }

    private void load() {
        List<Employee> loaded = new ArrayList<>();
        try {
            loaded = fileHandler.loadEmployees(loadWarnings);
        } catch (IOException ex) {
            loadWarnings.add("Could not read the employees file: " + ex.getMessage());
        }
        for (Employee e : loaded) {
            if (findById(e.getId()) != null) {
                loadWarnings.add("Employee with duplicate ID " + e.getId() + " skipped.");
                continue;
            }
            Department department = departmentManager.findByName(e.getDepartment());
            if (department == null) {
                department = departmentManager.addMissing(e.getDepartment());
            }
            e.setDepartment(department.getName());
            employees.add(e);
        }
    }

    public DepartmentManager getDepartmentManager() {
        return departmentManager;
    }

    /** Problems found while loading the file (shown once at start-up). */
    public List<String> getLoadWarnings() {
        return loadWarnings;
    }

    // ------------------------------------------------------------------ queries

    public List<Employee> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(employees));
    }

    public int count() {
        return employees.size();
    }

    public Employee findById(int id) {
        for (Employee e : employees) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    /** The next free employee ID (used to pre-fill the form). */
    public int nextId() {
        int max = 0;
        for (Employee e : employees) {
            max = Math.max(max, e.getId());
        }
        return max + 1;
    }

    /** Sum of every employee's monthly salary (each calculated by its own type). */
    public double getTotalPayroll() {
        double total = 0;
        for (Employee e : employees) {
            total += e.calculateSalary();
        }
        return total;
    }

    public int countInDepartment(String departmentName) {
        int count = 0;
        for (Employee e : employees) {
            if (e.getDepartment().equalsIgnoreCase(departmentName)) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ search / filter

    public List<Employee> searchById(String text) throws ValidationException {
        int id = Validator.parseId(text, "Employee ID");
        List<Employee> result = new ArrayList<>();
        Employee found = findById(id);
        if (found != null) {
            result.add(found);
        }
        return result;
    }

    /** Case-insensitive "contains" search on the full name. */
    public List<Employee> searchByName(String text) throws ValidationException {
        String needle = Validator.requireText(text, "Search text").toLowerCase(Locale.ROOT);
        List<Employee> result = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getFullName().toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(e);
            }
        }
        return result;
    }

    /** Case-insensitive "contains" search on the department name. */
    public List<Employee> searchByDepartment(String text) throws ValidationException {
        String needle = Validator.requireText(text, "Search text").toLowerCase(Locale.ROOT);
        List<Employee> result = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getDepartment().toLowerCase(Locale.ROOT).contains(needle)) {
                result.add(e);
            }
        }
        return result;
    }

    /** Exact (case-insensitive) department filter, used by the department combo box. */
    public List<Employee> filterByDepartment(String departmentName) {
        List<Employee> result = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getDepartment().equalsIgnoreCase(departmentName)) {
                result.add(e);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ changes

    public void add(Employee employee) throws ValidationException, IOException {
        useCanonicalDepartment(employee);
        if (findById(employee.getId()) != null) {
            throw new ValidationException("Employee ID " + employee.getId() + " already exists.");
        }
        employees.add(employee);
        try {
            fileHandler.saveEmployees(employees);
        } catch (IOException ex) {
            employees.remove(employee);
            throw ex;
        }
    }

    /**
     * Replaces the employee that currently has originalId with the updated one.
     * The ID may be changed as long as it does not clash with another employee.
     */
    public void update(int originalId, Employee updated) throws ValidationException, IOException {
        Employee original = findById(originalId);
        if (original == null) {
            throw new ValidationException("The selected employee no longer exists.");
        }
        useCanonicalDepartment(updated);
        if (updated.getId() != originalId && findById(updated.getId()) != null) {
            throw new ValidationException("Employee ID " + updated.getId() + " already exists.");
        }
        int index = employees.indexOf(original);
        employees.set(index, updated);
        try {
            fileHandler.saveEmployees(employees);
        } catch (IOException ex) {
            employees.set(index, original);
            throw ex;
        }
    }

    public void delete(int id) throws ValidationException, IOException {
        Employee employee = findById(id);
        if (employee == null) {
            throw new ValidationException("The selected employee no longer exists.");
        }
        int index = employees.indexOf(employee);
        employees.remove(index);
        try {
            fileHandler.saveEmployees(employees);
        } catch (IOException ex) {
            employees.add(index, employee);
            throw ex;
        }
    }

    /** A department can only be deleted when no employee is assigned to it. */
    public void deleteDepartment(int departmentId) throws ValidationException, IOException {
        Department department = departmentManager.findById(departmentId);
        if (department == null) {
            throw new ValidationException("Department not found.");
        }
        int assigned = countInDepartment(department.getName());
        if (assigned > 0) {
            throw new ValidationException("Cannot delete \"" + department.getName() + "\": "
                    + assigned + " employee(s) are assigned to it.\n"
                    + "Move or delete those employees first.");
        }
        departmentManager.remove(departmentId);
    }

    /** The department must exist in the department list; its stored spelling is used. */
    private void useCanonicalDepartment(Employee employee) throws ValidationException {
        Department department = departmentManager.findByName(employee.getDepartment());
        if (department == null) {
            throw new ValidationException("Department \"" + employee.getDepartment()
                    + "\" does not exist. Please choose a department from the list.");
        }
        employee.setDepartment(department.getName());
    }
}
