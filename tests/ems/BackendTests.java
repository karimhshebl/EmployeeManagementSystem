package ems;

import ems.io.FileHandler;
import ems.model.Employee;
import ems.model.EmployeeType;
import ems.model.FullTimeEmployee;
import ems.model.PartTimeEmployee;
import ems.service.DepartmentManager;
import ems.service.EmployeeFactory;
import ems.service.EmployeeManager;
import ems.service.ValidationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Plain-Java tests of the backend (no framework needed). Run: java -cp out:testout ems.BackendTests */
public class BackendTests {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String name) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void rejects(String name, String expectedPart, Runnable0 action) {
        try {
            action.run();
            failed++;
            System.out.println("FAIL (no exception): " + name);
        } catch (ValidationException ex) {
            boolean ok = expectedPart == null || ex.getMessage().contains(expectedPart);
            if (ok) {
                passed++;
            } else {
                failed++;
                System.out.println("FAIL (wrong message): " + name + " -> " + ex.getMessage());
            }
        } catch (Exception ex) {
            failed++;
            System.out.println("FAIL (wrong exception): " + name + " -> " + ex);
        }
    }

    interface Runnable0 {
        void run() throws Exception;
    }

    private static Employee ft(int id, String name, String dept, String basic, String bonus) throws ValidationException {
        return EmployeeFactory.create(EmployeeType.FULL_TIME, "" + id, name, "30", dept, "Engineer", basic, bonus);
    }

    private static Employee pt(int id, String name, String dept, String rate, String hours) throws ValidationException {
        return EmployeeFactory.create(EmployeeType.PART_TIME, "" + id, name, "22", dept, "Tutor", rate, hours);
    }

    public static void main(String[] args) throws Exception {
        salaryTests();
        factoryValidationTests();
        managerTests();
        persistenceTests();
        corruptFileTests();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void salaryTests() throws Exception {
        Employee a = ft(1, "A", "IT", "15000", "2000");
        Employee b = pt(2, "B", "IT", "100", "80");
        check(a.calculateSalary() == 17000.0, "full-time = basic + bonus (spec example 17,000)");
        check(b.calculateSalary() == 8000.0, "part-time = rate x hours (spec example 8,000)");
        check(a instanceof FullTimeEmployee && b instanceof PartTimeEmployee, "factory creates right classes");
        Employee[] all = {a, b};
        double sum = 0;
        for (Employee e : all) {
            sum += e.calculateSalary(); // polymorphism through the base type
        }
        check(sum == 25000.0, "polymorphic salary through Employee reference");
        check(ft(3, "C", "IT", "0", "0").calculateSalary() == 0.0, "zero salary allowed");
        check(pt(4, "D", "IT", "12.5", "10.5").calculateSalary() == 131.25, "fractional hours/rate");
    }

    static void factoryValidationTests() {
        rejects("empty name", "Full name", () -> ft(1, " ", "IT", "1", "1"));
        rejects("empty position", "Position", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "30", "IT", "", "1", "1"));
        rejects("empty department", "Department", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "30", null, "P", "1", "1"));
        rejects("id letters", "Employee ID", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "x1", "A", "30", "IT", "P", "1", "1"));
        rejects("id decimal", "Employee ID", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1.5", "A", "30", "IT", "P", "1", "1"));
        rejects("id zero", "Employee ID", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "0", "A", "30", "IT", "P", "1", "1"));
        rejects("id negative", "Employee ID", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "-3", "A", "30", "IT", "P", "1", "1"));
        rejects("id overflow", "Employee ID", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "99999999999", "A", "30", "IT", "P", "1", "1"));
        rejects("age letters", "Age", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "abc", "IT", "P", "1", "1"));
        rejects("age zero", "Age", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "0", "IT", "P", "1", "1"));
        rejects("age negative", "Age", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "-5", "IT", "P", "1", "1"));
        rejects("age decimal", "Age", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "25.5", "IT", "P", "1", "1"));
        rejects("age too big", "Age", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "500", "IT", "P", "1", "1"));
        rejects("basic negative", "Basic salary", () -> ft(1, "A", "IT", "-1", "0"));
        rejects("bonus negative", "Bonus", () -> ft(1, "A", "IT", "1", "-1"));
        rejects("basic letters", "Basic salary", () -> ft(1, "A", "IT", "abc", "0"));
        rejects("basic NaN", "Basic salary", () -> ft(1, "A", "IT", "NaN", "0"));
        rejects("basic Infinity", "Basic salary", () -> ft(1, "A", "IT", "Infinity", "0"));
        rejects("basic 12d suffix", "Basic salary", () -> ft(1, "A", "IT", "12d", "0"));
        rejects("basic huge", "Basic salary", () -> ft(1, "A", "IT", "1e999", "0"));
        rejects("basic empty", "Basic salary", () -> ft(1, "A", "IT", "", "0"));
        rejects("rate negative", "Hourly rate", () -> pt(1, "A", "IT", "-5", "10"));
        rejects("hours negative", "Hours worked", () -> pt(1, "A", "IT", "5", "-10"));
        rejects("hours empty", "Hours worked", () -> pt(1, "A", "IT", "5", " "));
        rejects("no type", "employee type", () -> EmployeeFactory.create(null, "1", "A", "30", "IT", "P", "1", "1"));
        rejects("missing salary data", "incomplete", () -> EmployeeFactory.create(EmployeeType.FULL_TIME, "1", "A", "30", "IT", "P", "1"));
        try {
            Employee e = ft(1, "  Ali  ", "IT", " 100 ", "5e1");
            check(e.getFullName().equals("Ali"), "name trimmed");
            check(e.calculateSalary() == 150.0, "whitespace and exponent numbers accepted");
        } catch (Exception ex) {
            check(false, "valid input rejected: " + ex);
        }
    }

    private static Path tempDir() throws IOException {
        return Files.createTempDirectory("ems-test");
    }

    static void managerTests() throws Exception {
        Path dir = tempDir();
        FileHandler fh = new FileHandler(dir);
        DepartmentManager dm = new DepartmentManager(fh);
        EmployeeManager em = new EmployeeManager(fh, dm);

        check(dm.getAll().size() == 5, "five default departments on first launch");
        check(dm.findByName("it") != null, "department lookup ignores case");
        check(em.count() == 0, "starts with no employees when file is missing");
        check(em.nextId() == 1, "first suggested id is 1");

        em.add(ft(1, "Alice Smith", "IT", "15000", "2000"));
        em.add(pt(2, "Bob", "finance", "100", "80")); // lower-case department -> canonical
        check(em.count() == 2, "two employees added");
        check(em.findById(2).getDepartment().equals("Finance"), "department stored with canonical spelling");
        check(em.getTotalPayroll() == 25000.0, "total payroll");
        check(em.nextId() == 3, "next id after 2 is 3");

        rejects("duplicate employee id", "already exists", () -> em.add(ft(1, "Dup", "IT", "1", "1")));
        rejects("unknown department", "does not exist", () -> em.add(ft(9, "X", "Astronomy", "1", "1")));
        check(em.count() == 2, "rejected adds do not change data");

        // update
        em.update(1, ft(1, "Alice Smith-Jones", "Sales", "16000", "1000"));
        check(em.findById(1).getFullName().equals("Alice Smith-Jones"), "update name");
        check(em.findById(1).getDepartment().equals("Sales"), "update department");
        check(em.count() == 2, "update keeps count");
        em.update(1, pt(1, "Alice Smith-Jones", "Sales", "50", "100")); // change type
        check(em.findById(1) instanceof PartTimeEmployee && em.findById(1).calculateSalary() == 5000.0, "update can change type");
        em.update(2, pt(7, "Bob", "Finance", "100", "80")); // change id to a free one
        check(em.findById(2) == null && em.findById(7) != null, "id can change to a free id");
        rejects("update to duplicate id", "already exists", () -> em.update(7, pt(1, "Bob", "Finance", "100", "80")));
        check(em.findById(7) != null && em.findById(7).getFullName().equals("Bob"), "failed update leaves data untouched");
        rejects("update missing employee", "no longer exists", () -> em.update(99, pt(99, "Z", "IT", "1", "1")));

        // search
        check(em.searchById("7").size() == 1, "search by id");
        check(em.searchById("123").isEmpty(), "search by id no match");
        rejects("search id letters", "Employee ID", () -> em.searchById("abc"));
        check(em.searchByName("alice").size() == 1, "search name case-insensitive");
        check(em.searchByName("zzz").isEmpty(), "search name no match");
        check(em.searchByDepartment("SALES").size() == 1, "search department case-insensitive");
        check(em.searchByDepartment("fin").size() == 1, "search department partial");
        rejects("empty search", "required", () -> em.searchByName("  "));
        check(em.filterByDepartment("finance").size() == 1, "filter by department");

        // department rules
        rejects("dup dept name ignoring case", "already exists", () -> dm.add("50", "it"));
        rejects("dup dept id", "already exists", () -> dm.add("1", "Legal"));
        rejects("dept id letters", "Department ID", () -> dm.add("x", "Legal"));
        rejects("dept empty name", "Department name", () -> dm.add("50", " "));
        dm.add("50", "Legal");
        check(dm.findByName("LEGAL") != null, "department added");
        rejects("delete department in use", "assigned", () -> em.deleteDepartment(dm.findByName("Finance").getId()));
        em.deleteDepartment(50);
        check(dm.findByName("Legal") == null, "unused department deleted");

        // delete
        em.delete(7);
        check(em.findById(7) == null && em.count() == 1, "delete");
        rejects("delete missing", "no longer exists", () -> em.delete(7));
    }

    static void persistenceTests() throws Exception {
        Path dir = tempDir();
        {
            FileHandler fh = new FileHandler(dir);
            DepartmentManager dm = new DepartmentManager(fh);
            EmployeeManager em = new EmployeeManager(fh, dm);
            dm.add("10", "Research, Development & \"R&D\"");
            em.add(ft(1, "Smith, John \"Johnny\"", "IT", "15000.5", "2000"));
            em.add(pt(2, "Åsa Müller محمد", "Research, Development & \"R&D\"", "100.25", "80.5"));
            em.add(ft(3, "Zero", "Sales", "0", "0"));
            em.update(3, ft(3, "Zero Two", "Sales", "1234567890.12", "0.01"));
            em.add(pt(4, "ToDelete", "IT", "1", "1"));
            em.delete(4);
        }
        // "restart": brand new objects reading the same files
        FileHandler fh = new FileHandler(dir);
        DepartmentManager dm = new DepartmentManager(fh);
        EmployeeManager em = new EmployeeManager(fh, dm);
        check(em.getLoadWarnings().isEmpty() && dm.getLoadWarnings().isEmpty(), "clean reload has no warnings");
        check(em.count() == 3, "three employees reloaded (deleted one gone)");
        Employee e1 = em.findById(1);
        Employee e2 = em.findById(2);
        Employee e3 = em.findById(3);
        check(e1 instanceof FullTimeEmployee && e1.getFullName().equals("Smith, John \"Johnny\""), "commas and quotes survive");
        check(e1.calculateSalary() == 17000.5, "full-time salary survives");
        check(e2 instanceof PartTimeEmployee && e2.getFullName().equals("Åsa Müller محمد"), "unicode survives");
        check(e2.calculateSalary() == 100.25 * 80.5, "part-time salary survives");
        check(e2.getDepartment().equals("Research, Development & \"R&D\""), "department with comma/quote survives");
        check(((FullTimeEmployee) e3).getBasicSalary() == 1234567890.12 && ((FullTimeEmployee) e3).getBonus() == 0.01, "large/small decimals survive");
        check(dm.findByName("research, development & \"r&d\"") != null, "custom department reloaded");
        List<String> lines = Files.readAllLines(fh.getEmployeesFile(), StandardCharsets.UTF_8);
        check(lines.get(0).startsWith("type,id,"), "csv header written");
        check(lines.size() == 4, "header + 3 rows");
        check(!Files.exists(dir.resolve("employees.csv.tmp")), "no temp file left over");

        // department file not recreated with defaults when it already exists but user removed one
        dm.add("77", "Temp");
        DepartmentManager dm2 = new DepartmentManager(new FileHandler(dir));
        check(dm2.findByName("Temp") != null && dm2.getAll().size() == 7, "departments persisted (5 + R&D + Temp)");
    }

    static void corruptFileTests() throws Exception {
        Path dir = tempDir();
        Files.write(dir.resolve("departments.csv"), List.of("id,name", "1,IT", "oops", "2,IT", "1,Dup", "3,Sales"), StandardCharsets.UTF_8);
        Files.write(dir.resolve("employees.csv"), List.of(
                "type,id,fullName,age,department,position,salary1,salary2",
                "FULL_TIME,1,Good One,30,IT,Dev,1000,100",
                "FULL_TIME,2,Bad Salary,30,IT,Dev,abc,100",
                "PART_TIME,3,Good Two,25,Sales,Tutor,50,10",
                "ROBOT,4,Bad Type,30,IT,Dev,1,1",
                "FULL_TIME,5,Too Few,30,IT",
                "FULL_TIME,1,Dup Id,31,IT,Dev,5,5",
                "FULL_TIME,6,\"Unclosed,30,IT,Dev,1,1",
                "FULL_TIME,7,New Dept Guy,40,Legal,Counsel,9000,0",
                "FULL_TIME,8,Lower Case Dept,40,it,Dev,1,1",
                "",
                "FULL_TIME,9,Negative,40,IT,Dev,-1,1"), StandardCharsets.UTF_8);
        FileHandler fh = new FileHandler(dir);
        DepartmentManager dm = new DepartmentManager(fh);
        EmployeeManager em = new EmployeeManager(fh, dm);
        check(em.count() == 4, "only valid employees loaded (1,3,7,8) but got " + em.count());
        check(em.findById(1) != null && em.findById(3) != null && em.findById(7) != null && em.findById(8) != null, "right ones loaded");
        check(em.findById(8).getDepartment().equals("IT"), "dept spelling canonicalised on load");
        check(dm.findByName("Legal") != null, "missing department auto-registered so employee is not lost");
        check(!em.getLoadWarnings().isEmpty() && !dm.getLoadWarnings().isEmpty(), "warnings reported");
        check(Files.exists(dir.resolve("employees.csv.bak")), "backup of damaged file kept");
        check(dm.getAll().size() == 3, "dept duplicates skipped (IT, Sales, Legal)");

        // unreadable (not valid UTF-8) file must not crash
        Path dir2 = tempDir();
        Files.write(dir2.resolve("employees.csv"), new byte[]{(byte) 0xff, (byte) 0xfe, (byte) 0xfd, '\n'});
        EmployeeManager em2 = new EmployeeManager(new FileHandler(dir2), new DepartmentManager(new FileHandler(dir2)));
        check(em2.count() == 0, "garbage file handled without crash");

        // directory that cannot be written -> IOException, memory rolled back
        Path dir3 = tempDir();
        FileHandler fh3 = new FileHandler(dir3);
        DepartmentManager dm3 = new DepartmentManager(fh3);
        EmployeeManager em3 = new EmployeeManager(fh3, dm3);
        em3.add(ft(1, "A", "IT", "1", "1"));
        Files.deleteIfExists(dir3.resolve("employees.csv"));
        Files.createDirectory(dir3.resolve("employees.csv")); // a directory where the file should be
        Files.createFile(dir3.resolve("employees.csv").resolve("block")); // make it non-empty so move fails
        try {
            em3.add(ft(2, "B", "IT", "1", "1"));
            check(false, "save failure should throw IOException");
        } catch (IOException expected) {
            check(em3.count() == 1 && em3.findById(2) == null, "failed save rolled back in memory");
        }
    }
}
