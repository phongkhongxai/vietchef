package com.spring2025.vietchefs.repositories;




import com.spring2025.vietchefs.models.entity.Role;
import com.spring2025.vietchefs.models.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@EnableJpaRepositories
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByPhone(String phone);
    Optional<User> findByEmail(String email);

    @Query(value = "SELECT * FROM users u WHERE u.is_delete = false and u.user_id = :userId", nativeQuery = true)
    User findExistUserById(Long userId);
    @Query("SELECT us FROM User us WHERE us.isDelete = false")
    Page<User> findAllNotDeleted(Pageable pageable);


    Optional<User> findByUid(String uid);
    Optional<User> findByResetPasswordToken(String token);
    Optional<User> findByResetPasswordTokenAndEmail(String token, String email);
    @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName AND u.isDelete = false")
    Page<User> findByRoleNameAndIsDeleteFalse(@Param("roleName") String roleName, Pageable pageable);

    // Statistics queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName AND u.isDelete = false")
    long countByRole(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isBanned = true AND u.isDelete = false")
    long countByIsBannedTrue();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isPremium = true AND u.isDelete = false")
    long countByIsPremiumTrue();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isDelete = false")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate AND u.isDelete = false")
    long countNewUsersFromDate(@Param("startDate") java.time.LocalDateTime startDate);

    // Date-based analytics queries for trend charts
    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) <= :date AND u.isDelete = false")
    Long countUsersByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = :date AND u.isDelete = false")
    Long countNewUsersByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = 'ROLE_CHEF' AND DATE(u.createdAt) = :date AND u.isDelete = false")
    Long countNewChefsByDate(@Param("date") java.time.LocalDate date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = 'ROLE_CUSTOMER' AND DATE(u.createdAt) = :date AND u.isDelete = false")
    Long countNewCustomersByDate(@Param("date") java.time.LocalDate date);

}
