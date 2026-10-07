# Employee Management – Database Programming Explained

This project shows every database requirement with the simplest possible JDBC + Oracle code.

| Requirement | Where |
|---|---|
| Read | `EmployeeDAO.getAllEmployees()` |
| Insert / Update / Delete | `insertEmployee()`, `updateEmployee()`, `deleteEmployee()` |
| Procedure call | `addEmployeeWithProcedure()` → `ADD_EMPLOYEE` |
| Return result set in procedure | `getEmployeesByDepartment()` → `GET_EMPLOYEES_BY_DEPT` |
| Procedure call with object | `addEmployeeWithObject()` → `ADD_EMPLOYEE_OBJ` |

**Order to run:** 1) run `sql/EMPLOYEE.sql` in SQL Developer (F5 "Run Script"), 2) fill in your credentials in `DBConnection.java`, 3) run `DBConnection` to test the connection, 4) run `MainApp`.

The flow is always: **MainApp → EmployeeDAO → DBConnection → Oracle**, with `Employee` carrying the data between them.

---

## 1. `sql/EMPLOYEE.sql` – the database side

### The table
```sql
CREATE TABLE EMPLOYEE (
    ID          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
```
- `ID` – Oracle generates the number itself (1, 2, 3…). That's why Java never sends an ID on insert. `PRIMARY KEY` = unique and not null.
```sql
    NAME        VARCHAR2(100) NOT NULL,
    DEPARTMENT  VARCHAR2(100),
    SALARY      NUMBER(10,2),
    BONUS       NUMBER(10,2),
```
- Text columns up to 100 chars; `NAME` is required. `NUMBER(10,2)` = up to 10 digits, 2 after the decimal point.
```sql
    INSERT_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```
- Audit columns. If you don't give a value, Oracle fills in "now". Our UPDATE statement sets `UPDATE_DATE` again.

### Procedure 1 – simple procedure call
```sql
CREATE OR REPLACE PROCEDURE ADD_EMPLOYEE (
    p_name       IN VARCHAR2,
    p_department IN VARCHAR2,
    p_salary     IN NUMBER,
    p_bonus      IN NUMBER
) AS
BEGIN
    INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS)
    VALUES (p_name, p_department, p_salary, p_bonus);
END;
/
```
- `CREATE OR REPLACE` – create it, or overwrite it if it already exists (so you can re-run the script).
- `IN` parameters – values coming **in** from Java.
- Body is a normal INSERT using the parameters.
- `/` – tells SQL Developer "the PL/SQL block ends here, run it". Needed after every procedure/type.

### Procedure 2 – returns a result set
```sql
CREATE OR REPLACE PROCEDURE GET_EMPLOYEES_BY_DEPT (
    p_department IN  VARCHAR2,
    p_result     OUT SYS_REFCURSOR
) AS
BEGIN
    OPEN p_result FOR
        SELECT ID, NAME, DEPARTMENT, SALARY, BONUS
        FROM EMPLOYEE
        WHERE DEPARTMENT = p_department;
END;
/
```
- A procedure can't `return` rows directly. Instead it has an `OUT` parameter of type `SYS_REFCURSOR` – a **pointer to the rows of a query**.
- `OPEN p_result FOR SELECT …` runs the query and points the cursor at the results.
- Java receives this cursor and reads it as a normal `ResultSet`.

### Procedure 3 – called with an object
```sql
CREATE OR REPLACE TYPE EMPLOYEE_OBJ AS OBJECT (
    NAME       VARCHAR2(100),
    DEPARTMENT VARCHAR2(100),
    SALARY     NUMBER(10,2),
    BONUS      NUMBER(10,2)
);
/
```
- Creates a custom Oracle **object type** – basically the database version of our Java `Employee` class (one "package" holding 4 fields).
```sql
CREATE OR REPLACE PROCEDURE ADD_EMPLOYEE_OBJ (
    p_emp IN EMPLOYEE_OBJ
) AS
BEGIN
    INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS)
    VALUES (p_emp.NAME, p_emp.DEPARTMENT, p_emp.SALARY, p_emp.BONUS);
END;
/
```
- Takes **one** parameter (the whole employee) instead of four.
- `p_emp.NAME` reads a field from the object, just like `e.getName()` in Java.

