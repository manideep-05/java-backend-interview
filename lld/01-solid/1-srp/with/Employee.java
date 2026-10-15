package with;

/**
 * SRP APPLIED — part 1 of 4.
 *
 * Employee is now a plain data holder (a "value object").
 * Its ONLY reason to change is if the shape of employee data changes
 * (e.g. we add a department). It knows nothing about pay rules,
 * databases, or report formats.
 */
public class Employee {

    private final String name;
    private final int hoursWorked;
    private final double hourlyRate;

    public Employee(String name, int hoursWorked, double hourlyRate) {
        this.name = name;
        this.hoursWorked = hoursWorked;
        this.hourlyRate = hourlyRate;
    }

    public String getName() {
        return name;
    }

    public int getHoursWorked() {
        return hoursWorked;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }
}
