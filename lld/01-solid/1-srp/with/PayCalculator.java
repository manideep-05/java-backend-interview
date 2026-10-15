package with;

/**
 * SRP APPLIED — part 2 of 4.
 *
 * The ONLY reason this class changes is a change to pay/overtime rules.
 * It can be unit-tested in complete isolation: no database, no formatting.
 */
public class PayCalculator {

    private static final int FULL_TIME_HOURS = 40;
    private static final double OVERTIME_MULTIPLIER = 0.5; // extra half on top of base

    public double calculatePay(Employee employee) {
        double pay = employee.getHoursWorked() * employee.getHourlyRate();
        if (employee.getHoursWorked() > FULL_TIME_HOURS) {
            int overtimeHours = employee.getHoursWorked() - FULL_TIME_HOURS;
            pay += overtimeHours * employee.getHourlyRate() * OVERTIME_MULTIPLIER;
        }
        return pay;
    }
}
