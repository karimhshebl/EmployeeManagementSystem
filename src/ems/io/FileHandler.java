package ems.io;

import ems.model.Department;
import ems.model.Employee;
import ems.model.EmployeeType;
import ems.service.EmployeeFactory;
import ems.service.ValidationException;
import ems.service.Validator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads and writes the application data as CSV text files.
 *
 * employees.csv   : type,id,fullName,age,department,position,salary1,salary2
 *                   FULL_TIME -> salary1 = basic salary, salary2 = bonus
 *                   PART_TIME -> salary1 = hourly rate,  salary2 = hours worked
 * departments.csv : id,name
 *
 * The type column lets every employee object (and its type specific data) be rebuilt exactly.
 * Files are written to a temporary file first and then moved over the real file, so a failure
 * while writing can never leave a half-written data file behind.
 */
public class FileHandler {
    private static final String EMPLOYEE_HEADER = "type,id,fullName,age,department,position,salary1,salary2";
    private static final String DEPARTMENT_HEADER = "id,name";

    private final Path employeesFile;
    private final Path departmentsFile;

    public FileHandler(Path dataDirectory) {
        this.employeesFile = dataDirectory.resolve("employees.csv");
        this.departmentsFile = dataDirectory.resolve("departments.csv");
    }

    public Path getEmployeesFile() {
        return employeesFile;
    }

    public Path getDepartmentsFile() {
        return departmentsFile;
    }

    public boolean departmentsFileExists() {
        return Files.exists(departmentsFile);
    }

    // ------------------------------------------------------------------ departments

    /** A missing file gives an empty list. Unreadable lines are skipped and reported in warnings. */
    public List<Department> loadDepartments(List<String> warnings) throws IOException {
        List<Department> result = new ArrayList<>();
        List<String> lines = readLines(departmentsFile);
        int skipped = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty() || (i == 0 && line.equalsIgnoreCase(DEPARTMENT_HEADER))) {
                continue;
            }
            try {
                List<String> f = CsvUtil.split(line);
                if (f.size() != 2) {
                    throw new IllegalArgumentException("expected 2 fields");
                }
                result.add(new Department(Validator.parseId(f.get(0), "Department ID"),
                        Validator.requireText(f.get(1), "Department name")));
            } catch (IllegalArgumentException | ValidationException ex) {
                skipped++;
                warnings.add("departments.csv line " + (i + 1) + " skipped: " + ex.getMessage());
            }
        }
        backupIfSkipped(departmentsFile, skipped);
        return result;
    }

    public void saveDepartments(Collection<Department> departments) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(DEPARTMENT_HEADER);
        for (Department d : departments) {
            lines.add(CsvUtil.join(String.valueOf(d.getId()), d.getName()));
        }
        writeLines(departmentsFile, lines);
    }

    // ------------------------------------------------------------------ employees

    /** A missing file gives an empty list. Unreadable lines are skipped and reported in warnings. */
    public List<Employee> loadEmployees(List<String> warnings) throws IOException {
        List<Employee> result = new ArrayList<>();
        List<String> lines = readLines(employeesFile);
        int skipped = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty() || (i == 0 && line.equalsIgnoreCase(EMPLOYEE_HEADER))) {
                continue;
            }
            try {
                List<String> f = CsvUtil.split(line);
                if (f.size() < 8) {
                    throw new IllegalArgumentException("expected 8 fields but found " + f.size());
                }
                EmployeeType type = EmployeeType.fromName(f.get(0));
                String[] salaryData = f.subList(6, f.size()).toArray(new String[0]);
                result.add(EmployeeFactory.create(type, f.get(1), f.get(2), f.get(3),
                        f.get(4), f.get(5), salaryData));
            } catch (IllegalArgumentException | ValidationException ex) {
                skipped++;
                warnings.add("employees.csv line " + (i + 1) + " skipped: " + ex.getMessage());
            }
        }
        backupIfSkipped(employeesFile, skipped);
        return result;
    }

    public void saveEmployees(Collection<Employee> employees) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(EMPLOYEE_HEADER);
        for (Employee e : employees) {
            String[] salary = e.getSalaryData();
            lines.add(CsvUtil.join(e.getType().name(), String.valueOf(e.getId()), e.getFullName(),
                    String.valueOf(e.getAge()), e.getDepartment(), e.getPosition(),
                    salary[0], salary[1]));
        }
        writeLines(employeesFile, lines);
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>(); // first launch: start empty
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!lines.isEmpty() && lines.get(0).startsWith("﻿")) {
            lines.set(0, lines.get(0).substring(1)); // BOM added by some editors
        }
        return lines;
    }

    private static void writeLines(Path file, List<String> lines) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path temp = dir.resolve(file.getFileName() + ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** Keeps a copy of a file that had bad lines, because the next save would drop those lines. */
    private static void backupIfSkipped(Path file, int skipped) {
        if (skipped == 0) {
            return;
        }
        try {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // best effort only
        }
    }
}
