import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * JUnit 5 tests for EmployeeManagementSystem.
 * Must live in the same package as EmployeeManagementSystem (default package here),
 * because the validate* helpers are package-private.
 */
class EmployeeManagementSystemTest {

    private static final double DELTA = 0.001;

    private EmployeeManagementSystem system;

    @BeforeEach
    void setUp() {
        system = new EmployeeManagementSystem();
    }

    private static Employee emp(String id, double salary) {
        return new Employee(id, "Alice", "IT", salary);
    }

    private static String money(double value) {
        return String.format("%.2f", value);
    }

    // ------------------------------------------------------------------
    // FR1: Employee management
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("FR1 - add / remove / update")
    class EmployeeManagement {

        @Test
        void addEmployee_validEmployee_appearsInReport() {
            system.addEmployee(emp("E001", 300000));
            assertTrue(system.generateReport().contains("E001"));
        }

        @Test
        void addEmployee_null_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> system.addEmployee(null));
            assertEquals("Employee cannot be null", ex.getMessage());
        }

        @Test
        void addEmployee_negativeSalary_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> system.addEmployee(emp("E001", -1)));
            assertEquals("Salary cannot be negative", ex.getMessage());
        }

        @Test
        void addEmployee_duplicateId_throwsAndKeepsOriginal() {
            system.addEmployee(emp("E001", 300000));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> system.addEmployee(new Employee("E001", "Bob", "HR", 400000)));
            assertEquals("Duplicate employee ID: E001", ex.getMessage());

            String report = system.generateReport();
            assertTrue(report.contains("Alice"));
            assertFalse(report.contains("Bob"));
        }

        @Test
        void removeEmployee_existing_returnsTrueAndRemoves() {
            system.addEmployee(emp("E001", 300000));
            assertTrue(system.removeEmployee("E001"));
            assertEquals("No employees found.", system.generateReport());
        }

        @Test
        void removeEmployee_unknownId_returnsFalse() {
            system.addEmployee(emp("E001", 300000));
            assertFalse(system.removeEmployee("NOPE"));
            assertTrue(system.generateReport().contains("E001"));
        }

        @Test
        void removeEmployee_onlyRemovesMatchingEmployee() {
            system.addEmployee(emp("E001", 300000));
            system.addEmployee(new Employee("E002", "Bob", "HR", 400000));
            assertTrue(system.removeEmployee("E001"));

            String report = system.generateReport();
            assertFalse(report.contains("E001"));
            assertTrue(report.contains("E002"));
        }

        @Test
        void updateEmployee_existing_changesSalary() {
            system.addEmployee(emp("E001", 300000));
            assertTrue(system.updateEmployee("E001", 450000));

            // Net pay is derived from the updated salary: 450000 * 0.85
            assertEquals(382500.0, system.processPayment("E001"), DELTA);
        }

        @Test
        void updateEmployee_unknownId_returnsFalse() {
            assertFalse(system.updateEmployee("NOPE", 100000));
        }

        @Test
        void updateEmployee_negativeSalary_throwsAndLeavesSalaryUnchanged() {
            system.addEmployee(emp("E001", 300000));
            assertThrows(IllegalArgumentException.class, () -> system.updateEmployee("E001", -5));
            assertEquals(255000.0, system.processPayment("E001"), DELTA); // 300000 * 0.85
        }

        @Test
        void updateEmployee_unknownIdWithNegativeSalary_returnsFalse() {
            // Current behaviour: salary is only validated once the employee is found
            assertFalse(system.updateEmployee("NOPE", -5));
        }
    }

    // ------------------------------------------------------------------
    // FR2: Payment processing
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("FR2 - net pay calculation")
    class PaymentProcessing {

        @Test
        void calculateNetPay_lowBracket_tenPercentTaxPlusPension() {
            // 300000 - 10% - 5% = 255000
            assertEquals(255000.0, system.calculateNetPay(emp("E001", 300000)), DELTA);
        }

        @Test
        void calculateNetPay_upperBoundOfLowBracket_500000() {
            // 500000 still uses 10% tax: 500000 * 0.85
            assertEquals(425000.0, system.calculateNetPay(emp("E001", 500000)), DELTA);
        }

        @Test
        void calculateNetPay_justAboveLowBracket_usesTwentyPercentTax() {
            double salary = 500000.01;
            assertEquals(salary * 0.75, system.calculateNetPay(emp("E001", salary)), DELTA);
        }

        @Test
        void calculateNetPay_middleBracket_twentyPercentTax() {
            // 750000 - 20% - 5% = 562500
            assertEquals(562500.0, system.calculateNetPay(emp("E001", 750000)), DELTA);
        }

        @Test
        void calculateNetPay_upperBoundOfMiddleBracket_1000000() {
            // 1,000,000 still uses 20% tax: 1000000 * 0.75
            assertEquals(750000.0, system.calculateNetPay(emp("E001", 1000000)), DELTA);
        }

        @Test
        void calculateNetPay_justAboveMiddleBracket_usesThirtyPercentTax() {
            double salary = 1000000.01;
            assertEquals(salary * 0.65, system.calculateNetPay(emp("E001", salary)), DELTA);
        }

        @Test
        void calculateNetPay_highBracket_thirtyPercentTax() {
            // 2,000,000 - 30% - 5% = 1,300,000
            assertEquals(1300000.0, system.calculateNetPay(emp("E001", 2000000)), DELTA);
        }

        @Test
        void calculateNetPay_zeroSalary_returnsZero() {
            assertEquals(0.0, system.calculateNetPay(emp("E001", 0)), DELTA);
        }

        @Test
        void calculateNetPay_storesResultOnEmployee() {
            Employee e = emp("E001", 300000);
            assertEquals(0.0, e.getNetPay(), DELTA);
            double net = system.calculateNetPay(e);
            assertEquals(net, e.getNetPay(), DELTA);
        }

        @Test
        void processPayment_existingEmployee_returnsNetPay() {
            system.addEmployee(emp("E001", 300000));
            Double net = system.processPayment("E001");
            assertNotNull(net);
            assertEquals(255000.0, net, DELTA);
        }

        @Test
        void processPayment_unknownEmployee_returnsNull() {
            assertNull(system.processPayment("NOPE"));
        }
    }

    // ------------------------------------------------------------------
    // FR3: Reporting
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("FR3 - report generation")
    class Reporting {

        @Test
        void generateReport_noEmployees_returnsMessage() {
            assertEquals("No employees found.", system.generateReport());
        }

        @Test
        void generateReport_containsHeaderAndEmployeeDetails() {
            system.addEmployee(new Employee("E001", "Alice", "IT", 300000));
            String report = system.generateReport();

            for (String header : new String[] {"ID", "Name", "Department", "BaseSalary", "NetPay"}) {
                assertTrue(report.contains(header), "Missing header: " + header);
            }
            assertTrue(report.contains("E001"));
            assertTrue(report.contains("Alice"));
            assertTrue(report.contains("IT"));
            assertTrue(report.contains(money(300000)));
        }

        @Test
        void generateReport_netPayIsZeroUntilPaymentProcessed() {
            system.addEmployee(emp("E001", 300000));
            assertTrue(system.generateReport().contains(money(0)));
            assertFalse(system.generateReport().contains(money(255000)));
        }

        @Test
        void generateReport_showsNetPayAfterPaymentProcessed() {
            system.addEmployee(emp("E001", 300000));
            system.processPayment("E001");
            assertTrue(system.generateReport().contains(money(255000)));
        }

        @Test
        void generateReport_listsAllEmployeesOnePerLine() {
            system.addEmployee(emp("E001", 300000));
            system.addEmployee(new Employee("E002", "Bob", "HR", 400000));
            system.addEmployee(new Employee("E003", "Cara", "Finance", 600000));

            String[] lines = system.generateReport().trim().split("\\R");
            assertEquals(4, lines.length); // header + 3 employees
        }
    }

    // ------------------------------------------------------------------
    // Input validation helpers
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("Validation - ID")
    class IdValidation {

        @ParameterizedTest
        @ValueSource(strings = {"E001", "a", "AB-12", "12345678"})
        void validId_isAccepted(String input) {
            assertEquals(input, EmployeeManagementSystem.validateId(input));
        }

        @Test
        void validId_isTrimmed() {
            assertEquals("E001", EmployeeManagementSystem.validateId("  E001  "));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void emptyId_throws(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateId(input));
            assertEquals("ID cannot be empty.", ex.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"E 01", "E_01", "E@01", "E.01"})
        void idWithInvalidCharacters_throws(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateId(input));
            assertTrue(ex.getMessage().contains("letters, digits and hyphens"));
        }

        @Test
        void idLongerThanEight_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateId("123456789"));
            assertEquals("ID cannot be longer than 8 characters.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Validation - Name")
    class NameValidation {

        @ParameterizedTest
        @ValueSource(strings = {"Alice", "Mary O'Neil", "Jean-Luc", "J. Smith"})
        void validName_isAccepted(String input) {
            assertEquals(input, EmployeeManagementSystem.validateName(input));
        }

        @Test
        void validName_isTrimmed() {
            assertEquals("Alice", EmployeeManagementSystem.validateName("  Alice "));
        }

        @Test
        void nameAtMaxLength_isAccepted() {
            String name = "A".repeat(15);
            assertEquals(name, EmployeeManagementSystem.validateName(name));
        }

        @Test
        void emptyName_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateName("   "));
            assertEquals("Name cannot be empty.", ex.getMessage());
        }

        @Test
        void nameZero_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateName("0"));
            assertTrue(ex.getMessage().contains("Name cannot be 0"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"1Alice", "Alice3", "-Alice", "Al!ce", "Al_ice"})
        void nameWithInvalidCharacters_throws(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateName(input));
            assertTrue(ex.getMessage().contains("Name must start with a letter"));
        }

        @Test
        void nameLongerThanFifteen_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateName("A".repeat(16)));
            assertEquals("Name cannot be longer than 15 characters.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Validation - Department")
    class DepartmentValidation {

        @ParameterizedTest
        @ValueSource(strings = {"IT", "R&D", "Human-Res", "Sales Ops"})
        void validDepartment_isAccepted(String input) {
            assertEquals(input, EmployeeManagementSystem.validateDepartment(input));
        }

        @Test
        void departmentAtMaxLength_isAccepted() {
            String dept = "D".repeat(12);
            assertEquals(dept, EmployeeManagementSystem.validateDepartment(dept));
        }

        @Test
        void emptyDepartment_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateDepartment(""));
            assertEquals("Department cannot be empty.", ex.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"IT2", "1IT", "&IT", "I.T"})
        void departmentWithInvalidCharacters_throws(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateDepartment(input));
            assertTrue(ex.getMessage().contains("Department must start with a letter"));
        }

        @Test
        void departmentLongerThanTwelve_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateDepartment("D".repeat(13)));
            assertEquals("Department cannot be longer than 12 characters.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Validation - Salary")
    class SalaryValidation {

        @Test
        void validSalary_integerAndDecimal() {
            assertEquals(450000.0, EmployeeManagementSystem.validateSalary("450000"), DELTA);
            assertEquals(450000.50, EmployeeManagementSystem.validateSalary("450000.50"), DELTA);
        }

        @Test
        void validSalary_isTrimmed() {
            assertEquals(100000.0, EmployeeManagementSystem.validateSalary("  100000 "), DELTA);
        }

        @Test
        void salaryAtMinimum_60000_isAccepted() {
            assertEquals(60000.0, EmployeeManagementSystem.validateSalary("60000"), DELTA);
        }

        @Test
        void salaryJustBelowMinimum_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateSalary("59999.99"));
            assertEquals("Salary is too little", ex.getMessage());
        }

        @Test
        void emptySalary_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateSalary(" "));
            assertEquals("Salary cannot be empty.", ex.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "12,000", "1 000", "$5000"})
        void nonNumericSalary_throws(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateSalary(input));
            assertTrue(ex.getMessage().contains("valid number"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"NaN", "Infinity", "-Infinity"})
        void nanAndInfinity_throw(String input) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateSalary(input));
            assertEquals("Salary must be a finite number.", ex.getMessage());
        }

        @Test
        void negativeSalary_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateSalary("-100"));
            assertEquals("Salary cannot be negative.", ex.getMessage());
        }

        @Test
        void zeroSalary_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EmployeeManagementSystem.validateSalary("0"));
            assertEquals("Salary must be greater than zero.", ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // main(): console flow, driven with simulated System.in / System.out
    // ------------------------------------------------------------------
    @Nested
    @DisplayName("main() console flow")
    class MainFlow {

        private InputStream originalIn;
        private PrintStream originalOut;
        private ByteArrayOutputStream captured;

        @BeforeEach
        void redirectStreams() {
            originalIn = System.in;
            originalOut = System.out;
            captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        }

        @AfterEach
        void restoreStreams() {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }

        private String runMain(String input) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            EmployeeManagementSystem.main(new String[0]);
            return captured.toString(StandardCharsets.UTF_8);
        }

        @Test
        void exitImmediately_printsEmptyReport() {
            String out = runMain("0\n");
            assertTrue(out.contains("No employees found."));
        }

        @Test
        void addOneEmployee_showsNetPayAndReport() {
            String out = runMain("E001\nAlice\nIT\n450000\n0\n");

            assertTrue(out.contains("Employee E001 added. Net pay: " + money(382500)));
            assertTrue(out.contains("Alice"));
            assertTrue(out.contains("BaseSalary"));
        }

        @Test
        void invalidId_showsErrorAndContinues() {
            String out = runMain("bad id\n0\n");

            assertTrue(out.contains("Invalid input: ID may only contain letters, digits and hyphens"));
            assertTrue(out.contains("No employees found."));
        }

        @Test
        void invalidFields_arePromptedAgainUntilValid() {
            // empty name -> retry, bad department -> retry, salary below minimum -> retry
            String input = String.join("\n",
                    "E001",
                    "", "Alice",
                    "IT2", "IT",
                    "100", "abc", "450000",
                    "0") + "\n";
            String out = runMain(input);

            assertTrue(out.contains("Invalid input: Name cannot be empty."));
            assertTrue(out.contains("Invalid input: Department must start with a letter"));
            assertTrue(out.contains("Invalid input: Salary is too little"));
            assertTrue(out.contains("Invalid input: Salary must be a valid number"));
            assertTrue(out.contains("Employee E001 added."));
        }

        @Test
        void duplicateId_isRejectedWithMessage() {
            String input = "E001\nAlice\nIT\n450000\n"
                         + "E001\nBob\nHR\n300000\n"
                         + "0\n";
            String out = runMain(input);

            assertTrue(out.contains("Could not add employee: Duplicate employee ID: E001"));
            assertTrue(out.contains("Alice"));
            assertFalse(out.contains("Bob  "), "Duplicate employee must not appear in the report");
        }

        @Test
        void multipleEmployees_allAppearInReport() {
            String input = "E001\nAlice\nIT\n450000\n"
                         + "E002\nBob\nHR\n750000\n"
                         + "0\n";
            String out = runMain(input);

            assertTrue(out.contains("Employee E001 added. Net pay: " + money(382500)));
            assertTrue(out.contains("Employee E002 added. Net pay: " + money(562500)));
        }

        @Test
        void inputEndsUnexpectedly_stillGeneratesReport() {
            // Stream ends while waiting for the department
            String out = runMain("E001\nAlice\n");

            assertTrue(out.contains("Input ended unexpectedly"));
            assertTrue(out.contains("No employees found."));
        }
    }
}
