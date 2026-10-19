-- Employee Table Creation
CREATE TABLE EMPLOYEE (
    ID NUMBER GENERATED ALWAYS AS IDENTITY
        PRIMARY KEY,
    NAME VARCHAR2(100) NOT NULL,
    DEPARTMENT VARCHAR2(100),
    SALARY NUMBER(10,2),
    BONUS NUMBER(10,2),

    INSERT_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- creates EmployeeObject thru java and sends it as a single parameter to procedure
CREATE OR REPLACE TYPE EMPLOYEE_OBJ AS OBJECT (
    ID          NUMBER,
    NAME        VARCHAR2(100),
    DEPARTMENT  VARCHAR2(100),
    SALARY      NUMBER,
    BONUS       NUMBER
);
/

-- Procedure takes an employee id and sends back a bonus (Bonus is 10% of salary)
CREATE OR REPLACE PROCEDURE CALCULATE_BONUS (
    p_id    IN  NUMBER,
    p_bonus OUT NUMBER
) AS
    v_salary NUMBER;
BEGIN
    SELECT SALARY INTO v_salary FROM EMPLOYEE WHERE ID = p_id;
    p_bonus := v_salary * 0.10;
    UPDATE EMPLOYEE SET BONUS = p_bonus WHERE ID = p_id;
    COMMIT;
END;
/

-- Return Resultset: will return rows through the OUT parameter
CREATE OR REPLACE PROCEDURE GET_EMPLOYEES_CURSOR (
    p_cursor OUT SYS_REFCURSOR
) AS
BEGIN
    OPEN p_cursor FOR
        SELECT ID, NAME, DEPARTMENT, SALARY, BONUS FROM EMPLOYEE ORDER BY ID;
END;
/

-- Call with object: receives EMPLOYEE_OBJ as one object
CREATE OR REPLACE PROCEDURE INSERT_EMPLOYEE_OBJ (
    p_emp IN EMPLOYEE_OBJ
) AS
BEGIN
    INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS)
    VALUES (p_emp.NAME, p_emp.DEPARTMENT, p_emp.SALARY, p_emp.BONUS);
    COMMIT;
END;
/
