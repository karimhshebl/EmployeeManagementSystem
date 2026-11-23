package ems.service;

import ems.model.Employee;
import ems.model.EmployeeType;
import ems.model.FullTimeEmployee;
import ems.model.PartTimeEmployee;

/**
 * Creates the correct Employee subclass from text input.
 * It is used both by the GUI forms and by the file loader, so there is a single place
 * that validates data and knows which class belongs to which type.
 */
public final class EmployeeFactory {
    private EmployeeFactory() {
    }

    /**
     * @param salaryData the type specific inputs: [basic salary, bonus] for full-time,
     *                   [hourly rate, hours worked] for part-time
     */
    public static Employee create(EmployeeType type, String id, String fullName, String age,
                                  String department, String position, String... salaryData)
            throws ValidationException {
        if (type == null) {
            throw new ValidationException("Please select the employee type.");
        }
        int employeeId = Validator.parseId(id, "Employee ID");
        String name = Validator.requireText(fullName, "Full name");
        int employeeAge = Validator.parseAge(age);
        String dept = Validator.requireText(department, "Department");
        String job = Validator.requireText(position, "Position");

        switch (type) {
            case FULL_TIME:
                requireCount(salaryData, 2);
                return new FullTimeEmployee(employeeId, name, employeeAge, dept, job,
                        Validator.parseNonNegative(salaryData[0], "Basic salary"),
                        Validator.parseNonNegative(salaryData[1], "Bonus"));
            case PART_TIME:
                requireCount(salaryData, 2);
                return new PartTimeEmployee(employeeId, name, employeeAge, dept, job,
                        Validator.parseNonNegative(salaryData[0], "Hourly rate"),
                        Validator.parseNonNegative(salaryData[1], "Hours worked"));
            default:
                throw new ValidationException("Unsupported employee type: " + type);
        }
    }

    private static void requireCount(String[] salaryData, int expected) throws ValidationException {
        if (salaryData == null || salaryData.length != expected) {
            throw new ValidationException("Salary information is incomplete.");
        }
    }
}
