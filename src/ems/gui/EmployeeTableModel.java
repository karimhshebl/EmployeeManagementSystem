package ems.gui;

import ems.model.Employee;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Shows a list of employees in a JTable. The salary column uses each employee's own calculateSalary(). */
class EmployeeTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"ID", "Full Name", "Age", "Department", "Position",
            "Type", "Salary Details", "Monthly Salary (EGP)"};

    private List<Employee> employees = new ArrayList<>();

    void setEmployees(List<Employee> employees) {
        this.employees = new ArrayList<>(employees);
        fireTableDataChanged();
    }

    Employee getEmployeeAt(int modelRow) {
        return employees.get(modelRow);
    }

    /** Model row of the employee with this ID, or -1. */
    int indexOfId(int id) {
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return employees.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
            case 2:
                return Integer.class;
            case 7:
                return Double.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Employee e = employees.get(row);
        switch (column) {
            case 0:
                return e.getId();
            case 1:
                return e.getFullName();
            case 2:
                return e.getAge();
            case 3:
                return e.getDepartment();
            case 4:
                return e.getPosition();
            case 5:
                return e.getType().getLabel();
            case 6:
                return e.getSalaryDetails();
            case 7:
                return e.calculateSalary(); // polymorphic call
            default:
                return "";
        }
    }
}
