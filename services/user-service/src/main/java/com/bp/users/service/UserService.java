package com.bp.users.service;

import com.bp.users.api.dto.CreateUserRequest;
import com.bp.users.api.dto.UserResponse;
import com.bp.users.entity.User;
import com.bp.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = User.builder()
                .name(request.fullName())
                .email(request.email())
                .build();
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElse(null); // Or throw an exception
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId().toString(), user.getEmail(), user.getName());
    }
}
