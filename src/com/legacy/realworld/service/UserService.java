package com.legacy.realworld.service;

import com.legacy.realworld.model.User;
import com.legacy.realworld.repository.UserRepository;
import com.legacy.realworld.util.PasswordUtil;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Service layer for user business logic.
 * Handles registration, login, and user retrieval.
 */
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Register a new user. Returns the created User, or null if email/username conflict.
     * Sets an error message via the returned status.
     */
    public RegistrationResult register(String email, String username, String password) throws SQLException {
        if (userRepo.existsByEmail(email)) {
            return new RegistrationResult(null, "Email already in use");
        }
        if (userRepo.existsByUsername(username)) {
            return new RegistrationResult(null, "Username already in use");
        }

        String id = UUID.randomUUID().toString();
        String hashedPassword = PasswordUtil.hash(password);
        userRepo.save(id, email, username, hashedPassword);

        User user = new User(id, email, username, "", "", "");
        return new RegistrationResult(user, null);
    }

    public User login(String email, String password) throws SQLException {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            return null;
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    public User getCurrentUser(String userId) throws SQLException {
        return userRepo.findById(userId);
    }

    /**
     * Result wrapper for registration that can carry an error message.
     */
    public static class RegistrationResult {
        private final User user;
        private final String error;

        public RegistrationResult(User user, String error) {
            this.user = user;
            this.error = error;
        }

        public User getUser() {
            return user;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return user != null;
        }
    }
}
