package ems.model;

/** Part-time employee: paid by the hour. */
public class PartTimeEmployee extends Employee {
    private double hourlyRate;
    private double hoursWorked;

    public PartTimeEmployee(int id, String fullName, int age, String department, String position,
                            double hourlyRate, double hoursWorked) {
        super(id, fullName, age, department, position);
        setHourlyRate(hourlyRate);
        setHoursWorked(hoursWorked);
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public final void setHourlyRate(double hourlyRate) {
        this.hourlyRate = requireNonNegative(hourlyRate, "Hourly rate");
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public final void setHoursWorked(double hoursWorked) {
        this.hoursWorked = requireNonNegative(hoursWorked, "Hours worked");
    }

    @Override
    public EmployeeType getType() {
        return EmployeeType.PART_TIME;
    }

    /** Monthly Salary = Hourly Rate x Hours Worked. */
    @Override
    public double calculateSalary() {
        return hourlyRate * hoursWorked;
    }

    @Override
    public String getSalaryDetails() {
        return money(hourlyRate) + "/hr x " + plain(hoursWorked) + " hrs";
    }

    @Override
    public String[] getSalaryData() {
        return new String[]{plain(hourlyRate), plain(hoursWorked)};
    }
}
