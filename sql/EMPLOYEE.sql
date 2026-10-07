-- 1. Employee table
CREATE TABLE EMPLOYEE (
    ID          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    NAME        VARCHAR2(100) NOT NULL,
    DEPARTMENT  VARCHAR2(100),
    SALARY      NUMBER(10,2),
    BONUS       NUMBER(10,2),
    INSERT_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Simple procedure call: insert an employee using normal IN parameters
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

-- 3. Procedure that returns a result set (SYS_REFCURSOR OUT parameter)
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

-- 4. Procedure call with an object
-- Java creates an EMPLOYEE_OBJ and sends it as a single parameter to the procedure
CREATE OR REPLACE TYPE EMPLOYEE_OBJ AS OBJECT (
    NAME       VARCHAR2(100),
    DEPARTMENT VARCHAR2(100),
    SALARY     NUMBER(10,2),
    BONUS      NUMBER(10,2)
);
/

CREATE OR REPLACE PROCEDURE ADD_EMPLOYEE_OBJ (
    p_emp IN EMPLOYEE_OBJ
) AS
BEGIN
    INSERT INTO EMPLOYEE (NAME, DEPARTMENT, SALARY, BONUS)
    VALUES (p_emp.NAME, p_emp.DEPARTMENT, p_emp.SALARY, p_emp.BONUS);
END;
/
