-- V2: Seed test data for manual testing

INSERT INTO test_apis (name, email, phone, age, gender, salary, birth_date, bio, status, score)
VALUES
    ('Nguyen Van A',     'nguyenvana@example.com',     '+84-901-234-567', 25, 'MALE',   15000000, '1999-03-15', 'Developer full-stack',  'ACTIVE',    8.5),
    ('Tran Thi B',       'tranthib@example.com',       '+84-902-345-678', 30, 'FEMALE', 20000000, '1994-07-22', 'Tech lead backend',     'ACTIVE',    9.0),
    ('Le Van C',         'levanc@example.com',          '+84-903-456-789', 28, 'MALE',   18000000, '1996-11-08', 'DevOps engineer',        'ACTIVE',    7.5),
    ('Pham Thi D',       'phamthid@example.com',        '+84-904-567-890', 22, 'FEMALE', 12000000, '2002-01-30', 'Junior frontend dev',    'ACTIVE',    6.5),
    ('Hoang Van E',      'hoangvane@example.com',       '+84-905-678-901', 35, 'MALE',   25000000, '1989-05-10', 'Engineering manager',    'ACTIVE',    9.5),
    ('Dao Thi F',        'daothif@example.com',         '+84-906-789-012', 27, 'FEMALE', 16000000, '1997-09-18', 'Full-stack developer',   'INACTIVE', 7.8),
    ('Bui Van G',        'buivang@example.com',         '+84-907-890-123', 40, 'MALE',   30000000, '1984-12-05', 'CTO',                   'ACTIVE',    9.8),
    ('Do Thi H',         'dothih@example.com',          '+84-908-901-234', 23, 'FEMALE', 13000000, '2001-06-25', 'Junior backend dev',    'SUSPENDED',5.0),
    ('Phan Van I',       'phanvani@example.com',        '+84-909-012-345', 31, 'MALE',   19000000, '1993-02-14', 'Senior data engineer',  'ACTIVE',    8.2),
    ('Ngo Thi K',        'ngothik@example.com',         '+84-910-123-456', 26, 'FEMALE', 17000000, '1998-08-03', 'Cloud architect',        'ACTIVE',    8.7);
