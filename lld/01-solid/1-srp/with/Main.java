package with;

/**
 * Wiring the single-responsibility classes together.
 *
 * Notice: each collaborator can be swapped or tested independently.
 * The report formatter can be tested without touching the database;
 * the repository can be tested without caring about pay rules.
 */
public class Main {
    public static void main(String[] args) {
        Employee employee = new Employee("Alice", 45, 20.0);

        PayCalculator payCalculator = new PayCalculator();
        EmployeeReportFormatter formatter = new EmployeeReportFormatter(payCalculator);
        EmployeeRepository repository = new EmployeeRepository();

        double pay = payCalculator.calculatePay(employee);

        System.out.println(formatter.format(employee));
        repository.save(employee, pay);
    }
}
