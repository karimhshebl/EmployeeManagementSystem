package ems.gui;

import ems.model.Department;
import ems.service.DepartmentManager;
import ems.service.EmployeeManager;
import ems.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.io.IOException;

/** Add and delete departments. */
class DepartmentPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final EmployeeManager employeeManager;
    private final DepartmentManager departmentManager;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Department ID", "Department Name", "Employees"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 1 ? String.class : Integer.class;
        }
    };
    private final JTable table = Ui.createTable(tableModel);
    private final JTextField idField = new JTextField(10);
    private final JTextField nameField = new JTextField(20);

    DepartmentPanel(EmployeeManager employeeManager) {
        this.employeeManager = employeeManager;
        this.departmentManager = employeeManager.getDepartmentManager();
        setLayout(new BorderLayout());
        setBackground(Ui.BACKGROUND);
        add(Ui.header("Manage Departments"), BorderLayout.NORTH);

        table.setName("departmentTable");
        idField.setName("deptIdField");
        nameField.setName("deptNameField");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDE3E9)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        form.add(Ui.label("Department ID"), Ui.cell(0, 0, 1, 0));
        form.add(idField, Ui.cell(1, 0, 1, 0));
        form.add(Ui.label("Department Name"), Ui.cell(2, 0, 1, 0));
        form.add(nameField, Ui.cell(3, 0, 1, 1));
        Ui.ActionButton addButton = Ui.button("Add Department", Ui.SUCCESS);
        addButton.setName("addDepartmentButton");
        addButton.addActionListener(e -> addDepartment());
        form.add(addButton, Ui.cell(4, 0, 1, 0));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        bottom.setOpaque(false);
        Ui.ActionButton deleteButton = Ui.button("Delete Selected", Ui.DANGER);
        deleteButton.setName("deleteDepartmentButton");
        deleteButton.addActionListener(e -> deleteSelected());
        bottom.add(deleteButton);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));
        body.add(form, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        reload();
        idField.setText(String.valueOf(departmentManager.nextId()));
        nameField.setText("");
    }

    private void reload() {
        tableModel.setRowCount(0);
        for (Department d : departmentManager.getAll()) {
            tableModel.addRow(new Object[]{d.getId(), d.getName(), employeeManager.countInDepartment(d.getName())});
        }
    }

    private void addDepartment() {
        try {
            departmentManager.add(idField.getText(), nameField.getText());
            Ui.info(this, "Department added successfully!");
            refresh();
        } catch (ValidationException ex) {
            Ui.error(this, ex.getMessage());
        } catch (IOException ex) {
            Ui.error(this, "The department could not be saved to the file:\n" + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.warn(this, "Please select a department from the table first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (Integer) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        if (!Ui.confirm(this, "Are you sure you want to delete the department \"" + name + "\"?",
                "Confirm Delete")) {
            return;
        }
        try {
            employeeManager.deleteDepartment(id);
            Ui.info(this, "Department deleted successfully.");
            refresh();
        } catch (ValidationException ex) {
            Ui.error(this, ex.getMessage());
        } catch (IOException ex) {
            Ui.error(this, "The change could not be saved to the file:\n" + ex.getMessage());
        }
    }
}
