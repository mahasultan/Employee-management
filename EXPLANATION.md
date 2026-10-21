# Employee Management – Database Programming Explained

This project shows every database requirement with the simplest possible JDBC + Oracle code.

| Requirement | Where |
|---|---|
| Read | `EmployeeDAO.getAllEmployees()` |
| Insert / Update / Delete | `insertEmployee()`, `updateEmployee()`, `deleteEmployee()` |
| Procedure call | `calculateBonus()` → `CALCULATE_BONUS` |
| Return result set in procedure | `getEmployeesWithCursor()` → `GET_EMPLOYEES_CURSOR` |
| Procedure call with object | `insertEmployeeWithObject()` → `INSERT_EMPLOYEE_OBJ` |

**Order to run:** 1) run `sql/EMPLOYEE.sql` in SQL Developer (F5 "Run Script"), 2) fill in your credentials in `DBConnection.java`, 3) run `DBConnection` to test the connection, 4) run `MainApp`.

The flow is always: **MainApp → EmployeeDAO → DBConnection → Oracle**, with `Employee` carrying the data between them.

---

## 1. `sql/EMPLOYEE.sql` – the database side

### The table
```sql
CREATE TABLE EMPLOYEE (
    ID NUMBER GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,
```
- `ID` – Oracle generates the number itself (1, 2, 3…). That's why Java never sends an ID on insert. `PRIMARY KEY` = unique and not null.
```sql
    NAME VARCHAR2(100) NOT NULL,
    DEPARTMENT VARCHAR2(100),
    SALARY NUMBER(10,2),
    BONUS NUMBER(10,2),
```
- Text columns up to 100 chars; `NAME` is required. `NUMBER(10,2)` = up to 10 digits, 2 after the decimal point.
```sql
    INSERT_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```
- Audit columns. If you don't give a value, Oracle fills in "now" automatically, so Java never has to send them.

### The object type
```sql
CREATE OR REPLACE TYPE EMPLOYEE_OBJ AS OBJECT (
    ID          NUMBER,
    NAME        VARCHAR2(100),
    DEPARTMENT  VARCHAR2(100),
    SALARY      NUMBER,
    BONUS       NUMBER
);
/
```
- Creates a custom Oracle **object type** – basically the database version of our Java `Employee` class (one "package" holding 5 fields).
- `CREATE OR REPLACE` – create it, or overwrite it if it already exists (so you can re-run the script).
- `/` – tells SQL Developer "this block ends here, run it". Needed after every type/procedure.

### Procedure 1 – `CALCULATE_BONUS` (simple procedure call with IN and OUT)
```sql
CREATE OR REPLACE PROCEDURE CALCULATE_BONUS (
    p_id    IN  NUMBER,
    p_bonus OUT NUMBER
) AS
```
- `IN` parameter = a value coming **in** from Java (the employee ID).
- `OUT` parameter = a value the procedure sends **back** to Java (the bonus).
```sql
    v_salary NUMBER;
```
- A local variable, declared between `AS` and `BEGIN`, to hold the salary while we work.
```sql
BEGIN
    SELECT SALARY INTO v_salary FROM EMPLOYEE WHERE ID = p_id;
```
- Read the employee's salary and store it **into** `v_salary`. (If the ID doesn't exist, Oracle raises "no data found".)
```sql
    p_bonus := v_salary * 0.10;
```
- `:=` means "assign" in PL/SQL. The bonus = 10% of the salary. Setting `p_bonus` is what sends it back to Java.
```sql
    UPDATE EMPLOYEE SET BONUS = p_bonus WHERE ID = p_id;
    COMMIT;
END;
/
```
- Save the new bonus in the table and `COMMIT` (make it permanent).

### Procedure 2 – `GET_EMPLOYEES_CURSOR` (returns a result set)
```sql
CREATE OR REPLACE PROCEDURE GET_EMPLOYEES_CURSOR (
    p_cursor OUT SYS_REFCURSOR
) AS
BEGIN
    OPEN p_cursor FOR
        SELECT ID, NAME, DEPARTMENT, SALARY, BONUS FROM EMPLOYEE ORDER BY ID;
END;
/
```
- A procedure can't `return` rows directly. Instead it has an `OUT` parameter of type `SYS_REFCURSOR` – a **pointer to the rows of a query**.
- `OPEN p_cursor FOR SELECT …` runs the query (sorted by ID) and points the cursor at the results.
- Java receives this cursor and reads it as a normal `ResultSet`.