---

## 2. `src/module-info.java`
```java
module qnbmanagementsystem {
    requires java.sql;
}
```
- Eclipse created this because the project uses Java modules. `requires java.sql` gives us access to `Connection`, `PreparedStatement`, `ResultSet`, etc. Without it nothing in `java.sql` compiles.
- (If your existing module-info has a different module name, keep your name – only the `requires java.sql;` line matters.)

---

## 3. `com.qnb.util.DBConnection` – opens the connection
```java
package com.qnb.util;
```
- Which package (folder) this class lives in.
```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
```
- The JDBC classes we use.
```java
private static final String URL = "jdbc:oracle:thin:@//HOST:PORT/SERVICE_NAME";
private static final String USER = "YOUR_USER";
private static final String PASSWORD = "YOUR_PASSWORD";
```
- Connection details. `jdbc:oracle:thin` = use Oracle's pure-Java ("thin") driver (the ojdbc jar in *Referenced Libraries*). `@//host:port/service` = where the database is.
- `private static final` = a constant belonging to the class, can't be changed, hidden from other classes.
- **Put your real values here on your machine. They are placeholders in GitHub on purpose – never push real passwords.**
```java
public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASSWORD);
}
```
- Every DAO method calls this to get a fresh connection. `DriverManager` finds the Oracle driver and logs in. `throws SQLException` = the caller must handle a failure.
```java
public static void main(String[] args) {
    try (Connection conn = getConnection()) {
        System.out.println("Connected successfully!");
    } catch (SQLException e) {
        System.out.println("Connection failed:");
        e.printStackTrace();
    }
}
```
- A quick test: run this class alone to check the login works.
- `try ( … )` is **try-with-resources**: the connection is closed automatically at the end, even on error.

---

## 4. `com.qnb.model.Employee` – the data holder (POJO)
```java
private int id;
private String name;
private String department;
private double salary;
private double bonus;
```
- One field per table column. `private` = only accessible through getters/setters (encapsulation).
```java
public Employee() { }
```
- Empty constructor – create a blank employee and fill it with setters.
```java
public Employee(String name, String department, double salary, double bonus) { … }
```
- Used for **inserting**: no ID, because Oracle generates it.
```java
public Employee(int id, String name, String department, double salary, double bonus) { … }
```
- Used for **reading**: the row from the database already has an ID. `this.name = name` means "the field `name` = the parameter `name`".
```java
public int getId() { return id; }
public void setId(int id) { this.id = id; }
… (same for name, department, salary, bonus)
```
- Getters read a field, setters change it. The DAO uses getters to put values into SQL.
```java
@Override
public String toString() {
    return "ID: " + id + " | Name: " + name + …;
}
```
- Decides what `System.out.println(employee)` prints. `@Override` = we are replacing Java's default `toString()` (which would print something like `Employee@1b6d3586`).

---

## 5. `com.qnb.dao.EmployeeDAO` – all database work
DAO = **Data Access Object**: the only class that writes SQL. Every method follows the same 4 steps:
1. Write the SQL with `?` placeholders.
2. Open connection + statement in `try ( … )` so they auto-close.
3. Fill the `?` (numbered from **1**) and execute.
4. `catch (SQLException ex)` prints any database error.

### Imports
- `Employee`, `DBConnection` – our own classes.
- `CallableStatement` – for calling procedures. `PreparedStatement` – for normal SQL. `ResultSet` – rows returned from a query. `Struct` – an Oracle object in Java. `Types` – SQL type constants (we use `REF_CURSOR`). `List/ArrayList` – to return many employees.

