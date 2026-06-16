package com.blood.auth.service;

import com.blood.auth.dto.UserResponse;
import com.blood.auth.event.AdminAuditEvent;
import com.blood.auth.model.AccountStatus;
import com.blood.auth.model.Role;
import com.blood.auth.model.User;
import com.blood.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> listPendingUsers() {
        return userRepository.findByStatus(AccountStatus.PENDING_APPROVAL).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse approveUser(Long targetUserId) {
        User target = findUserOrThrow(targetUserId);
        if (target.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User is not in PENDING_APPROVAL status");
        }
        target.setStatus(AccountStatus.ACTIVE);
        userRepository.save(target);

        publishAudit("APPROVE_USER", targetUserId, "User approved: " + target.getEmail());
        return UserResponse.from(target);
    }

    @Transactional
    public UserResponse deactivateUser(Long targetUserId) {
        User target = findUserOrThrow(targetUserId);

        if (target.getRole() == Role.ADMIN && target.getStatus() == AccountStatus.ACTIVE) {
            long activeAdminCount = userRepository.findByStatus(AccountStatus.ACTIVE).stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot deactivate the last active admin");
            }
        }

        target.setStatus(AccountStatus.SUSPENDED);
        userRepository.save(target);

        publishAudit("DEACTIVATE_USER", targetUserId, "User deactivated: " + target.getEmail());
        return UserResponse.from(target);
    }

    @Transactional
    public UserResponse updateUserRole(Long targetUserId, Role newRole) {
        User target = findUserOrThrow(targetUserId);

        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long activeAdminCount = userRepository.findByStatus(AccountStatus.ACTIVE).stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot change role of the last active admin");
            }
        }

        target.setRole(newRole);
        userRepository.save(target);

        publishAudit("UPDATE_ROLE", targetUserId,
                "Role updated to " + newRole + " for user: " + target.getEmail());
        return UserResponse.from(target);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId));
    }

    private void publishAudit(String action, Long targetUserId, String detail) {
        Long adminId = getCurrentAdminId();
        eventPublisher.publishEvent(new AdminAuditEvent(
                action,
                adminId,
                targetUserId,
                detail,
                LocalDateTime.now().toString()
        ));
        log.info("Admin audit: {} by admin {} on user {}", action, adminId, targetUserId);
    }

    private Long getCurrentAdminId() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
