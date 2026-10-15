package with;

/**
 * SRP APPLIED — part 4 of 4.
 *
 * The ONLY reason this class changes is a change to the report's
 * look/format (e.g. switch to JSON or HTML). It depends on the
 * PayCalculator for the number but owns nothing else.
 */
public class EmployeeReportFormatter {

    private final PayCalculator payCalculator;

    public EmployeeReportFormatter(PayCalculator payCalculator) {
        this.payCalculator = payCalculator;
    }

    public String format(Employee employee) {
        return "=== Employee Report ===\n"
                + "Name: " + employee.getName() + "\n"
                + "Pay : $" + payCalculator.calculatePay(employee);
    }
}
