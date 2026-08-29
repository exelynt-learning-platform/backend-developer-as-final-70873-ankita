package com.exelynt.booking.config;

import com.exelynt.booking.model.Role;
import com.exelynt.booking.model.User;
import com.exelynt.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Defaults are for local/dev only; set ADMIN_SEED_PASSWORD / USER_SEED_PASSWORD
    // env vars in any shared or deployed environment so real credentials never
    // live in source control.
    @Value("${ADMIN_SEED_EMAIL:admin@test.com}")
    private String adminEmail;

    @Value("${ADMIN_SEED_PASSWORD:admin123}")
    private String adminPassword;

    @Value("${USER_SEED_EMAIL:user@test.com}")
    private String userEmail;

    @Value("${USER_SEED_PASSWORD:user123}")
    private String userPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            log.info("Seeded admin user: {}", adminEmail);
        }

        if (userRepository.findByEmail(userEmail).isEmpty()) {
            User user = new User();
            user.setEmail(userEmail);
            user.setPassword(passwordEncoder.encode(userPassword));
            user.setRole(Role.ROLE_USER);
            userRepository.save(user);
            log.info("Seeded standard user: {}", userEmail);
        }
    }
}
