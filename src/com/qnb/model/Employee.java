package com.qnb.model;

public class Employee {

    private int id;
    private String name;
    private String department;
    private double salary;
    private double bonus;

    // Empty constructor
    public Employee() {
    }

    // Constructor for INSERTING a new employee
    public Employee(String name, String department, double salary, double bonus) {
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.bonus = bonus;
    }

    // Constructor for READING an employee back from the database
    public Employee(int id, String name, String department, double salary, double bonus) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.bonus = bonus;
    }

    // getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public double getBonus() {
        return bonus;
    }

    public void setBonus(double bonus) {
        this.bonus = bonus;
    }

    @Override
    public String toString() {
        return "ID: " + id
                + " | Name: " + name
                + " | Department: " + department
                + " | Salary: " + salary
                + " | Bonus: " + bonus;
    }
}