### INSERT – `insertEmployee(Employee e)`
```java
String sql = "INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS) VALUES (?, ?, ?, ?)";
```
- `?` are placeholders. Using them (instead of gluing strings together) **prevents SQL injection** and handles quotes for you.
```java
try (Connection conn = DBConnection.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
```
- Opens the connection and prepares (pre-compiles) the SQL. Both close automatically.
```java
stmt.setString(1, e.getName());
stmt.setString(2, e.getDepartment());
stmt.setDouble(3, e.getSalary());
stmt.setDouble(4, e.getBonus());
```
- Fill `?` number 1, 2, 3, 4 with values from the employee. Use the setter matching the type (`setString`, `setDouble`, `setInt`).
```java
int rows = stmt.executeUpdate();
System.out.println(rows + " row(s) inserted.");
```
- `executeUpdate()` is used for INSERT/UPDATE/DELETE. It returns **how many rows changed**.
- JDBC is in *auto-commit* mode by default, so the change is saved immediately.

### READ – `getAllEmployees()`
```java
List<Employee> employees = new ArrayList<>();
String sql = "SELECT ID, NAME, DEPARTMENT, SALARY, BONUS FROM EMPLOYEE";
```
- An empty list we will fill, and the query.
```java
try (Connection conn = DBConnection.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
```
- `executeQuery()` is used for SELECT. It returns a `ResultSet` – like a table you read row by row.
```java
while (rs.next()) {
    employees.add(new Employee(
            rs.getInt("ID"),
            rs.getString("NAME"),
            rs.getString("DEPARTMENT"),
            rs.getDouble("SALARY"),
            rs.getDouble("BONUS")));
}
```
- `rs.next()` moves to the next row; returns `false` when there are no more rows → loop ends.
- `rs.getXxx("COLUMN")` reads a column of the current row. We build an `Employee` (using the 5-argument "reading" constructor) and add it to the list.
```java
return employees;
```
- Give the list back to the caller (empty if there was an error).

### UPDATE – `updateEmployee(Employee e)`
```java
String sql = "UPDATE EMPLOYEE SET NAME = ?, DEPARTMENT = ?, SALARY = ?, BONUS = ?, "
        + "UPDATE_DATE = CURRENT_TIMESTAMP WHERE ID = ?";
```
- Change all fields of the row whose `ID` matches. `UPDATE_DATE = CURRENT_TIMESTAMP` records when it changed.
```java
stmt.setString(1, e.getName());
… 
stmt.setInt(5, e.getId());
```
- Same as insert, plus the 5th `?` is the ID in the `WHERE`.
- `executeUpdate()` returns `1` if the employee was found and `0` if no such ID.

### DELETE – `deleteEmployee(int id)`
```java
String sql = "DELETE FROM EMPLOYEE WHERE ID = ?";
stmt.setInt(1, id);
int rows = stmt.executeUpdate();
```
- Delete the row with that ID; prints how many rows were deleted.

### PROCEDURE CALL – `addEmployeeWithProcedure(Employee e)`
```java
String sql = "{call ADD_EMPLOYEE(?, ?, ?, ?)}";
```
- `{call NAME(?, …)}` is the standard JDBC syntax for calling a stored procedure.
```java
try (Connection conn = DBConnection.getConnection();
     CallableStatement stmt = conn.prepareCall(sql)) {
```
- `prepareCall` gives a `CallableStatement` – the statement type for procedures (it supports OUT parameters too).
```java
stmt.setString(1, e.getName());
stmt.setString(2, e.getDepartment());
stmt.setDouble(3, e.getSalary());
stmt.setDouble(4, e.getBonus());
stmt.execute();
```
- Fill the 4 IN parameters (same order as in the procedure) and run it. `execute()` is the general "run it" method used for procedures.

