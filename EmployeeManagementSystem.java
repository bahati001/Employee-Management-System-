import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Minimal Employee Management System implementing the three core
 * functions described in the scenario: employee management,
 * payment processing, and reporting.
 */
class Employee {
    private String id;
    private String name;
    private String department;
    private double baseSalary;
    private double netPay;

    public Employee(String id, String name, String department, double baseSalary) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.baseSalary = baseSalary;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    public double getNetPay() { return netPay; }
    public void setNetPay(double netPay) { this.netPay = netPay; }
}

public class EmployeeManagementSystem {
    private static final String EXIT_VALUE = "0";

    private List<Employee> employees = new ArrayList<>();

    // FR1: Add a new employee (rejects null, negative salary and duplicate IDs)
    public void addEmployee(Employee e) {
        if (e == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }
        if (e.getBaseSalary() < 0) {
            throw new IllegalArgumentException("Salary cannot be negative");
        }
        for (Employee existing : employees) {
            if (existing.getId().equals(e.getId())) {
                throw new IllegalArgumentException("Duplicate employee ID: " + e.getId());
            }
        }
        employees.add(e);
    }

    // FR1: Remove an employee by ID
    public boolean removeEmployee(String id) {
        return employees.removeIf(emp -> emp.getId().equals(id));
    }

    // FR1: Update an employee's salary
    public boolean updateEmployee(String id, double newSalary) {
        for (Employee e : employees) {
            if (e.getId().equals(id)) {
                if (newSalary < 0) {
                    throw new IllegalArgumentException("Salary cannot be negative");
                }
                e.setBaseSalary(newSalary);
                return true;
            }
        }
        return false;
    }

    // FR2: Calculate net pay using tiered tax brackets + fixed pension contribution
    public double calculateNetPay(Employee e) {
        double salary = e.getBaseSalary();
        double taxRate;
        if (salary <= 500000) {
            taxRate = 0.10;
        } else if (salary <= 1000000) {
            taxRate = 0.20;
        } else {
            taxRate = 0.30;
        }
        double pensionRate = 0.05;
        double tax = salary * taxRate;
        double pension = salary * pensionRate;
        double net = salary - tax - pension;
        e.setNetPay(net);
        return net;
    }

    // FR2: Process payment for a given employee ID
    public Double processPayment(String id) {
        for (Employee e : employees) {
            if (e.getId().equals(id)) {
                return calculateNetPay(e);
            }
        }
        return null; // employee not found
    }

    // FR3: Generate a payroll report
    public String generateReport() {
        if (employees.isEmpty()) {
            return "No employees found.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-15s %-12s %12s %12s%n",
                "ID", "Name", "Department", "BaseSalary", "NetPay"));
        for (Employee e : employees) {
            sb.append(String.format("%-8s %-15s %-12s %12.2f %12.2f%n",
                    e.getId(), e.getName(), e.getDepartment(),
                    e.getBaseSalary(), e.getNetPay()));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Input validation helpers. Each throws IllegalArgumentException with
    // a message explaining exactly what was wrong with the input.
    // ------------------------------------------------------------------

    static String validateId(String input) {
        String id = input.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty.");
        }
        if (!id.matches("[A-Za-z0-9-]+")) {
            throw new IllegalArgumentException(
                    "ID may only contain letters, digits and hyphens (no spaces or symbols).");
        }
        if (id.length() > 8) {
            throw new IllegalArgumentException("ID cannot be longer than 8 characters.");
        }
        return id;
    }

    static String validateName(String input) {
        String name = input.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (name.equals(EXIT_VALUE)) {
            throw new IllegalArgumentException("Name cannot be 0. Enter the employee's real name.");
        }
        if (!name.matches("[A-Za-z][A-Za-z .'-]*")) {
            throw new IllegalArgumentException(
                    "Name must start with a letter and contain only letters, spaces, apostrophes, periods or hyphens.");
        }
        if (name.length() > 15) {
            throw new IllegalArgumentException("Name cannot be longer than 15 characters.");
        }
        return name;
    }

    static String validateDepartment(String input) {
        String dept = input.trim();
        if (dept.isEmpty()) {
            throw new IllegalArgumentException("Department cannot be empty.");
        }
        if (!dept.matches("[A-Za-z][A-Za-z &-]*")) {
            throw new IllegalArgumentException(
                    "Department must start with a letter and contain only letters, spaces, '&' or hyphens.");
        }
        if (dept.length() > 12) {
            throw new IllegalArgumentException("Department cannot be longer than 12 characters.");
        }
        return dept;
    }

    static double validateSalary(String input) {
        String text = input.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Salary cannot be empty.");
        }
        double salary;
        try {
            salary = Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Salary must be a valid number (e.g. 450000 or 450000.50), but you entered: \"" + text + "\".");
        }
        if (Double.isNaN(salary) || Double.isInfinite(salary)) {
            throw new IllegalArgumentException("Salary must be a finite number.");
        }
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative.");
        }
        if (salary == 0) {
            throw new IllegalArgumentException("Salary must be greater than zero.");
        }
        if (salary < 60000){
            throw new IllegalArgumentException("Salary is too little");
        }
        return salary;
    }

    /**
     * Prompts repeatedly until the validator accepts the input.
     * The validator throws IllegalArgumentException with a helpful message
     * when the input is invalid; that message is shown and the prompt repeats.
     */
    private static <T> T promptUntilValid(Scanner scanner, String prompt, Function<String, T> validator) {
        while (true) {
            System.out.println(prompt);
            String raw = scanner.nextLine();
            try {
                return validator.apply(raw);
            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid input: " + ex.getMessage() + " Please try again.");
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        EmployeeManagementSystem system = new EmployeeManagementSystem();

        System.out.println("=== Employee Entry (enter 0 as the ID to finish) ===");

        try {
            while (true) {
                System.out.println();
                System.out.println("Enter the ID (or 0 to finish):");
                String rawId = scanner.nextLine().trim();

                if (rawId.equals(EXIT_VALUE)) {
                    break;
                }

                // Validate the ID; on failure show the message and restart the loop
                String id;
                try {
                    id = validateId(rawId);
                } catch (IllegalArgumentException ex) {
                    System.out.println("Invalid input: " + ex.getMessage() + " Please try again.");
                    continue;
                }

                String name = promptUntilValid(scanner, "Enter the name:", EmployeeManagementSystem::validateName);
                String department = promptUntilValid(scanner, "Enter the department:",
                        EmployeeManagementSystem::validateDepartment);
                double baseSalary = promptUntilValid(scanner, "Enter the base salary:",
                        EmployeeManagementSystem::validateSalary);

                try {
                    system.addEmployee(new Employee(id, name, department, baseSalary));
                    Double net = system.processPayment(id);
                    if (net == null) {
                        System.out.println("Warning: payment could not be processed. Employee " + id + " was not found.");
                    } else {
                        System.out.printf("Employee %s added. Net pay: %.2f%n", id, net);
                    }
                } catch (IllegalArgumentException ex) {
                    // e.g. duplicate ID
                    System.out.println("Could not add employee: " + ex.getMessage());
                }
            }
        } catch (NoSuchElementException ex) {
            // Input stream closed unexpectedly (e.g. Ctrl+D / end of piped input)
            System.out.println("\nInput ended unexpectedly. Generating report with the data entered so far.");
        } finally {
            scanner.close();
        }

        System.out.println();
        System.out.println(system.generateReport());
    }
}
