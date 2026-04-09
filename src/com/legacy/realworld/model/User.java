package com.legacy.realworld.model;

import java.io.Serializable;

/**
 * User model class.
 * 
 * WARNING: Password is stored as plain text! This is a known security issue.
 * TODO: Implement password hashing with BCrypt or similar.
 * TODO: The modern version uses Spring Security's PasswordEncoder.
 * 
 * @author legacy-team
 * @since 1.0
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String email;
    private String username;
    private String password;  // TODO: SECURITY RISK - plain text password storage!
    private String bio;
    private String image;

    // Dead code: role-based access was planned but never implemented
    // private String role = "user";
    // private boolean isActive = true;
    // private Date lastLoginDate;

    public User() {
        // Default constructor
    }

    public User(String id, String email, String username, String password, 
                String bio, String image) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;  // Storing plain text - this is intentionally wrong
        this.bio = bio;
        this.image = image;
    }

    // --- Getters and Setters (manual, no Lombok) ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the password in PLAIN TEXT.
     * TODO: This should never return the raw password. 
     * The modern version never exposes the password hash through a getter.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password as PLAIN TEXT.
     * TODO: Should hash the password before storing.
     * Modern equivalent uses PasswordEncoder.encode().
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // Dead code: was going to add role checks but never finished
    /*
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public boolean isAdmin() {
        return "admin".equals(role);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    */

    @Override
    public String toString() {
        // TODO: This logs the password! Should be removed or redacted.
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
