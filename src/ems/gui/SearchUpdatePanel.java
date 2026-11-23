package ems.gui;

import ems.model.Employee;
import ems.service.EmployeeManager;
import ems.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

/**
 * Search employees (by ID, name or department), select one, edit it in the form below,
 * then update or delete it.
 */
class SearchUpdatePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private static final String BY_ID = "Employee ID";
    private static final String BY_NAME = "Name";
    private static final String BY_DEPARTMENT = "Department";

    private final EmployeeManager manager;
    private final JComboBox<String> criteriaCombo = new JComboBox<>(new String[]{BY_ID, BY_NAME, BY_DEPARTMENT});
    private final JTextField searchField = new JTextField(20);
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable table = Ui.createTable(tableModel);
    {
        Ui.sizeEmployeeColumns(table);
    }
    private final EmployeeFormPanel form;
    /** ID of the employee currently loaded in the form, or null when nothing is selected. */
    private Integer selectedId;

    SearchUpdatePanel(EmployeeManager manager) {
        this.manager = manager;
        this.form = new EmployeeFormPanel(manager.getDepartmentManager());
        setLayout(new BorderLayout());
        setBackground(Ui.BACKGROUND);
        add(Ui.header("Search and Update Employees"), BorderLayout.NORTH);

        criteriaCombo.setName("searchCriteria");
        searchField.setName("searchField");
        table.setName("searchTable");

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        searchBar.setOpaque(false);
        searchBar.add(Ui.label("Search by:"));
        searchBar.add(criteriaCombo);
        searchBar.add(searchField);
        Ui.ActionButton searchButton = Ui.button("Search", Ui.ACCENT);
        searchButton.setName("searchButton");
        searchButton.addActionListener(e -> search());
        Ui.ActionButton showAllButton = Ui.button("Show All", Ui.NEUTRAL);
        showAllButton.setName("showAllButton");
        showAllButton.addActionListener(e -> showAll());
        searchBar.add(searchButton);
        searchBar.add(showAllButton);
        searchField.addActionListener(e -> search());

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel editCard = new JPanel(new BorderLayout());
        editCard.setBackground(Color.WHITE);
        editCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 12, 12, 12),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0xDDE3E9)),
                                "Selected employee (edit the fields, then press Update)"),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8))));
        editCard.add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        buttons.setOpaque(false);
        Ui.ActionButton updateButton = Ui.button("Update Employee", Ui.SUCCESS);
        updateButton.setName("updateButton");
        updateButton.addActionListener(e -> updateSelected());
        Ui.ActionButton deleteButton = Ui.button("Delete Employee", Ui.DANGER);
        deleteButton.setName("deleteButton");
        deleteButton.addActionListener(e -> deleteSelected());
        Ui.ActionButton clearButton = Ui.button("Clear Selection", Ui.WARNING);
        clearButton.setName("clearSelectionButton");
        clearButton.addActionListener(e -> clearSelection());
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);
        editCard.add(buttons, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(searchBar, BorderLayout.NORTH);
        body.add(tablePanel, BorderLayout.CENTER);
        body.add(editCard, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
    }

    @Override
    public void refresh() {
        form.refreshDepartments();
        searchField.setText("");
        showAll();
    }

    private void showAll() {
        showResults(manager.getAll());
    }

    /** Puts the given employees in the table and resets the edit form. */
    private void showResults(List<Employee> employees) {
        tableModel.setEmployees(employees); // this clears the selection
        form.clear(null);
        selectedId = null;
    }

    private void onSelectionChanged() {
        int row = table.getSelectedRow();
        if (row < 0) {
            selectedId = null;
            return;
        }
        Employee employee = tableModel.getEmployeeAt(table.convertRowIndexToModel(row));
        selectedId = employee.getId();
        form.loadEmployee(employee);
    }

    private void clearSelection() {
        table.clearSelection();
        form.clear(null);
        selectedId = null;
    }

    private void search() {
        String criteria = (String) criteriaCombo.getSelectedItem();
        String text = searchField.getText();
        try {
            List<Employee> results;
            if (BY_ID.equals(criteria)) {
                results = manager.searchById(text);
            } else if (BY_NAME.equals(criteria)) {
                results = manager.searchByName(text);
            } else {
                results = manager.searchByDepartment(text);
            }
            showResults(results);
            if (results.isEmpty()) {
                Ui.info(this, "No employees found matching \"" + text.trim() + "\".");
            }
        } catch (ValidationException ex) {
            Ui.error(this, ex.getMessage());
        }
    }

    private void updateSelected() {
        if (selectedId == null) {
            Ui.warn(this, "Please select an employee from the table first.");
            return;
        }
        try {
            Employee updated = form.buildEmployee();
            manager.update(selectedId, updated);
            Ui.info(this, "Employee updated successfully!");
            showResults(manager.getAll());
            int row = tableModel.indexOfId(updated.getId());
            if (row >= 0) {
                int viewRow = table.convertRowIndexToView(row);
                table.setRowSelectionInterval(viewRow, viewRow);
            }
        } catch (ValidationException ex) {
            Ui.error(this, ex.getMessage());
        } catch (IOException ex) {
            Ui.error(this, "The change could not be saved to the file:\n" + ex.getMessage());
        }
    }

    private void deleteSelected() {
        if (selectedId == null) {
            Ui.warn(this, "Please select an employee from the table first.");
            return;
        }
        Employee employee = manager.findById(selectedId);
        if (employee == null) {
            showAll();
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
        showAll();
    }
}
