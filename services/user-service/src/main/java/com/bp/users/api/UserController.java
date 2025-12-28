package com.bp.users.api;

import com.bp.users.api.dto.CreateUserRequest;
import com.bp.users.api.dto.UserResponse;
import com.bp.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The type User controller.
 */
@Tag(name = "User Management", description = "Operations related to user accounts")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Create user response.
     *
     * @param request the request
     * @return the user response
     */
    @Operation(summary = "Create a new user")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request);
    }

    /**
     * Get user response.
     *
     * @param id the id
     * @return the user response
     */
    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * Gets all.
     *
     * @return the all
     */
    @Operation(summary = "Get all users")
    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAllUsers();
    }

    /**
     * Delete.
     *
     * @param id the id
     */
    @Operation(summary = "Delete user by ID")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    /**
     * Delete all.
     */
    @Operation(summary = "Delete all users (for testing/admin purposes)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        userService.deleteAllUsers();
    }
}