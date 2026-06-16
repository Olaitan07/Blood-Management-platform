package com.blood.auth.service;

import com.blood.auth.dto.AuthResponse;
import com.blood.auth.dto.LoginRequest;
import com.blood.auth.dto.RegisterRequest;
import com.blood.auth.dto.UserResponse;
import com.blood.auth.event.UserRegisteredEvent;
import com.blood.auth.exception.EmailAlreadyExistsException;
import com.blood.auth.exception.UnknownHospitalException;
import com.blood.auth.model.AccountStatus;
import com.blood.auth.model.User;
import com.blood.auth.repository.UserRepository;
import com.blood.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountLockoutService lockoutService;
    private final HospitalService hospitalService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already in use: " + request.email());
        }

        if (request.role().requiresHospital() && request.hospitalId() != null) {
            if (!hospitalService.hospitalExists(request.hospitalId())) {
                throw new UnknownHospitalException("Hospital not found with id: " + request.hospitalId());
            }
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .hospitalId(request.hospitalId())
                .status(AccountStatus.PENDING_APPROVAL)
                .build();

        User saved = userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getHospitalId()
        ));

        log.info("User registered: {} with role {}", saved.getEmail(), saved.getRole());
        return UserResponse.from(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        lockoutService.checkLocked(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            lockoutService.recordFailedAttempt(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Account is not active. Status: " + user.getStatus());
        }

        lockoutService.resetAttempts(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getStatus());
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return UserResponse.from(user);
    }
}
