package com.bp.users.service;

import com.bp.users.api.dto.CreateUserRequest;
import com.bp.users.api.dto.UserResponse;
import com.bp.users.entity.User;
import com.bp.users.exception.EntityNotFoundException;
import com.bp.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email={}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("User creation failed. Email already exists: {}", request.email());
            throw new IllegalArgumentException(
                    "User with email already exists: " + request.email()
            );
        }

        User user = User.builder()
                .name(request.fullName())
                .email(request.email())
                .build();

        User savedUser = userRepository.save(user);

        log.info("User created successfully. id={}, email={}",
                savedUser.getId(), savedUser.getEmail());

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user by id={}", id);

        return userRepository.findById(id)
                .map(user -> {
                    log.debug("User found. id={}, email={}", user.getId(), user.getEmail());
                    return toResponse(user);
                })
                .orElseThrow(() -> {
                    log.warn("User not found. id={}", id);
                    return new EntityNotFoundException("User not found: " + id);
                });
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");

        List<UserResponse> users = userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();

        log.info("Fetched {} users", users.size());
        return users;
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id={}", id);

        if (!userRepository.existsById(id)) {
            log.warn("User deletion failed. User not found. id={}", id);
            throw new EntityNotFoundException("User not found: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully. id={}", id);
    }

    @Transactional
    public void deleteAllUsers() {
        log.warn("Deleting ALL users (dangerous operation)");
        userRepository.deleteAll();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}