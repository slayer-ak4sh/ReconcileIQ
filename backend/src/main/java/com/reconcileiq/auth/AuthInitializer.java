package com.reconcileiq.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoExecPassword;
    private final String demoFounderPassword;

    public AuthInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.exec-password}") String demoExecPassword,
            @Value("${app.demo.founder-password}") String demoFounderPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoExecPassword = demoExecPassword;
        this.demoFounderPassword = demoFounderPassword;
    }

    @Override
    public void run(String... args) {
        upsertDemoUser("exec@demo.com", demoExecPassword, "FINANCE_EXECUTIVE", "Finance Executive Demo");
        upsertDemoUser("founder@demo.com", demoFounderPassword, "COFOUNDER", "Finance Co-founder Demo");
    }

    private void upsertDemoUser(String email, String rawPassword, String role, String fullName) {
        String encoded = passwordEncoder.encode(rawPassword);
        userRepository.findByEmail(email).ifPresentOrElse(
            u -> {
                u.setPasswordHash(encoded);
                u.setRole(role);
                u.setFullName(fullName);
                userRepository.save(u);
            },
            () -> {
                User user = new User(email, encoded, role, fullName);
                userRepository.save(user);
            }
        );
    }

}