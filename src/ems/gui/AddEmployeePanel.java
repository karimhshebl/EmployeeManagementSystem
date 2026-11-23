package ems.gui;

import ems.model.Employee;
import ems.service.EmployeeManager;
import ems.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;

/** Form for adding a new employee. */
class AddEmployeePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final EmployeeManager manager;
    private final EmployeeFormPanel form;

    AddEmployeePanel(EmployeeManager manager) {
        this.manager = manager;
        this.form = new EmployeeFormPanel(manager.getDepartmentManager());
        setLayout(new BorderLayout());
        setBackground(Ui.BACKGROUND);
        add(Ui.header("Add New Employee"), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(java.awt.Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0xDDE3E9)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        card.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttons.setOpaque(false);
        Ui.ActionButton addButton = Ui.button("Add Employee", Ui.SUCCESS);
        addButton.setName("addButton");
        addButton.addActionListener(e -> addEmployee());
        Ui.ActionButton clearButton = Ui.button("Clear", Ui.WARNING);
        clearButton.setName("clearButton");
        clearButton.addActionListener(e -> form.clear(String.valueOf(manager.nextId())));
        buttons.add(addButton);
        buttons.add(clearButton);
        card.add(buttons, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
        wrapper.add(card, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        form.refreshDepartments();
        form.clear(String.valueOf(manager.nextId()));
    }

    private void addEmployee() {
        try {
            Employee employee = form.buildEmployee();
            manager.add(employee);
            Ui.info(this, "Employee added successfully!");
            form.clear(String.valueOf(manager.nextId()));
        } catch (ValidationException ex) {
            Ui.error(this, ex.getMessage());
        } catch (IOException ex) {
            Ui.error(this, "The employee could not be saved to the file:\n" + ex.getMessage());
        }
    }
}
