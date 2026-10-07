package com.qnb.main;

import com.qnb.dao.EmployeeDAO;
import com.qnb.model.Employee;

import java.util.List;

public class MainApp {

    public static void main(String[] args) {

        EmployeeDAO dao = new EmployeeDAO();

        // 1. INSERT a new employee
        dao.insertEmployee(new Employee("Noel Thomas", "IT", 12000.0, 500.0));

        // 2. READ - print everyone currently in the table
        System.out.println("Employee List");
        List<Employee> employees = dao.getAllEmployees();
        for (Employee e : employees) {
            System.out.println(e);
        }

        // 3. UPDATE - give the first employee in the list a raise
        Employee toUpdate = employees.get(0);
        toUpdate.setSalary(15000.0);
        toUpdate.setBonus(1000.0);
        dao.updateEmployee(toUpdate);

        // 4. PROCEDURE CALL - insert using the ADD_EMPLOYEE procedure
        dao.addEmployeeWithProcedure(new Employee("Sara Ali", "HR", 9000.0, 300.0));

        // 5. PROCEDURE CALL WITH OBJECT - insert using the ADD_EMPLOYEE_OBJ procedure
        dao.addEmployeeWithObject(new Employee("Omar Khan", "IT", 11000.0, 400.0));

        // 6. PROCEDURE RETURNING A RESULT SET - read only the IT department
        System.out.println("IT Department (from procedure)");
        for (Employee e : dao.getEmployeesByDepartment("IT")) {
            System.out.println(e);
        }

        // 7. DELETE - remove the employee we updated
        dao.deleteEmployee(toUpdate.getId());

        // Read again to see the final table
        System.out.println("Final Employee List");
        for (Employee e : dao.getAllEmployees()) {
            System.out.println(e);
        }
    }
}
