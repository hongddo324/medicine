# Initial Data - User Account Creation

This document contains SQL statements for creating initial user accounts in the PostgreSQL database.

## Prerequisites

- PostgreSQL database must be running
- Database schema must be created (run the application once with `spring.jpa.hibernate.ddl-auto=update` or use `DATABASE_SCHEMA.md`)
- Connect to your PostgreSQL database using psql or any PostgreSQL client

## Connect to Database

```bash
psql -h localhost -p 5432 -U postgres -d medicine
```

## Create Initial Users

### Example 1: Admin User

```sql
INSERT INTO users (username, password, display_name, role, profile_image, created_at)
VALUES (
    'admin',
    'admin123',  -- Change this to a secure password
    '관리자',
    'ADMIN',
    '/images/profile/default.png',
    CURRENT_TIMESTAMP
);
```

### Example 2: Regular User (User1)

```sql
INSERT INTO users (username, password, display_name, role, profile_image, created_at)
VALUES (
    'user1',
    'password123',  -- Change this to a secure password
    '홍길동',
    'USER',
    '/images/profile/default.png',
    CURRENT_TIMESTAMP
);
```

### Example 3: Regular User (User2)

```sql
INSERT INTO users (username, password, display_name, role, profile_image, created_at)
VALUES (
    'user2',
    'password456',  -- Change this to a secure password
    '김철수',
    'USER',
    '/images/profile/default.png',
    CURRENT_TIMESTAMP
);
```

## Bulk Insert Multiple Users

If you want to create multiple users at once:

```sql
INSERT INTO users (username, password, display_name, role, profile_image, created_at)
VALUES
    ('admin', 'admin123', '관리자', 'ADMIN', '/images/profile/default.png', CURRENT_TIMESTAMP),
    ('user1', 'password123', '홍길동', 'USER', '/images/profile/default.png', CURRENT_TIMESTAMP),
    ('user2', 'password456', '김철수', 'USER', '/images/profile/default.png', CURRENT_TIMESTAMP),
    ('user3', 'password789', '이영희', 'USER', '/images/profile/default.png', CURRENT_TIMESTAMP);
```

## Verify Users Created

```sql
SELECT id, username, display_name, role, created_at FROM users ORDER BY created_at;
```

## Update User Information

### Update Display Name

```sql
UPDATE users SET display_name = '새로운이름' WHERE username = 'user1';
```

### Update Password

```sql
UPDATE users SET password = 'new_password' WHERE username = 'user1';
```

### Update Profile Image

```sql
UPDATE users SET profile_image = '/images/profile/user1.jpg' WHERE username = 'user1';
```

## Delete User

```sql
-- Warning: This will also delete all related data (comments, meal checks, medicine records)
DELETE FROM users WHERE username = 'user1';
```

## Important Notes

### Security Considerations

1. **Password Security**: The examples above use plain text passwords for simplicity. In a production environment, you should:
   - Use a password hashing library (bcrypt, scrypt, etc.)
   - Never store plain text passwords
   - Consider implementing Spring Security's `PasswordEncoder`

2. **Change Default Passwords**: Always change the default passwords after first login

3. **Admin Account**: Protect the admin account with a strong password

### User Roles

The application supports two roles:
- `ADMIN`: Full access to all features including user management
- `USER`: Standard user with access to medicine tracking, meal management, and comments

### Profile Images

- Default profile image path: `/images/profile/default.png`
- Custom profile images should be stored in: `/files/profile/` directory
- Supported formats: JPG, PNG, GIF
- Make sure to create the profile image directory:

```bash
mkdir -p /home/user/medicine/uploads/profile
```

### Serial ID

The `id` column is auto-generated using PostgreSQL's `BIGSERIAL` type (equivalent to `IDENTITY`). You should **NOT** specify the `id` value in INSERT statements.

## Example: Production-Ready User Creation

For production environments, you might want to use hashed passwords:

```sql
-- Using pgcrypto extension for password hashing
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password, display_name, role, profile_image, created_at)
VALUES (
    'admin',
    crypt('your_secure_password', gen_salt('bf')),  -- Blowfish hashing
    '관리자',
    'ADMIN',
    '/images/profile/default.png',
    CURRENT_TIMESTAMP
);
```

**Note**: If using password hashing in the database, you'll need to update your authentication logic in `UserService.java` to use the same hashing algorithm.

## Testing Login

After creating users, you can test login by:

1. Start the application
2. Navigate to: `https://www.hongddo.top/login`
3. Use the credentials you created:
   - Username: `admin`
   - Password: `admin123`

## Troubleshooting

### Error: "duplicate key value violates unique constraint"

This means a user with the same username already exists. Use a different username or delete the existing user first.

```sql
-- Check if user exists
SELECT * FROM users WHERE username = 'admin';

-- Delete existing user if needed
DELETE FROM users WHERE username = 'admin';
```

### Error: "relation 'users' does not exist"

The database schema hasn't been created yet. Either:
1. Run the application once to auto-create tables (if `ddl-auto=update`)
2. Or manually run the DDL from `DATABASE_SCHEMA.md`

### Error: "column does not exist"

Make sure your database schema is up to date. Run the application with `spring.jpa.hibernate.ddl-auto=update` to sync the schema.
