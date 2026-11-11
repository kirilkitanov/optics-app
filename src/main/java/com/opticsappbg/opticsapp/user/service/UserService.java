package com.opticsappbg.opticsapp.user.service;

import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.model.UserRole;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService implements UserDetailsService, CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Създаване на Admin при празна база
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .createdOn(LocalDateTime.now())
                    .updatedOn(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
        }
    }

    // Търсене по ID
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Потребителят не е намерен с ID: " + id));
    }

    // Текущ потребител
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null ||
                !(authentication.getPrincipal() instanceof AuthenticationDetails)) {
            throw new IllegalArgumentException("Потребителят не е намерен.");
        }

        AuthenticationDetails details =
                (AuthenticationDetails) authentication.getPrincipal();

        User user = getById(details.getUserId());

        if (!user.isActive()) {
            throw new SecurityException("Профилът е неактивен.");
        }

        return user;
    }

    // Списък с потребители според правата
public List<User> getUsers(Authentication authentication) {
    User currentUser = getCurrentUser(authentication);

    if (currentUser.getRole() == UserRole.ADMIN) {
        return userRepository.findAllByOrderByCreatedOnDesc();
    }

    if (currentUser.getRole() == UserRole.OWNER) {
        return userRepository.findAllByOrderByCreatedOnDesc()
                .stream()
                .filter(user -> user.getRole() == UserRole.OWNER
                        || user.getRole() == UserRole.EMPLOYEE)
                .toList();
    }

    throw new SecurityException("Нямате право да управлявате потребители.");
}

    // Намиране на профил за редакция според правата
    public User getUserForManagement(Long userId,
                                     Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        if (currentUser.getRole() == UserRole.ADMIN) {
            return getById(userId);
        }

        if (currentUser.getRole() == UserRole.OWNER) {
            return userRepository
                    .findByIdAndIsActiveTrueAndRoleIn(
                            userId,
                            List.of(UserRole.OWNER, UserRole.EMPLOYEE)
                    )
                    .orElseThrow(() ->
                            new SecurityException("Нямате достъп до този профил."));
        }

        throw new SecurityException(
                "Нямате право да редактирате потребители.");
    }

    // Създаване на потребител
    @Transactional
    public User createUser(String username,
                           String fullName,
                           String password,
                           UserRole role,
                           Authentication authentication) {

        User currentUser = getCurrentUser(authentication);

        if (currentUser.getRole() != UserRole.ADMIN &&
                currentUser.getRole() != UserRole.OWNER) {
            throw new SecurityException("Нямате право да създавате потребители.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Въведете потребителско име.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Въведете парола.");
        }

        if (role == null) {
            throw new IllegalArgumentException("Изберете роля.");
        }

        // Owner не може да създава Admin
        if (currentUser.getRole() == UserRole.OWNER &&
                role == UserRole.ADMIN) {
            throw new SecurityException("Нямате право да създавате Admin.");
        }

        // Не допускаме втори Admin
        if (role == UserRole.ADMIN &&
                userRepository.findAll().stream()
                        .anyMatch(user -> user.getRole() == UserRole.ADMIN)) {
            throw new IllegalArgumentException("Вече съществува Admin профил.");
        }

        if (userRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException(
                    "Това потребителско име вече съществува.");
        }

        User user = User.builder()
                .username(username.trim())
                .fullName(fullName)
                .password(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    // Редактиране на профил
    @Transactional
    public User updateUser(Long userId,
                           String username,
                           String fullName,
                           UserRole role,
                           Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        User targetUser;

        if (currentUser.getRole() == UserRole.ADMIN) {
            targetUser = getById(userId);
        } else if (currentUser.getRole() == UserRole.OWNER) {
            targetUser = userRepository
                    .findByIdAndIsActiveTrueAndRoleIn(
                            userId,
                            List.of(UserRole.OWNER, UserRole.EMPLOYEE)
                    )
                    .orElseThrow(() ->
                            new SecurityException("Нямате достъп до този профил."));
        } else {
            throw new SecurityException("Нямате право да редактирате профили.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Въведете потребителско име.");
        }

        String newUsername = username.trim();

        if (!targetUser.getUsername().equals(newUsername) &&
                userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException(
                    "Това потребителско име вече съществува.");
        }

        if (role == null) {
            throw new IllegalArgumentException("Изберете роля.");
        }

        if (currentUser.getRole() == UserRole.OWNER &&
                role == UserRole.ADMIN) {
            throw new SecurityException("Нямате право да задавате Admin роля.");
        }

        if (targetUser.getRole() == UserRole.ADMIN &&
                role != UserRole.ADMIN) {
            throw new SecurityException("Не може да се променя ролята на Admin.");
        }

        if (role == UserRole.ADMIN &&
                targetUser.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Не може да се създава втори Admin.");
        }

        targetUser.setUsername(newUsername);
        targetUser.setFullName(fullName);
        targetUser.setRole(role);
        targetUser.setUpdatedOn(LocalDateTime.now());

        return userRepository.save(targetUser);
    }

    // Промяна на парола
    @Transactional
    public void changePassword(Long userId,
                               String newPassword,
                               Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        User targetUser;

        if (currentUser.getRole() == UserRole.ADMIN) {
            targetUser = getById(userId);
        } else if (currentUser.getRole() == UserRole.OWNER) {
            targetUser = userRepository
                    .findByIdAndIsActiveTrueAndRoleIn(
                            userId,
                            List.of(UserRole.OWNER, UserRole.EMPLOYEE)
                    )
                    .orElseThrow(() ->
                            new SecurityException("Нямате достъп до този профил."));
        } else {
            throw new SecurityException("Нямате право да променяте пароли.");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Въведете нова парола.");
        }

        targetUser.setPassword(passwordEncoder.encode(newPassword));
        targetUser.setUpdatedOn(LocalDateTime.now());

        userRepository.save(targetUser);
    }

    // Деактивиране на профил
@Transactional
public void deactivateUser(Long userId,
                           Authentication authentication) {

    User currentUser = getCurrentUser(authentication);
    User targetUser;

    if (currentUser.getRole() == UserRole.ADMIN) {
        targetUser = getById(userId);
    } else if (currentUser.getRole() == UserRole.OWNER) {
        targetUser = userRepository
                .findByIdAndIsActiveTrueAndRoleIn(
                        userId,
                        List.of(UserRole.OWNER, UserRole.EMPLOYEE)
                )
                .orElseThrow(() ->
                        new SecurityException("Нямате достъп до този профил."));
    } else {
        throw new SecurityException("Нямате право да деактивирате профили.");
    }

    if (targetUser.getRole() == UserRole.ADMIN) {
        throw new IllegalArgumentException(
                "Администраторският профил не може да бъде деактивиран."
        );
    }

    targetUser.setActive(false);
    targetUser.setUpdatedOn(LocalDateTime.now());

    userRepository.save(targetUser);
}

    // Възстановяване на профил
@Transactional
public void reactivateUser(Long userId,
                           Authentication authentication) {

    User currentUser = getCurrentUser(authentication);
    User targetUser = getById(userId);

    if (currentUser.getRole() == UserRole.OWNER) {

        if (targetUser.getRole() != UserRole.OWNER
                && targetUser.getRole() != UserRole.EMPLOYEE) {
            throw new SecurityException(
                    "Нямате право да възстановявате този профил.");
        }

    } else if (currentUser.getRole() != UserRole.ADMIN) {

        throw new SecurityException(
                "Нямате право да възстановявате профили.");
    }

    if (targetUser.isActive()) {
        throw new IllegalArgumentException("Профилът вече е активен.");
    }

    targetUser.setActive(true);
    targetUser.setUpdatedOn(LocalDateTime.now());

    userRepository.save(targetUser);
}

    // Spring Security – зареждане при вход
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Потребителят не е намерен: " + username));

        return new AuthenticationDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );
    }
}
