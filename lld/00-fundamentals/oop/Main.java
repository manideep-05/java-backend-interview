package oop;

/** Runs every OOP fundamentals demo. */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Composition vs Inheritance ===");
        CompositionVsInheritance.demo();

        System.out.println("\n=== Interface vs Abstract class ===");
        Shapes.demo();

        System.out.println("\n=== equals / hashCode ===");
        EqualsHashCode.demo();

        System.out.println("\n=== Immutability ===");
        ImmutableMoney.demo();

        System.out.println("\n=== Comparable vs Comparator ===");
        Sorting.demo();

        System.out.println("\n=== Enum with behavior ===");
        Operation.demo();
    }
}
