package com.blood.auth.config;

import com.blood.auth.model.AccountStatus;
import com.blood.auth.model.Role;
import com.blood.auth.model.User;
import com.blood.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@blood.com";
    private static final String ADMIN_PASSWORD = "Admin1!";
    private static final String ADMIN_NAME = "System Admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean activeAdminExists = userRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .anyMatch(u -> u.getRole() == Role.ADMIN);

        if (activeAdminExists) {
            log.info("Active admin already exists. Skipping seed.");
            return;
        }

        userRepository.findByEmail(ADMIN_EMAIL).ifPresentOrElse(
                existing -> {
                    existing.setStatus(AccountStatus.ACTIVE);
                    existing.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                    existing.setRole(Role.ADMIN);
                    userRepository.save(existing);
                    log.info("Admin account updated and activated: {}", ADMIN_EMAIL);
                },
                () -> {
                    User admin = User.builder()
                            .name(ADMIN_NAME)
                            .email(ADMIN_EMAIL)
                            .password(passwordEncoder.encode(ADMIN_PASSWORD))
                            .role(Role.ADMIN)
                            .status(AccountStatus.ACTIVE)
                            .build();
                    userRepository.save(admin);
                    log.info("Default admin account created: {}", ADMIN_EMAIL);
                }
        );
    }
}
