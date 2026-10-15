package oop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Comparable vs Comparator.
 *
 * - Comparable: the type's ONE natural ordering, baked in via compareTo.
 * - Comparator: EXTERNAL, swappable orderings (a Strategy for sorting);
 *   define as many as you like at the call site.
 */
public class Sorting {

    // Natural ordering: by name (Comparable).
    static class Employee implements Comparable<Employee> {
        final String name;
        final int salary;
        Employee(String name, int salary) { this.name = name; this.salary = salary; }

        @Override public int compareTo(Employee o) {
            return this.name.compareTo(o.name); // natural order = by name
        }
        @Override public String toString() { return name + "(" + salary + ")"; }
    }

    static void demo() {
        List<Employee> list = new ArrayList<>(List.of(
                new Employee("Charlie", 300),
                new Employee("Alice", 500),
                new Employee("Bob", 400)));

        list.sort(null); // null => use natural ordering (Comparable)
        System.out.println("by name (natural):   " + list);

        // Comparator = external ordering strategy, swapped at the call site.
        list.sort(Comparator.comparingInt((Employee e) -> e.salary).reversed());
        System.out.println("by salary desc:      " + list);
    }
}
