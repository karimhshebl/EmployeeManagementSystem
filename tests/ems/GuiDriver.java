package ems;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * End-to-end test that operates the REAL application window (finds components by name, types, clicks)
 * and saves screenshots. Run under a virtual display (xvfb-run) from an empty working directory.
 * Usage: GuiDriver phase1 <screenshotDir>   |   GuiDriver phase2 <screenshotDir>
 */
public class GuiDriver {
    static int passed = 0;
    static int failed = 0;
    static File shots;
    static Robot robot;

    static void check(boolean ok, String name) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }

    // ------------------------------------------------------------ component helpers

    static <T extends Component> T find(Container root, String name, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (!c.isVisible()) {
                continue; // components of hidden cards (other panels) must not be found
            }
            if (name.equals(c.getName()) && type.isInstance(c)) {
                return type.cast(c);
            }
            if (c instanceof Container) {
                T found = find((Container) c, name, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static Window visibleWindow(String className) {
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.getClass().getSimpleName().equals(className)) {
                return w;
            }
        }
        return null;
    }

    static Window waitWindow(String className) throws Exception {
        for (int i = 0; i < 100; i++) {
            Window w = visibleWindow(className);
            if (w != null) {
                Thread.sleep(300);
                return w;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("window not shown: " + className);
    }

    static <T extends Component> T need(Window w, String name, Class<T> type) {
        T c = find(w, name, type);
        if (c == null) {
            throw new IllegalStateException("component not found: " + name);
        }
        return c;
    }

    static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    static void text(Window w, String name, String value) throws Exception {
        JTextComponent c = need(w, name, JTextComponent.class);
        edt(() -> c.setText(value));
    }

    static String getText(Window w, String name) {
        return need(w, name, JTextComponent.class).getText();
    }

    static void select(Window w, String name, Object item) throws Exception {
        JComboBox<?> c = need(w, name, JComboBox.class);
        edt(() -> c.setSelectedItem(item));
    }

    /** Click without blocking this thread (a modal dialog may open and block the event thread). */
    static void click(Window w, String name) throws Exception {
        AbstractButton b = need(w, name, AbstractButton.class);
        SwingUtilities.invokeLater(b::doClick);
        Thread.sleep(250);
    }

    static JDialog waitDialog() throws Exception {
        for (int i = 0; i < 100; i++) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isShowing()) {
                    Thread.sleep(400);
                    return (JDialog) w;
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("no dialog appeared");
    }

    static String dialogText(JDialog d) {
        StringBuilder sb = new StringBuilder();
        collectLabels(d, sb);
        return sb.toString();
    }

    private static void collectLabels(Container c, StringBuilder sb) {
        for (Component child : c.getComponents()) {
            if (child instanceof JLabel && ((JLabel) child).getText() != null) {
                sb.append(((JLabel) child).getText()).append(' ');
            }
            if (child instanceof Container) {
                collectLabels((Container) child, sb);
            }
        }
    }

    /** Presses a button of the open dialog (OK / Yes / No). */
    static void dialogButton(JDialog d, String label) throws Exception {
        JButton target = findButton(d, label);
        if (target == null) {
            throw new IllegalStateException("dialog button not found: " + label);
        }
        SwingUtilities.invokeLater(target::doClick);
        for (int i = 0; i < 50 && d.isShowing(); i++) {
            Thread.sleep(100);
        }
        Thread.sleep(200);
    }

    private static JButton findButton(Container c, String label) {
        for (Component child : c.getComponents()) {
            if (child instanceof JButton && label.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                JButton b = findButton((Container) child, label);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    /** Click, expect a dialog, check its text, screenshot it, close it. */
    static String clickAndExpectDialog(Window w, String button, String shotName, String closeWith) throws Exception {
        click(w, button);
        JDialog d = waitDialog();
        String message = dialogText(d);
        if (shotName != null) {
            shot(w, shotName);
        }
        dialogButton(d, closeWith);
        return message;
    }

    static void shot(Window w, String name) throws Exception {
        Thread.sleep(300);
        Rectangle r = new Rectangle(w.getLocationOnScreen(), w.getSize());
        BufferedImage img = robot.createScreenCapture(r);
        ImageIO.write(img, "png", new File(shots, name + ".png"));
    }

    static JTable table(Window w, String name) {
        return need(w, name, JTable.class);
    }

    static int rows(Window w, String name) {
        return table(w, name).getRowCount();
    }

    static Object cell(Window w, String name, int row, int col) {
        return table(w, name).getValueAt(row, col);
    }

    static void selectRow(Window w, String name, int row) throws Exception {
        JTable t = table(w, name);
        edt(() -> t.setRowSelectionInterval(row, row));
        Thread.sleep(200);
    }

    static void nav(Window w, String target) throws Exception {
        click(w, "nav-" + target);
        Thread.sleep(300);
    }

    // ------------------------------------------------------------ the scenario

    static void addEmployee(Window w, String id, String name, String age, String dept, String position,
                            boolean partTime, String s1, String s2) throws Exception {
        text(w, "idField", id);
        text(w, "nameField", name);
        text(w, "ageField", age);
        select(w, "departmentCombo", dept);
        text(w, "positionField", position);
        if (partTime) {
            select(w, "typeCombo", ems.model.EmployeeType.PART_TIME);
            text(w, "hourlyRateField", s1);
            text(w, "hoursWorkedField", s2);
        } else {
            select(w, "typeCombo", ems.model.EmployeeType.FULL_TIME);
            text(w, "basicSalaryField", s1);
            text(w, "bonusField", s2);
        }
    }

    static Window login() throws Exception {
        Window loginWindow = waitWindow("LoginFrame");
        text(loginWindow, "usernameField", "admin");
        text(loginWindow, "passwordField", "admin123");
        click(loginWindow, "loginButton");
        return waitWindow("MainFrame");
    }

    public static void main(String[] args) throws Exception {
        String phase = args[0];
        shots = new File(args[1]);
        shots.mkdirs();
        robot = new Robot();
        Main.main(new String[0]);
        try {
            if (phase.equals("phase1")) {
                phase1();
            } else {
                phase2();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            failed++;
        }
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void phase1() throws Exception {
        Window loginWindow = waitWindow("LoginFrame");
        shot(loginWindow, "01_login");

        // wrong credentials
        text(loginWindow, "usernameField", "admin");
        text(loginWindow, "passwordField", "wrong");
        String msg = clickAndExpectDialog(loginWindow, "loginButton", "02_login_error", "OK");
        check(msg.contains("Incorrect username or password"), "wrong password shows error message");
        check(visibleWindow("MainFrame") == null, "main window not opened after wrong password");

        // correct credentials
        text(loginWindow, "usernameField", "admin");
        text(loginWindow, "passwordField", "admin123");
        click(loginWindow, "loginButton");
        Window main = waitWindow("MainFrame");
        check(visibleWindow("LoginFrame") == null, "login window closed after success");
        shot(main, "03_dashboard_empty");

        // ---------------- Add: validation errors
        nav(main, "add");
        check(getText(main, "idField").equals("1"), "ID suggestion pre-filled with 1");
        shot(main, "04_add_employee_form");
        String m = clickAndExpectDialog(main, "addButton", "05_add_validation_error", "OK");
        check(m.contains("Full name is required"), "empty form rejected: " + m.trim());
        addEmployee(main, "1", "Ahmed Hassan", "abc", "IT", "Engineer", false, "15000", "2000");
        m = clickAndExpectDialog(main, "addButton", null, "OK");
        check(m.contains("Age must be a valid whole number"), "bad age rejected: " + m.trim());
        addEmployee(main, "1", "Ahmed Hassan", "30", "IT", "Engineer", false, "-5", "2000");
        m = clickAndExpectDialog(main, "addButton", null, "OK");
        check(m.contains("Basic salary must not be negative"), "negative salary rejected: " + m.trim());
        addEmployee(main, "1", "Ahmed Hassan", "30", "IT", "Engineer", false, "15000", "xyz");
        m = clickAndExpectDialog(main, "addButton", null, "OK");
        check(m.contains("Bonus must be a valid number"), "bad bonus rejected: " + m.trim());
        addEmployee(main, "1", "Omar", "30", "IT", "Tutor", true, "100", "-3");
        m = clickAndExpectDialog(main, "addButton", null, "OK");
        check(m.contains("Hours worked must not be negative"), "negative hours rejected: " + m.trim());

        // ---------------- Add: valid employees
        addEmployee(main, "1", "Ahmed Hassan", "30", "IT", "Software Engineer", false, "15000", "2000");
        shot(main, "06_add_fulltime_filled");
        m = clickAndExpectDialog(main, "addButton", "07_add_success", "OK");
        check(m.contains("Employee added successfully!"), "success message shown: " + m.trim());
        check(getText(main, "idField").equals("2"), "form cleared with next ID after add");

        addEmployee(main, "2", "Sara Mohamed", "24", "Human Resources", "HR Assistant", true, "100", "80");
        shot(main, "08_add_parttime_filled");
        m = clickAndExpectDialog(main, "addButton", null, "OK");
        check(m.contains("added successfully"), "part-time employee added");

        addEmployee(main, "2", "Duplicate Id", "40", "IT", "Dev", false, "1", "1");
        m = clickAndExpectDialog(main, "addButton", "09_add_duplicate_id_error", "OK");
        check(m.contains("Employee ID 2 already exists"), "duplicate ID rejected: " + m.trim());

        addEmployee(main, "3", "Mona Adel", "35", "Finance", "Accountant", false, "12000", "500");
        clickAndExpectDialog(main, "addButton", null, "OK");
        addEmployee(main, "4", "Khaled Ali", "28", "Sales", "Sales Rep", true, "75.5", "120");
        clickAndExpectDialog(main, "addButton", null, "OK");
        addEmployee(main, "5", "Laila, \"Lili\" Samir", "45", "IT", "Team Lead", false, "25000", "5000");
        clickAndExpectDialog(main, "addButton", null, "OK");

        // ---------------- View
        nav(main, "view");
        check(rows(main, "viewTable") == 5, "5 employees in the table");
        shot(main, "10_view_all_employees");
        // find rows by id (table can be sorted) and check polymorphic salaries
        double full = 0, part = 0;
        for (int i = 0; i < rows(main, "viewTable"); i++) {
            int id = (Integer) cell(main, "viewTable", i, 0);
            double sal = (Double) cell(main, "viewTable", i, 7);
            if (id == 1) {
                full = sal;
            }
            if (id == 2) {
                part = sal;
            }
        }
        check(full == 17000.0, "full-time salary column = 17,000");
        check(part == 8000.0, "part-time salary column = 8,000");
        select(main, "filterCombo", "IT");
        Thread.sleep(300);
        check(rows(main, "viewTable") == 2, "department filter IT shows 2 employees");
        shot(main, "11_view_filter_department");
        select(main, "filterCombo", "All Departments");
        Thread.sleep(300);
        check(rows(main, "viewTable") == 5, "filter back to all");

        // ---------------- Search & update
        nav(main, "search");
        check(rows(main, "searchTable") == 5, "search panel lists everyone first");
        select(main, "searchCriteria", "Employee ID");
        text(main, "searchField", "abc");
        m = clickAndExpectDialog(main, "searchButton", null, "OK");
        check(m.contains("Employee ID must be a valid whole number"), "search with letters rejected");
        text(main, "searchField", "3");
        click(main, "searchButton");
        Thread.sleep(400);
        check(rows(main, "searchTable") == 1 && cell(main, "searchTable", 0, 1).equals("Mona Adel"), "search by ID 3 finds Mona");
        text(main, "searchField", "999");
        m = clickAndExpectDialog(main, "searchButton", "12_search_no_result", "OK");
        check(m.contains("No employees found"), "no-match message shown");
        select(main, "searchCriteria", "Department");
        text(main, "searchField", "it");
        click(main, "searchButton");
        Thread.sleep(400);
        check(rows(main, "searchTable") == 2, "department search is case-insensitive (it -> IT)");
        select(main, "searchCriteria", "Name");
        text(main, "searchField", "SARA");
        click(main, "searchButton");
        Thread.sleep(400);
        check(rows(main, "searchTable") == 1, "name search case-insensitive");

        // update with nothing selected
        select(main, "searchCriteria", "Employee ID");
        text(main, "searchField", "2");
        click(main, "searchButton");
        Thread.sleep(400);
        m = clickAndExpectDialog(main, "updateButton", null, "OK");
        check(m.contains("select an employee"), "update without selection shows message");
        m = clickAndExpectDialog(main, "deleteButton", null, "OK");
        check(m.contains("select an employee"), "delete without selection shows message");

        selectRow(main, "searchTable", 0);
        check(getText(main, "nameField").equals("Sara Mohamed") && getText(main, "hoursWorkedField").equals("80"),
                "selected employee loaded into the form (part-time fields)");
        shot(main, "13_search_select_loaded_in_form");
        text(main, "nameField", "Sara Mohamed Ali");
        text(main, "hoursWorkedField", "100");
        m = clickAndExpectDialog(main, "updateButton", "14_update_success", "OK");
        check(m.contains("Employee updated successfully!"), "update success message");
        check(rows(main, "searchTable") == 5, "table refreshed after update");

        // update to duplicate ID must fail
        text(main, "searchField", "2");
        click(main, "searchButton");
        Thread.sleep(400);
        selectRow(main, "searchTable", 0);
        check(cell(main, "searchTable", 0, 1).equals("Sara Mohamed Ali"), "updated name visible in table");
        check(((Double) cell(main, "searchTable", 0, 7)) == 10000.0, "salary recalculated 100 x 100 = 10,000");
        text(main, "idField", "1");
        m = clickAndExpectDialog(main, "updateButton", "15_update_duplicate_id_error", "OK");
        check(m.contains("Employee ID 1 already exists"), "update to duplicate ID rejected");
        // change type to full-time and ID to a free number
        text(main, "idField", "20");
        select(main, "typeCombo", ems.model.EmployeeType.FULL_TIME);
        text(main, "basicSalaryField", "9000");
        text(main, "bonusField", "1000");
        m = clickAndExpectDialog(main, "updateButton", null, "OK");
        check(m.contains("updated successfully"), "id + type change accepted");
        check(rows(main, "searchTable") == 5, "still 5 employees after id change");

        // delete: cancel then confirm
        text(main, "searchField", "20");
        click(main, "searchButton");
        Thread.sleep(400);
        selectRow(main, "searchTable", 0);
        click(main, "deleteButton");
        JDialog confirm = waitDialog();
        check(dialogText(confirm).contains("Are you sure you want to delete"), "delete asks for confirmation");
        shot(main, "16_delete_confirmation");
        dialogButton(confirm, "No");
        click(main, "showAllButton");
        Thread.sleep(300);
        check(rows(main, "searchTable") == 5, "cancel keeps the employee");
        text(main, "searchField", "20");
        click(main, "searchButton");
        Thread.sleep(400);
        selectRow(main, "searchTable", 0);
        click(main, "deleteButton");
        confirm = waitDialog();
        dialogButton(confirm, "Yes");
        JDialog done = waitDialog();
        check(dialogText(done).contains("deleted successfully"), "delete success message");
        dialogButton(done, "OK");
        check(rows(main, "searchTable") == 4, "employee deleted (4 left)");

        // ---------------- Departments
        nav(main, "departments");
        check(rows(main, "departmentTable") == 5, "5 default departments listed");
        text(main, "deptNameField", "it");
        m = clickAndExpectDialog(main, "addDepartmentButton", "17_department_duplicate_error", "OK");
        check(m.contains("already exists"), "duplicate department name (any case) rejected: " + m.trim());
        text(main, "deptNameField", "Legal");
        m = clickAndExpectDialog(main, "addDepartmentButton", null, "OK");
        check(m.contains("Department added successfully"), "department added");
        check(rows(main, "departmentTable") == 6, "6 departments now");
        selectRow(main, "departmentTable", 0); // IT (has employees)
        click(main, "deleteDepartmentButton");
        confirm = waitDialog();
        dialogButton(confirm, "Yes");
        done = waitDialog();
        check(dialogText(done).contains("assigned"), "cannot delete a department that has employees");
        dialogButton(done, "OK");
        shot(main, "18_departments");

        // new department appears in the Add form
        nav(main, "add");
        JComboBox<?> combo = need(main, "departmentCombo", JComboBox.class);
        boolean hasLegal = false;
        for (int i = 0; i < combo.getItemCount(); i++) {
            hasLegal |= "Legal".equals(combo.getItemAt(i));
        }
        check(hasLegal, "new department shows up in the department drop-down");

        nav(main, "dashboard");
        shot(main, "19_dashboard");

        // ---------------- Logout
        click(main, "logoutButton");
        JDialog logoutDialog = waitDialog();
        dialogButton(logoutDialog, "Yes");
        Window again = waitWindow("LoginFrame");
        check(again != null && visibleWindow("MainFrame") == null, "logout returns to the login screen");
    }

    /** New JVM = the "close and reopen the application" step. */
    static void phase2() throws Exception {
        Window main = login();
        check(true, "login works after restart");
        nav(main, "view");
        check(rows(main, "viewTable") == 4, "4 employees reloaded from the file after restart");
        boolean sawComma = false;
        double lila = 0;
        for (int i = 0; i < rows(main, "viewTable"); i++) {
            if ("Laila, \"Lili\" Samir".equals(cell(main, "viewTable", i, 1))) {
                sawComma = true;
                lila = (Double) cell(main, "viewTable", i, 7);
            }
        }
        check(sawComma && lila == 30000.0, "name with comma/quote and its salary reloaded");
        shot(main, "20_after_restart_view");
        nav(main, "departments");
        check(rows(main, "departmentTable") == 6, "departments (incl. Legal) reloaded");
        nav(main, "dashboard");
        shot(main, "21_dashboard_after_restart");
    }
}
