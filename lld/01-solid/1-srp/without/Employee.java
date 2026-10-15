package without;

/**
 * SRP VIOLATION.
 *
 * This single class has THREE reasons to change:
 *   1. The HR/finance team changes how pay is calculated.
 *   2. The DBA team changes how/where data is persisted.
 *   3. The reporting team changes the report format.
 *
 * Any of those three unrelated teams can force an edit to this file.
 * A bug introduced while tweaking the report could break payroll.
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

    // Reason to change #1: business/finance rules.
    public double calculatePay() {
        double pay = hoursWorked * hourlyRate;
        if (hoursWorked > 40) {
            // overtime at 1.5x for hours beyond 40
            pay += (hoursWorked - 40) * hourlyRate * 0.5;
        }
        return pay;
    }

    // Reason to change #2: persistence / database technology.
    public void saveToDatabase() {
        // Pretend this opens a JDBC connection and runs an INSERT.
        System.out.println("INSERT INTO employees VALUES ('" + name + "', " + calculatePay() + ")");
    }

    // Reason to change #3: presentation / report format.
    public String formatReport() {
        return "=== Employee Report ===\n"
                + "Name: " + name + "\n"
                + "Pay : $" + calculatePay();
    }

    public static void main(String[] args) {
        Employee e = new Employee("Alice", 45, 20.0);
        System.out.println(e.formatReport());
        e.saveToDatabase();
    }
}
