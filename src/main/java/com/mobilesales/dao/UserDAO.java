package com.mobilesales.dao;

import com.mobilesales.exception.DataValidationException;
import com.mobilesales.exception.InvalidLoginException;
import com.mobilesales.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object (DAO) for User entity.
 * Demonstrates JDBC usage, PreparedStatement, SQL queries, and Exception Handling.
 */
public class UserDAO {

    private final DatabaseConnection dbConnection;

    public UserDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    public boolean registerUser(User user) throws DataValidationException, SQLException {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty() ||
            user.getPassword() == null || user.getPassword().trim().isEmpty() ||
            user.getEmail() == null || user.getEmail().trim().isEmpty() ||
            user.getName() == null || user.getName().trim().isEmpty()) {
            throw new DataValidationException("All registration fields are required.");
        }

        if (existsByUsername(user.getUsername())) {
            throw new DataValidationException("Username '" + user.getUsername() + "' is already registered.");
        }

        if (existsByEmail(user.getEmail())) {
            throw new DataValidationException("Email address '" + user.getEmail() + "' is already in use.");
        }

        String sql = "INSERT INTO users (name, username, email, password, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole() != null ? user.getRole() : "ANALYST");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setUserId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public User authenticate(String username, String password) throws InvalidLoginException, SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new InvalidLoginException("Username and password cannot be empty.");
        }

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, password.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setName(rs.getString("name"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    return user;
                }
            }
        }
        throw new InvalidLoginException("Invalid username or password. Please verify credentials and retry.");
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
