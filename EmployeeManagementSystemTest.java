/**
 * EmployeeManagementSystemTest.java
 * Plain-Java test harness — no JUnit required.
 * 17 Black-Box tests (BB-01 … BB-17)
 * 16 White-Box tests (WB-01 … WB-16)
 * Run: java EmployeeManagementSystemTest
 */
public class EmployeeManagementSystemTest {

    // ── tiny assertion helpers ────────────────────────────────────────────────
    private static int pass = 0, fail = 0;

    private static void check(String id, boolean expected, boolean actual) {
        String result = (expected == actual) ? "PASS" : "FAIL";
        System.out.printf("%-6s | expected=%-5s | actual=%-5s | %s%n",
                id, expected, actual, result);
        if (result.equals("PASS")) pass++; else fail++;
    }

    private static void checkDouble(String id, double expected, double actual, double delta) {
        boolean ok = Math.abs(expected - actual) <= delta;
        String result = ok ? "PASS" : "FAIL";
        System.out.printf("%-6s | expected=%-10.2f | actual=%-10.2f | %s%n",
                id, expected, actual, result);
        if (ok) pass++; else fail++;
    }

    private static void checkInt(String id, int expected, int actual) {
        boolean ok = expected == actual;
        String result = ok ? "PASS" : "FAIL";
        System.out.printf("%-6s | expected=%-5d | actual=%-5d | %s%n",
                id, expected, actual, result);
        if (ok) pass++; else fail++;
    }

    private static void checkContains(String id, String keyword, String actual) {
        boolean ok = actual != null && actual.contains(keyword);
        String result = ok ? "PASS" : "FAIL";
        System.out.printf("%-6s | expected contains='%s' | actual='%.40s...' | %s%n",
                id, keyword, actual == null ? "null" : actual, result);
        if (ok) pass++; else fail++;
    }

    // ── main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        System.out.println("========== BLACK-BOX TESTS ==========");
        runBlackBoxTests();

        System.out.println("\n========== WHITE-BOX TESTS ==========");
        runWhiteBoxTests();

