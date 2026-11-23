package ems.model;

/** Full-time employee: fixed monthly salary plus an optional bonus. */
public class FullTimeEmployee extends Employee {
    private double basicSalary;
    private double bonus;

    public FullTimeEmployee(int id, String fullName, int age, String department, String position,
                            double basicSalary, double bonus) {
        super(id, fullName, age, department, position);
        setBasicSalary(basicSalary);
        setBonus(bonus);
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public final void setBasicSalary(double basicSalary) {
        this.basicSalary = requireNonNegative(basicSalary, "Basic salary");
    }

    public double getBonus() {
        return bonus;
    }

    public final void setBonus(double bonus) {
        this.bonus = requireNonNegative(bonus, "Bonus");
    }

    @Override
    public EmployeeType getType() {
        return EmployeeType.FULL_TIME;
    }

    /** Monthly Salary = Basic Salary + Bonus. */
    @Override
    public double calculateSalary() {
        return basicSalary + bonus;
    }

    @Override
    public String getSalaryDetails() {
        return "Basic " + money(basicSalary) + " + Bonus " + money(bonus);
    }

    @Override
    public String[] getSalaryData() {
        return new String[]{plain(basicSalary), plain(bonus)};
    }
}
