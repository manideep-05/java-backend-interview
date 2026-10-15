package with;

/**
 * SRP APPLIED — part 3 of 4.
 *
 * The ONLY reason this class changes is a change to persistence:
 * switching from JDBC to JPA, Postgres to Mongo, etc.
 * Pay rules and report formats are irrelevant here.
 */
public class EmployeeRepository {

    public void save(Employee employee, double pay) {
        // Pretend this opens a connection and runs a parameterized INSERT.
        System.out.println("INSERT INTO employees VALUES ('"
                + employee.getName() + "', " + pay + ")");
    }
}