        System.out.printf("%nTOTAL pass=%d fail=%d%n", pass, fail);
    }

    // =========================================================================
    //  BLACK-BOX TESTS  (BB-01 … BB-17)
    //  Based purely on the requirements — no knowledge of internal code needed.
    // =========================================================================
    private static void runBlackBoxTests() {

        EmployeeManagementSystem sys = new EmployeeManagementSystem();

        // BB-01: Add a valid employee → should return true
        check("BB-01", true, sys.addEmployee("E001", "Alice", "Engineering", 3000));

        // BB-02: Add another valid employee → should return true
        check("BB-02", true, sys.addEmployee("E002", "Bob", "Management", 4800));

        // BB-03: Add employee with empty ID → should return false
        check("BB-03", false, sys.addEmployee("", "Carol", "HR", 2000));

        // BB-04: Add employee with empty name → should return false
        check("BB-04", false, sys.addEmployee("E003", "", "HR", 2000));

        // BB-05: Add employee with negative salary → should return false
        check("BB-05", false, sys.addEmployee("E004", "Dave", "IT", -500));

        // BB-06: Add duplicate employee ID → should return false
        check("BB-06", false, sys.addEmployee("E001", "Eve", "Finance", 3500));

        // BB-07: Employee count after 2 successful adds → should be 2
        checkInt("BB-07", 2, sys.getEmployeeCount());

        // BB-08: Remove existing employee → should return true
        check("BB-08", true, sys.removeEmployee("E002"));

        // BB-09: Remove non-existent employee → should return false
        check("BB-09", false, sys.removeEmployee("E999"));

        // BB-10: Employee count after removal → should be 1
        checkInt("BB-10", 1, sys.getEmployeeCount());

        // BB-11: Process payment for non-existent employee → should return -1
        checkDouble("BB-11", -1.0, sys.processPayment("E999", 160, 0), 0.001);

        // BB-12: Process payment with negative hours → should return -1
        checkDouble("BB-12", -1.0, sys.processPayment("E001", -10, 0), 0.001);

        // BB-13: Tax boundary — gross exactly 5000 → 25% tax
        //   E001: baseSalary=3000, hourlyRate=18.75
        //   Need gross=5000 → hoursWorked = 5000/18.75 = 266.67 hours, 0 overtime
        //   netPay = 5000 * 0.75 = 3750.00
        EmployeeManagementSystem sys2 = new EmployeeManagementSystem();
        sys2.addEmployee("E010", "Frank", "Engineering", 3000);
        // hoursWorked to hit exactly 5000 gross: 5000 / (3000/160) = 266.6667
        checkDouble("BB-13", 3750.00, sys2.processPayment("E010", 266.6667, 0), 0.10);

        // BB-14: Tax below boundary — gross < 5000 → 15% tax
        //   E001 baseSalary=3000, 160 hours, 0 OT → gross=3000, net=3000*0.85=2550
        checkDouble("BB-14", 2550.00, sys.processPayment("E001", 160, 0), 0.001);

        // BB-15: Overtime pay — 10 OT hours on E001 (baseSalary=3000)
        //   hourlyRate=18.75, OT pay=18.75*1.5*10=281.25
        //   gross=3000+281.25=3281.25, tax=15%, net=3281.25*0.85=2789.0625
        EmployeeManagementSystem sys3 = new EmployeeManagementSystem();
        sys3.addEmployee("E020", "Grace", "Engineering", 3000);
        checkDouble("BB-15", 2789.06, sys3.processPayment("E020", 160, 10), 0.01);

        // BB-16: Management bonus — Bob baseSalary=4800, 160 hours, 0 OT
        //   hourlyRate=30, gross=4800, +10%=5280, tax=25%, net=5280*0.75=3960
        EmployeeManagementSystem sys4 = new EmployeeManagementSystem();
        sys4.addEmployee("E030", "Henry", "Management", 4800);
        checkDouble("BB-16", 3960.00, sys4.processPayment("E030", 160, 0), 0.001);

        // BB-17: Generate report when no payments processed → contains "No payroll"
        EmployeeManagementSystem sys5 = new EmployeeManagementSystem();
        checkContains("BB-17", "No payroll", sys5.generatePayrollReport());
    }

    // =========================================================================
    //  WHITE-BOX TESTS  (WB-01 … WB-16)
    //  Based on internal branches in the source code.
    // =========================================================================
    private static void runWhiteBoxTests() {

        // WB-01: Branch — empty ID check (id.trim().isEmpty())
        EmployeeManagementSystem s = new EmployeeManagementSystem();
        check("WB-01", false, s.addEmployee("   ", "Alice", "IT", 2000));

        // WB-02: Branch — empty name check (name.trim().isEmpty())
        check("WB-02", false, s.addEmployee("E001", "   ", "IT", 2000));

        // WB-03: Branch — negative salary check (baseSalary < 0)
        check("WB-03", false, s.addEmployee("E001", "Alice", "IT", -1));

        // WB-04: Branch — duplicate ID check (employees.containsKey(id))
        s.addEmployee("E001", "Alice", "IT", 2000);
        check("WB-04", false, s.addEmployee("E001", "Bob", "IT", 3000));

        // WB-05: Branch — negative hoursWorked check (hoursWorked < 0)
        checkDouble("WB-05", -1.0, s.processPayment("E001", -1, 0), 0.001);

        // WB-06: Branch — negative overtimeHours check (overtimeHours < 0)
        checkDouble("WB-06", -1.0, s.processPayment("E001", 160, -5), 0.001);

        // WB-07: Branch — Management bonus applied (department == "Management")
        EmployeeManagementSystem s2 = new EmployeeManagementSystem();
        s2.addEmployee("M001", "Manager", "Management", 3200);
        // hourlyRate=20, gross=3200, +10%=3520, tax=15%, net=3520*0.85=2992
        checkDouble("WB-07", 2992.00, s2.processPayment("M001", 160, 0), 0.001);

        // WB-08: Branch — Non-Management, no bonus applied
        EmployeeManagementSystem s3 = new EmployeeManagementSystem();
        s3.addEmployee("E002", "Engineer", "Engineering", 3200);
        // gross=3200, tax=15%, net=3200*0.85=2720
        checkDouble("WB-08", 2720.00, s3.processPayment("E002", 160, 0), 0.001);

        // WB-09: Branch — grossPay >= 5000 → taxRate = 0.25
        EmployeeManagementSystem s4 = new EmployeeManagementSystem();
        s4.addEmployee("E003", "Senior", "Engineering", 5000);
        // hourlyRate=31.25, gross=5000, tax=25%, net=5000*0.75=3750
        checkDouble("WB-09", 3750.00, s4.processPayment("E003", 160, 0), 0.001);

        // WB-10: Branch — grossPay < 5000 → taxRate = 0.15
        EmployeeManagementSystem s5 = new EmployeeManagementSystem();
        s5.addEmployee("E004", "Junior", "Engineering", 2400);
        // hourlyRate=15, gross=2400, tax=15%, net=2400*0.85=2040
        checkDouble("WB-10", 2040.00, s5.processPayment("E004", 160, 0), 0.001);

        // WB-11: Branch — empty payroll report (paymentRecords.isEmpty())
        EmployeeManagementSystem s6 = new EmployeeManagementSystem();
        checkContains("WB-11", "No payroll", s6.generatePayrollReport());

        // WB-12: Branch — top earner identified in report
        EmployeeManagementSystem s7 = new EmployeeManagementSystem();
        s7.addEmployee("E005", "LowPay",  "Engineering", 2000);
        s7.addEmployee("E006", "HighPay", "Engineering", 6400);
        s7.processPayment("E005", 160, 0);
        s7.processPayment("E006", 160, 0);
        checkContains("WB-12", "HighPay", s7.generatePayrollReport());

        // WB-13: Branch — removeEmployee success path (employee exists)
        EmployeeManagementSystem s8 = new EmployeeManagementSystem();
        s8.addEmployee("E007", "ToRemove", "HR", 2500);
        check("WB-13", true, s8.removeEmployee("E007"));

        // WB-14: Branch — removeEmployee failure path (employee not found)
        check("WB-14", false, s8.removeEmployee("E007")); // already removed

        // WB-15: Branch — getEmployeeCount reflects correct count after adds
        EmployeeManagementSystem s9 = new EmployeeManagementSystem();
        s9.addEmployee("E008", "One",   "IT", 2000);
        s9.addEmployee("E009", "Two",   "IT", 2000);
        s9.addEmployee("E010", "Three", "IT", 2000);
        checkInt("WB-15", 3, s9.getEmployeeCount());

        // WB-16: Branch — overtime pay calculation (hourlyRate * 1.5 * overtimeHours)
        EmployeeManagementSystem s10 = new EmployeeManagementSystem();
        s10.addEmployee("E011", "OTWorker", "Engineering", 3200);
        // hourlyRate=20, regular=3200, OT=20*1.5*20=600, gross=3800, tax=15%, net=3800*0.85=3230
        checkDouble("WB-16", 3230.00, s10.processPayment("E011", 160, 20), 0.001);
    }
}
