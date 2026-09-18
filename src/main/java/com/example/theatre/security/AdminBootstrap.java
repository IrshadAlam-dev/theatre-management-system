package com.example.theatre.security;

import com.example.theatre.entity.Role;
import com.example.theatre.entity.User;
import com.example.theatre.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${ADMIN_EMAIL:}") String adminEmail,
                          @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.length() < 12
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("ADMIN_EMAIL and a 12-72 byte ADMIN_PASSWORD are required together");
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        User existing = users.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.ADMIN) {
                throw new IllegalStateException("ADMIN_EMAIL is already used by a non-admin account");
            }
            return;
        }

        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