### PROCEDURE RETURNING A RESULT SET – `getEmployeesByDepartment(String department)`
```java
String sql = "{call GET_EMPLOYEES_BY_DEPT(?, ?)}";
```
- 1st `?` = IN department, 2nd `?` = OUT cursor.
```java
stmt.setString(1, department);
stmt.registerOutParameter(2, Types.REF_CURSOR);
```
- Set the IN value. For an OUT parameter we don't set a value – we **register** it, telling JDBC "parameter 2 will come back as a cursor".
```java
stmt.execute();
try (ResultSet rs = stmt.getObject(2, ResultSet.class)) {
```
- After running, get parameter 2 back **as a `ResultSet`**. From here it's exactly like the READ method.
```java
    while (rs.next()) {
        employees.add(new Employee(rs.getInt("ID"), …));
    }
}
return employees;
```
- Loop over the rows, build employees, return the list.

### PROCEDURE CALL WITH OBJECT – `addEmployeeWithObject(Employee e)`
```java
String sql = "{call ADD_EMPLOYEE_OBJ(?)}";
```
- Only **one** `?` – the whole employee object.
```java
Object[] attributes = { e.getName(), e.getDepartment(), e.getSalary(), e.getBonus() };
```
- The values of the object's fields, **in the same order as in `CREATE TYPE EMPLOYEE_OBJ`** (NAME, DEPARTMENT, SALARY, BONUS).
```java
Struct empObj = conn.createStruct("EMPLOYEE_OBJ", attributes);
```
- Builds a real Oracle `EMPLOYEE_OBJ` in Java. The type name must be in **UPPERCASE**, as Oracle stores it.
```java
stmt.setObject(1, empObj);
stmt.execute();
```
- Send the object as the single parameter and run the procedure.

---

## 6. `com.qnb.main.MainApp` – runs everything in order
```java
EmployeeDAO dao = new EmployeeDAO();
```
- Create the DAO so we can call its methods.
```java
dao.insertEmployee(new Employee("Noel Thomas", "IT", 12000.0, 500.0));
```
- **1. INSERT** – uses the 4-argument constructor (no ID).
```java
List<Employee> employees = dao.getAllEmployees();
for (Employee e : employees) { System.out.println(e); }
```
- **2. READ** – get all rows and print each one (uses `toString()`).
```java
Employee toUpdate = employees.get(0);
toUpdate.setSalary(15000.0);
toUpdate.setBonus(1000.0);
dao.updateEmployee(toUpdate);
```
- **3. UPDATE** – take the first employee we just read (it already has a real ID from the database, so we don't hard-code `1`), change salary and bonus, save it.
```java
dao.addEmployeeWithProcedure(new Employee("Sara Ali", "HR", 9000.0, 300.0));
```
- **4. PROCEDURE CALL** – inserts through `ADD_EMPLOYEE`.
```java
dao.addEmployeeWithObject(new Employee("Omar Khan", "IT", 11000.0, 400.0));
```
- **5. PROCEDURE WITH OBJECT** – inserts through `ADD_EMPLOYEE_OBJ`.
```java
for (Employee e : dao.getEmployeesByDepartment("IT")) { System.out.println(e); }
```
- **6. PROCEDURE RETURNING RESULT SET** – prints only IT employees (Sara from HR won't appear).
```java
dao.deleteEmployee(toUpdate.getId());
```
- **7. DELETE** – removes the employee we updated.
```java
for (Employee e : dao.getAllEmployees()) { System.out.println(e); }
```
- Prints the final table so you can see the result of everything.

---

## Quick cheat-sheet (good for explaining)
| Use | Statement | Execute with | Returns |
|---|---|---|---|
| SELECT | `PreparedStatement` | `executeQuery()` | `ResultSet` |
| INSERT / UPDATE / DELETE | `PreparedStatement` | `executeUpdate()` | number of rows |
| Stored procedure | `CallableStatement` (`{call …}`) | `execute()` | OUT params via `getXxx()` |
| Procedure OUT cursor | `registerOutParameter(n, Types.REF_CURSOR)` | `execute()` | `getObject(n, ResultSet.class)` |
| Oracle object param | `conn.createStruct("TYPE", values)` | `setObject(n, struct)` | – |
