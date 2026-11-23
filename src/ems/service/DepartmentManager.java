package ems.service;

import ems.io.FileHandler;
import ems.model.Department;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Manages the departments and keeps departments.csv up to date. */
public final class DepartmentManager {
    private static final String[] DEFAULT_DEPARTMENTS =
            {"IT", "Human Resources", "Finance", "Marketing", "Sales"};

    private final FileHandler fileHandler;
    private final List<Department> departments = new ArrayList<>();
    private final List<String> loadWarnings = new ArrayList<>();

    public DepartmentManager(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
        load();
    }

    private void load() {
        List<Department> loaded = new ArrayList<>();
        try {
            loaded = fileHandler.loadDepartments(loadWarnings);
        } catch (IOException ex) {
            loadWarnings.add("Could not read the departments file: " + ex.getMessage());
        }
        for (Department d : loaded) {
            if (findById(d.getId()) != null || findByName(d.getName()) != null) {
                loadWarnings.add("Duplicate department skipped: " + d.getId() + " / " + d.getName());
            } else {
                departments.add(d);
            }
        }
        // First launch (no file yet): start with the departments named in the lab description.
        if (!fileHandler.departmentsFileExists()) {
            int id = 1;
            for (String name : DEFAULT_DEPARTMENTS) {
                departments.add(new Department(id++, name));
            }
            try {
                fileHandler.saveDepartments(departments);
            } catch (IOException ex) {
                loadWarnings.add("Could not create the departments file: " + ex.getMessage());
            }
        }
    }

    /** Problems found while loading the file (shown once at start-up). */
    public List<String> getLoadWarnings() {
        return loadWarnings;
    }

    public List<Department> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(departments));
    }

    public Department findById(int id) {
        for (Department d : departments) {
            if (d.getId() == id) {
                return d;
            }
        }
        return null;
    }

    /** Case-insensitive lookup. */
    public Department findByName(String name) {
        if (name == null) {
            return null;
        }
        for (Department d : departments) {
            if (d.getName().equalsIgnoreCase(name.trim())) {
                return d;
            }
        }
        return null;
    }

    /** The next free department ID (used to pre-fill the form). */
    public int nextId() {
        int max = 0;
        for (Department d : departments) {
            max = Math.max(max, d.getId());
        }
        return max + 1;
    }

    /** Adds a department typed by the user. Rejects duplicate IDs and duplicate names (ignoring case). */
    public Department add(String idText, String nameText) throws ValidationException, IOException {
        int id = Validator.parseId(idText, "Department ID");
        String name = Validator.requireText(nameText, "Department name");
        if (findById(id) != null) {
            throw new ValidationException("Department ID " + id + " already exists.");
        }
        if (findByName(name) != null) {
            throw new ValidationException("A department named \"" + findByName(name).getName()
                    + "\" already exists.");
        }
        Department department = new Department(id, name);
        departments.add(department);
        try {
            fileHandler.saveDepartments(departments);
        } catch (IOException ex) {
            departments.remove(department); // keep memory and file consistent
            throw ex;
        }
        return department;
    }

    /**
     * Registers a department found in an employee record that is missing from the department file,
     * so that no stored employee is lost. Used while loading only.
     */
    Department addMissing(String name) {
        Department department = new Department(nextId(), name);
        departments.add(department);
        try {
            fileHandler.saveDepartments(departments);
        } catch (IOException ex) {
            loadWarnings.add("Could not save the departments file: " + ex.getMessage());
        }
        return department;
    }

    /** Removes a department. EmployeeManager.deleteDepartment checks that nobody uses it first. */
    public void remove(int id) throws ValidationException, IOException {
        Department department = findById(id);
        if (department == null) {
            throw new ValidationException("Department not found.");
        }
        int index = departments.indexOf(department);
        departments.remove(index);
        try {
            fileHandler.saveDepartments(departments);
        } catch (IOException ex) {
            departments.add(index, department);
            throw ex;
        }
    }
}
