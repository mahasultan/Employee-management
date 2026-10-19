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
                Employee e = new Employee();
                e.setId(rs.getInt("ID"));
                e.setName(rs.getString("NAME"));
                e.setDepartment(rs.getString("DEPARTMENT"));
                e.setSalary(rs.getDouble("SALARY"));
                e.setBonus(rs.getDouble("BONUS"));
                employees.add(e);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return employees;
    }

    // UPDATE - change an existing employee's details by ID
    public void updateEmployee(Employee e) {

        String sql = "UPDATE EMPLOYEE SET NAME = ?, DEPARTMENT = ?, SALARY = ?, BONUS = ? WHERE ID = ?";

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

    // PROCEDURE CALL - send an ID in, get the bonus back out (CALCULATE_BONUS)
    public void calculateBonus(int id) {

        String sql = "{call CALCULATE_BONUS(?, ?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, id);
            stmt.registerOutParameter(2, Types.NUMERIC);

            stmt.execute();

            double bonus = stmt.getDouble(2);
            System.out.println("New bonus for employee " + id + ": " + bonus);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // PROCEDURE RETURNING A RESULT SET - get every employee (GET_EMPLOYEES_CURSOR)
    public List<Employee> getEmployeesWithCursor() {

        List<Employee> employees = new ArrayList<>();
        String sql = "{call GET_EMPLOYEES_CURSOR(?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.registerOutParameter(1, Types.REF_CURSOR);

            stmt.execute();

            ResultSet rs = (ResultSet) stmt.getObject(1);

            while (rs.next()) {
                Employee e = new Employee();
                e.setId(rs.getInt("ID"));
                e.setName(rs.getString("NAME"));
                e.setDepartment(rs.getString("DEPARTMENT"));
                e.setSalary(rs.getDouble("SALARY"));
                e.setBonus(rs.getDouble("BONUS"));
                employees.add(e);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return employees;
    }

    // PROCEDURE CALL WITH OBJECT - send the whole employee as one EMPLOYEE_OBJ (INSERT_EMPLOYEE_OBJ)
    public void insertEmployeeWithObject(Employee e) {

        String sql = "{call INSERT_EMPLOYEE_OBJ(?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            Object[] attributes = { e.getId(), e.getName(), e.getDepartment(), e.getSalary(), e.getBonus() };
            Struct empObj = conn.createStruct("EMPLOYEE_OBJ", attributes);

            stmt.setObject(1, empObj);

            stmt.execute();
            System.out.println("Procedure INSERT_EMPLOYEE_OBJ executed.");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
