package ems.gui;

import ems.model.Department;
import ems.model.Employee;
import ems.model.EmployeeType;
import ems.service.DepartmentManager;
import ems.service.EmployeeFactory;
import ems.service.ValidationException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * The employee input form, shared by the Add panel and the Update panel.
 * The salary fields change with the selected employee type.
 */
class EmployeeFormPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DepartmentManager departmentManager;

    private final JTextField idField = new JTextField(12);
    private final JTextField nameField = new JTextField(18);
    private final JTextField ageField = new JTextField(12);
    private final JComboBox<String> departmentCombo = new JComboBox<>();
    private final JTextField positionField = new JTextField(18);
    private final JComboBox<EmployeeType> typeCombo = new JComboBox<>(EmployeeType.values());

    // Full-time salary fields
    private final JLabel basicLabel = Ui.label("Basic Monthly Salary (EGP)");
    private final JTextField basicField = new JTextField(12);
    private final JLabel bonusLabel = Ui.label("Monthly Bonus (EGP)");
    private final JTextField bonusField = new JTextField(12);
    // Part-time salary fields
    private final JLabel rateLabel = Ui.label("Hourly Rate (EGP)");
    private final JTextField rateField = new JTextField(12);
    private final JLabel hoursLabel = Ui.label("Hours Worked per Month");
    private final JTextField hoursField = new JTextField(12);

    EmployeeFormPanel(DepartmentManager departmentManager) {
        this.departmentManager = departmentManager;
        setOpaque(false);
        setLayout(new GridBagLayout());

        idField.setName("idField");
        nameField.setName("nameField");
        ageField.setName("ageField");
        departmentCombo.setName("departmentCombo");
        positionField.setName("positionField");
        typeCombo.setName("typeCombo");
        basicField.setName("basicSalaryField");
        bonusField.setName("bonusField");
        rateField.setName("hourlyRateField");
        hoursField.setName("hoursWorkedField");

        // fixed label width so the layout does not shift when the employee type changes
        for (JLabel l : new JLabel[]{basicLabel, bonusLabel, rateLabel, hoursLabel}) {
            l.setPreferredSize(new Dimension(190, 20));
        }
        add(Ui.label("Employee ID"), Ui.cell(0, 0, 1, 0));
        add(idField, Ui.cell(1, 0, 1, 1));
        add(Ui.label("Full Name"), Ui.cell(2, 0, 1, 0));
        add(nameField, Ui.cell(3, 0, 1, 1));

        add(Ui.label("Age"), Ui.cell(0, 1, 1, 0));
        add(ageField, Ui.cell(1, 1, 1, 1));
        add(Ui.label("Department"), Ui.cell(2, 1, 1, 0));
        add(departmentCombo, Ui.cell(3, 1, 1, 1));

        add(Ui.label("Position"), Ui.cell(0, 2, 1, 0));
        add(positionField, Ui.cell(1, 2, 1, 1));
        add(Ui.label("Employee Type"), Ui.cell(2, 2, 1, 0));
        add(typeCombo, Ui.cell(3, 2, 1, 1));

        // The full-time pair and the part-time pair share the same cells; only one pair is visible.
        add(basicLabel, Ui.cell(0, 3, 1, 0));
        add(basicField, Ui.cell(1, 3, 1, 1));
        add(bonusLabel, Ui.cell(2, 3, 1, 0));
        add(bonusField, Ui.cell(3, 3, 1, 1));
        add(rateLabel, Ui.cell(0, 3, 1, 0));
        add(rateField, Ui.cell(1, 3, 1, 1));
        add(hoursLabel, Ui.cell(2, 3, 1, 0));
        add(hoursField, Ui.cell(3, 3, 1, 1));

        typeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateSalaryFields();
            }
        });
        updateSalaryFields();
        refreshDepartments();
    }

    /** Show the salary fields that belong to the selected employee type. */
    private void updateSalaryFields() {
        EmployeeType type = (EmployeeType) typeCombo.getSelectedItem();
        boolean fullTime = type == EmployeeType.FULL_TIME;
        basicLabel.setVisible(fullTime);
        basicField.setVisible(fullTime);
        bonusLabel.setVisible(fullTime);
        bonusField.setVisible(fullTime);
        rateLabel.setVisible(!fullTime);
        rateField.setVisible(!fullTime);
        hoursLabel.setVisible(!fullTime);
        hoursField.setVisible(!fullTime);
        revalidate();
        repaint();
    }

    /** Reloads the department list, keeping the current choice when it still exists. */
    void refreshDepartments() {
        Object selected = departmentCombo.getSelectedItem();
        departmentCombo.removeAllItems();
        List<Department> departments = departmentManager.getAll();
        for (Department d : departments) {
            departmentCombo.addItem(d.getName());
        }
        if (selected != null) {
            departmentCombo.setSelectedItem(selected);
        }
    }

    /** Builds an Employee from the form. Throws ValidationException with a clear message. */
    Employee buildEmployee() throws ValidationException {
        EmployeeType type = (EmployeeType) typeCombo.getSelectedItem();
        String[] salary = type == EmployeeType.PART_TIME
                ? new String[]{rateField.getText(), hoursField.getText()}
                : new String[]{basicField.getText(), bonusField.getText()};
        return EmployeeFactory.create(type, idField.getText(), nameField.getText(), ageField.getText(),
                (String) departmentCombo.getSelectedItem(), positionField.getText(), salary);
    }

    /** Loads an existing employee into the form for editing. */
    void loadEmployee(Employee e) {
        clearFields();
        idField.setText(String.valueOf(e.getId()));
        nameField.setText(e.getFullName());
        ageField.setText(String.valueOf(e.getAge()));
        departmentCombo.setSelectedItem(e.getDepartment());
        positionField.setText(e.getPosition());
        typeCombo.setSelectedItem(e.getType());
        String[] salary = e.getSalaryData();
        if (e.getType() == EmployeeType.PART_TIME) {
            rateField.setText(salary[0]);
            hoursField.setText(salary[1]);
        } else {
            basicField.setText(salary[0]);
            bonusField.setText(salary[1]);
        }
    }

    /** Empties the form. The ID field can be pre-filled with a suggested ID. */
    void clear(String suggestedId) {
        clearFields();
        idField.setText(suggestedId == null ? "" : suggestedId);
        typeCombo.setSelectedItem(EmployeeType.FULL_TIME);
        if (departmentCombo.getItemCount() > 0) {
            departmentCombo.setSelectedIndex(0);
        }
        updateSalaryFields();
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        ageField.setText("");
        positionField.setText("");
        basicField.setText("");
        bonusField.setText("");
        rateField.setText("");
        hoursField.setText("");
    }
}
