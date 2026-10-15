package oop;

/**
 * Composition vs Inheritance — the same capability ("a car has an engine
 * that can start") modeled both ways, to show why we favor composition.
 */
public class CompositionVsInheritance {

    // ---------- INHERITANCE approach (is-a) ----------
    // A Car "is an" Engine? No — that's a false is-a. Inheritance here is
    // wrong modeling: a Car is not a kind of Engine. Forcing it couples Car
    // to Engine's entire API and breaks if Engine changes.
    static class Engine {
        void start() { System.out.println("Engine starts"); }
    }
    static class InheritedCar extends Engine {
        // Inherits start(), but "Car extends Engine" is a lie about identity.
    }

    // ---------- COMPOSITION approach (has-a) ----------
    // A Car HAS an Engine and delegates. We can swap ElectricEngine at
    // runtime, and Car depends only on the Engine abstraction, not its guts.
    interface Motor {
        void start();
    }
    static class PetrolMotor implements Motor {
        public void start() { System.out.println("Petrol motor vrooms"); }
    }
    static class ElectricMotor implements Motor {
        public void start() { System.out.println("Electric motor hums"); }
    }
    static class ComposedCar {
        private Motor motor; // HAS-A

        ComposedCar(Motor motor) { this.motor = motor; }
        void setMotor(Motor motor) { this.motor = motor; } // swap at runtime
        void start() { motor.start(); } // delegate
    }

    static void demo() {
        System.out.println("-- inheritance (false is-a) --");
        new InheritedCar().start();

        System.out.println("-- composition (has-a, swappable) --");
        ComposedCar car = new ComposedCar(new PetrolMotor());
        car.start();
        car.setMotor(new ElectricMotor()); // behavior changed without new class
        car.start();
    }
}
