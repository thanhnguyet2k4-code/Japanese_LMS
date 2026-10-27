
package com.japaneselms.repository;

import com.japaneselms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByAccountStatus(String accountStatus);

    List<User> findByRole(String role);

    // Tim kiem theo ho ten hoac email, co phan trang
    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName,
            String email,
            Pageable pageable
    );

    // Loc theo vai tro va trang thai hoat dong, co phan trang
    Page<User> findByRoleContainingIgnoreCaseAndActive(
            String role,
            boolean active,
            Pageable pageable
    );

    long countByRoleIgnoreCaseAndActiveTrue(String role);

}