### Procedure 3 – `INSERT_EMPLOYEE_OBJ` (called with an object)
```sql
CREATE OR REPLACE PROCEDURE INSERT_EMPLOYEE_OBJ (
    p_emp IN EMPLOYEE_OBJ
) AS
BEGIN
    INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS)
    VALUES (p_emp.NAME, p_emp.DEPARTMENT, p_emp.SALARY, p_emp.BONUS);
    COMMIT;
END;
/
```
- Takes **one** parameter (the whole employee) instead of four.
- `p_emp.NAME` reads a field from the object, just like `e.getName()` in Java.
- `p_emp.ID` is **not** used – Oracle generates the ID itself.

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
private static Connection connection = null;
```
- One shared connection for the whole program. `static` = it belongs to the class, so every DAO method sees the same one. It starts as `null` (no connection yet).
```java
private DBConnection() {

}
```
- A `private` constructor means nobody can do `new DBConnection()` – you can only use it through `DBConnection.getConnection()`.
```java
public static Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }
    return connection;
}
```
- **Only the first call actually logs in** (when `connection` is still `null`). Every call after that just returns the same connection – much faster than logging in every time.
- `connection.isClosed()` – if the connection was closed (for example by the test `main` below), a new one is opened instead of returning a dead one.
- `DriverManager` finds the Oracle driver and logs in. `throws SQLException` = the caller must handle a failure.
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
- `try ( … )` is **try-with-resources**: the connection is closed automatically at the end, even on error. That's fine here because this is only a test – `getConnection()` will open a new one next time because of the `isClosed()` check.

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
- An all-fields constructor (includes the ID). We don't use it in this project – the DAO uses the empty constructor + setters instead – but it's handy to have. `this.name = name` means "the field `name` = the parameter `name`".
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
2. Get the shared connection with `DBConnection.getConnection()` and create the statement in `try ( … )`. **Only the statement goes inside `try ( … )`** – it is closed automatically, but the shared connection stays open for the next method.
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
try (PreparedStatement stmt = DBConnection.getConnection().prepareStatement(sql)) {
```
- Get the shared connection and prepare (pre-compile) the SQL on it.
- Only `stmt` is inside `try ( … )`, so only the statement is closed at the end. If we put the `Connection` inside `try ( … )` too, it would be **closed after the first method**, and the next method would fail with "Closed Connection".
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
try (PreparedStatement stmt = DBConnection.getConnection().prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
```
- `executeQuery()` is used for SELECT. It returns a `ResultSet` – like a table you read row by row.
```java
while (rs.next()) {
    Employee e = new Employee();
    e.setId(rs.getInt("ID"));
    e.setName(rs.getString("NAME"));
    e.setDepartment(rs.getString("DEPARTMENT"));
    e.setSalary(rs.getDouble("SALARY"));
    e.setBonus(rs.getDouble("BONUS"));
    employees.add(e);
}
```
- `rs.next()` moves to the next row; returns `false` when there are no more rows → loop ends.
- For each row: create an empty `Employee`, then copy each column into it with a setter. `rs.getXxx("COLUMN")` reads a column of the current row (`getInt` for numbers without decimals, `getString` for text, `getDouble` for decimals).
- `employees.add(e)` puts the finished employee into the list.
```java
return employees;
```
- Give the list back to the caller (empty if there was an error).

### UPDATE – `updateEmployee(Employee e)`
```java
String sql = "UPDATE EMPLOYEE SET NAME = ?, DEPARTMENT = ?, SALARY = ?, BONUS = ? WHERE ID = ?";
```
- Change all fields of the row whose `ID` matches. Without the `WHERE`, **every** row would be updated!
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

### PROCEDURE CALL – `calculateBonus(int id)`
```java
String sql = "{call CALCULATE_BONUS(?, ?)}";
```
- `{call NAME(?, …)}` is the standard JDBC syntax for calling a stored procedure. 1st `?` = IN id, 2nd `?` = OUT bonus.
```java
try (CallableStatement stmt = DBConnection.getConnection().prepareCall(sql)) {
```
- Same shared connection. `prepareCall` gives a `CallableStatement` – the statement type for procedures (it supports OUT parameters).
```java
stmt.setInt(1, id);
stmt.registerOutParameter(2, Types.NUMERIC);
```
- Set the IN value. For an OUT parameter we don't set a value – we **register** it, telling JDBC "parameter 2 will come back as a number".
```java
stmt.execute();
double bonus = stmt.getDouble(2);
System.out.println("New bonus for employee " + id + ": " + bonus);
```
- `execute()` runs the procedure. Then `getDouble(2)` reads the OUT value the procedure sent back, and we print it.

### PROCEDURE RETURNING A RESULT SET – `getEmployeesWithCursor()`
```java
String sql = "{call GET_EMPLOYEES_CURSOR(?)}";
```
- Only one `?` – the OUT cursor.
```java
stmt.registerOutParameter(1, Types.REF_CURSOR);
```
- Tell JDBC "parameter 1 will come back as a cursor".
```java
stmt.execute();
ResultSet rs = (ResultSet) stmt.getObject(1);
```
- After running, get parameter 1 back. `getObject` returns a general `Object`, so we **cast** it with `(ResultSet)` to tell Java "this is a ResultSet".
```java
while (rs.next()) {
    Employee e = new Employee();
    e.setId(rs.getInt("ID"));
    …
    employees.add(e);
}
return employees;
```
- From here it's exactly the same loop as the READ method. The `rs` is closed automatically when the statement `stmt` closes.

### PROCEDURE CALL WITH OBJECT – `insertEmployeeWithObject(Employee e)`
```java
String sql = "{call INSERT_EMPLOYEE_OBJ(?)}";
```
- Only **one** `?` – the whole employee object.
```java
Object[] attributes = { e.getId(), e.getName(), e.getDepartment(), e.getSalary(), e.getBonus() };
```
- The values of the object's fields, **in the same order as in `CREATE TYPE EMPLOYEE_OBJ`** (ID, NAME, DEPARTMENT, SALARY, BONUS). A new employee's ID is `0` – that's fine, the procedure ignores it.
```java
Struct empObj = DBConnection.getConnection().createStruct("EMPLOYEE_OBJ", attributes);
```
- Uses the same shared connection to build a real Oracle `EMPLOYEE_OBJ` in Java. The type name must be in **UPPERCASE**, as Oracle stores it.
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
Employee toUpdate = new Employee();
toUpdate.setId(1);
toUpdate.setName("Noel Thomas");
toUpdate.setDepartment("IT");
toUpdate.setSalary(15000.0);
toUpdate.setBonus(1000.0);
dao.updateEmployee(toUpdate);
```
- **3. UPDATE** – build an employee with ID 1 and the new values, then save it. If there is no row with ID 1, it prints `0 row(s) updated.` – check the IDs printed by the READ step and change the number if needed.
```java
dao.calculateBonus(1);
```
- **4. PROCEDURE CALL** – `CALCULATE_BONUS` sets employee 1's bonus to 10% of their salary and prints it.
```java
dao.insertEmployeeWithObject(new Employee("Omar Khan", "IT", 11000.0, 400.0));
```
- **5. PROCEDURE WITH OBJECT** – inserts through `INSERT_EMPLOYEE_OBJ`.
```java
for (Employee e : dao.getEmployeesWithCursor()) { System.out.println(e); }
```
- **6. PROCEDURE RETURNING RESULT SET** – prints every employee, read through `GET_EMPLOYEES_CURSOR`.
```java
dao.deleteEmployee(1);
```
- **7. DELETE** – removes the employee with ID 1.
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
| Stored procedure | `CallableStatement` (`{call …}`) | `execute()` | OUT value: `registerOutParameter(n, Types.NUMERIC)` then `getDouble(n)` |
| Procedure OUT cursor | `registerOutParameter(n, Types.REF_CURSOR)` | `execute()` | `(ResultSet) stmt.getObject(n)` |
| Oracle object param | `connection.createStruct("TYPE", values)` | `setObject(n, struct)` | – |
