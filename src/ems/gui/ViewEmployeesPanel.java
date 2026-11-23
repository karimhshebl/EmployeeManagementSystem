package ems.gui;

import ems.model.Department;
import ems.model.Employee;
import ems.service.EmployeeManager;
import ems.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

/** Shows all employees in a table, with a department filter and delete. */
class ViewEmployeesPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final String ALL = "All Departments";

    private final EmployeeManager manager;
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable table = Ui.createTable(tableModel);
    {
        Ui.sizeEmployeeColumns(table);
    }
    private final JComboBox<String> filterCombo = new JComboBox<>();
    private final JLabel summaryLabel = new JLabel();
    private final JLabel selectedLabel = new JLabel("No employee selected");
    private boolean loadingFilter;

    ViewEmployeesPanel(EmployeeManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Ui.BACKGROUND);
        add(Ui.header("All Employees"), BorderLayout.NORTH);

        table.setName("viewTable");
        filterCombo.setName("filterCombo");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        top.add(Ui.label("Filter by department:"));
        top.add(filterCombo);
        Ui.ActionButton refreshButton = Ui.button("Refresh", Ui.ACCENT);
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(e -> refresh());
        top.add(refreshButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        JPanel info = new JPanel(new java.awt.GridLayout(2, 1));
        info.setOpaque(false);
        summaryLabel.setFont(Ui.font(java.awt.Font.BOLD, 13));
        selectedLabel.setFont(Ui.font(java.awt.Font.PLAIN, 13));
        info.add(summaryLabel);
        info.add(selectedLabel);
        Ui.ActionButton deleteButton = Ui.button("Delete Selected", Ui.DANGER);
        deleteButton.setName("deleteButton");
        deleteButton.addActionListener(e -> deleteSelected());
        bottom.add(info, BorderLayout.CENTER);
        bottom.add(deleteButton, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        filterCombo.addActionListener(e -> {
            if (!loadingFilter) {
                reloadTable();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelection();
            }
        });
    }

    @Override
    public void refresh() {
        Object previous = filterCombo.getSelectedItem();
        loadingFilter = true;
        filterCombo.removeAllItems();
        filterCombo.addItem(ALL);
        for (Department d : manager.getDepartmentManager().getAll()) {
            filterCombo.addItem(d.getName());
        }
        if (previous != null) {
            filterCombo.setSelectedItem(previous);
        }
        loadingFilter = false;
        reloadTable();
    }

    private void reloadTable() {
        String choice = (String) filterCombo.getSelectedItem();
        List<Employee> list = (choice == null || ALL.equals(choice))
                ? manager.getAll() : manager.filterByDepartment(choice);
        tableModel.setEmployees(list);
        double total = 0;
        for (Employee e : list) {
            total += e.calculateSalary();
        }
        summaryLabel.setText("Employees shown: " + list.size() + "     Total monthly payroll: "
                + Ui.money(total) + " EGP");
        showSelection();
    }

    private Employee selectedEmployee() {
        int row = table.getSelectedRow();
        return row < 0 ? null : tableModel.getEmployeeAt(table.convertRowIndexToModel(row));
    }

    private void showSelection() {
        Employee e = selectedEmployee();
        selectedLabel.setText(e == null ? "No employee selected"
                : "Selected: #" + e.getId() + " " + e.getFullName() + " - " + e.getType()
                + " - monthly salary " + Ui.money(e.calculateSalary()) + " EGP");
    }

    private void deleteSelected() {
        Employee employee = selectedEmployee();
        if (employee == null) {
            Ui.warn(this, "Please select an employee from the table first.");
            return;
        }
        if (!Ui.confirm(this, "Are you sure you want to delete " + employee.getFullName()
                + " (ID " + employee.getId() + ")?", "Confirm Delete")) {
            return; // cancelled: the employee stays
        }
        try {
            manager.delete(employee.getId());
            Ui.info(this, "Employee deleted successfully.");
        } catch (ValidationException ex) {
            Ui.error(this, ex.getMessage());
        } catch (IOException ex) {
            Ui.error(this, "The change could not be saved to the file:\n" + ex.getMessage());
        }
        reloadTable();
    }
}
