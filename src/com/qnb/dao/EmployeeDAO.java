package com.qnb.dao;

import com.qnb.model.Employee;
import com.qnb.util.DBConnection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    // INSERT - add a new employee
    public void insertEmployee(Employee e) {

        String sql = "INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, e.getName());
            stmt.setString(2, e.getDepartment());
            stmt.setDouble(3, e.getSalary());
            stmt.setDouble(4, e.getBonus());

            int rows = stmt.executeUpdate();
            System.out.println(rows + " row(s) inserted.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // READ - get every employee in the table
    public List<Employee> getAllEmployees() {

        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT ID, NAME, DEPARTMENT, SALARY, BONUS FROM EMPLOYEE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(new Employee(
                        rs.getInt("ID"),
                        rs.getString("NAME"),
                        rs.getString("DEPARTMENT"),
                        rs.getDouble("SALARY"),
                        rs.getDouble("BONUS")));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return employees;
    }

    // UPDATE - change an existing employee's details by ID
    public void updateEmployee(Employee e) {

        String sql = "UPDATE EMPLOYEE SET NAME = ?, DEPARTMENT = ?, SALARY = ?, BONUS = ?, "
                + "UPDATE_DATE = CURRENT_TIMESTAMP WHERE ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, e.getName());
            stmt.setString(2, e.getDepartment());
            stmt.setDouble(3, e.getSalary());
            stmt.setDouble(4, e.getBonus());
            stmt.setInt(5, e.getId());

            int rows = stmt.executeUpdate();
            System.out.println(rows + " row(s) updated.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // DELETE - remove an employee by ID
    public void deleteEmployee(int id) {

        String sql = "DELETE FROM EMPLOYEE WHERE ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();
            System.out.println(rows + " row(s) deleted.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // PROCEDURE CALL - insert an employee by calling ADD_EMPLOYEE
    public void addEmployeeWithProcedure(Employee e) {

        String sql = "{call ADD_EMPLOYEE(?, ?, ?, ?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, e.getName());
            stmt.setString(2, e.getDepartment());
            stmt.setDouble(3, e.getSalary());
            stmt.setDouble(4, e.getBonus());

            stmt.execute();
            System.out.println("Procedure ADD_EMPLOYEE executed.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // PROCEDURE RETURNING A RESULT SET - get employees of one department
    public List<Employee> getEmployeesByDepartment(String department) {

        List<Employee> employees = new ArrayList<>();
        String sql = "{call GET_EMPLOYEES_BY_DEPT(?, ?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, department);
            stmt.registerOutParameter(2, Types.REF_CURSOR);

            stmt.execute();

            try (ResultSet rs = stmt.getObject(2, ResultSet.class)) {
                while (rs.next()) {
                    employees.add(new Employee(
                            rs.getInt("ID"),
                            rs.getString("NAME"),
                            rs.getString("DEPARTMENT"),
                            rs.getDouble("SALARY"),
                            rs.getDouble("BONUS")));
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return employees;
    }

    // PROCEDURE CALL WITH OBJECT - send the whole employee as one EMPLOYEE_OBJ
    public void addEmployeeWithObject(Employee e) {

        String sql = "{call ADD_EMPLOYEE_OBJ(?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            Object[] attributes = { e.getName(), e.getDepartment(), e.getSalary(), e.getBonus() };
            Struct empObj = conn.createStruct("EMPLOYEE_OBJ", attributes);

            stmt.setObject(1, empObj);

            stmt.execute();
            System.out.println("Procedure ADD_EMPLOYEE_OBJ executed.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
