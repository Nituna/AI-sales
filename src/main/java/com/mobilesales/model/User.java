package com.mobilesales.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model class representing an authorized system User.
 * Demonstrates Encapsulation with private fields and public accessors.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String name;
    private String username;
    private String email;
    private String password;
    private String role;
    private Timestamp createdAt;

    public User() {
        this.role = "ANALYST";
    }

    public User(int userId, String name, String username, String email, String password, String role) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "ANALYST";
    }

    public User(String name, String username, String email, String password) {
        this(0, name, username, email, password, "ANALYST");
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
