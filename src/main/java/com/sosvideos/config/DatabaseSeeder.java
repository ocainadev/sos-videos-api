package com.sosvideos.config;

import com.sosvideos.user.entity.User;
import com.sosvideos.user.enums.UserRole;
import com.sosvideos.user.repository.UserRepository;
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
public class DatabaseSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
    }

    private void seedAdmin() {
        String email = appProperties.getAdmin().getEmail();
        if (userRepository.existsByEmail(email)) {
            log.debug("Admin already exists, skipping seed.");
            return;
        }
        User admin = User.builder()
                .name("Administrador")
                .email(email)
                .passwordHash(passwordEncoder.encode(appProperties.getAdmin().getPassword()))
                .role(UserRole.ADMIN)
                .active(true)
                .firstLoginPendingPasswordChange(false)
                .build();
        userRepository.save(admin);
        log.info("Admin user created: {}", email);
    }
}
