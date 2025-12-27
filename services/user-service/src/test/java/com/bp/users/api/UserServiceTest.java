package com.bp.users.api;

import com.bp.users.api.dto.CreateUserRequest;
import com.bp.users.api.dto.UserResponse;
import com.bp.users.entity.User;
import com.bp.users.repository.UserRepository;
import com.bp.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUser() {
        CreateUserRequest request =
                new CreateUserRequest("john.doe@example.com", "John Doe");

        User savedUser = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .name("John Doe")
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.fullName()).isEqualTo("John Doe");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldGetUserById() {
        // arrange
        User user = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .name("John Doe")
                .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // act
        UserResponse response = userService.getUserById(1L);

        // assert
        assertThat(response.id()).isEqualTo("1");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.fullName()).isEqualTo("John Doe");

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldGetAllUsers() {
        // arrange
        List<User> users = List.of(
                User.builder().id(1L).email("a@test.com").name("User A").build(),
                User.builder().id(2L).email("b@test.com").name("User B").build()
        );

        when(userRepository.findAll()).thenReturn(users);

        // act
        List<UserResponse> result = userService.getAllUsers();

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).email()).isEqualTo("a@test.com");
        assertThat(result.get(1).fullName()).isEqualTo("User B");

        verify(userRepository).findAll();
    }

    @Test
    void shouldDeleteUser() {
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void shouldDeleteAllUsers() {
        // act
        userService.deleteAllUsers();

        // assert
        verify(userRepository).deleteAll();
    }
}
