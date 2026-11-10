package com.opticsappbg.opticsapp.user.repository;

import com.opticsappbg.opticsapp.user.model.User;
import com.opticsappbg.opticsapp.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // Всички активни потребители с определени роли
    List<User> findAllByIsActiveTrueAndRoleInOrderByCreatedOnDesc(
            Collection<UserRole> roles
    );

    // Всички потребители, включително неактивните – за Admin
    List<User> findAllByOrderByCreatedOnDesc();

    // Търсене на активен потребител с разрешена роля
    Optional<User> findByIdAndIsActiveTrueAndRoleIn(
            Long id,
            Collection<UserRole> roles
    );
}
