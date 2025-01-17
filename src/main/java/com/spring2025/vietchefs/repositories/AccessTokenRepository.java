package com.spring2025.vietchefs.repositories;




import com.spring2025.vietchefs.models.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {
    @Query("""
                        select t from AccessToken t
                        inner join User u on u.id = t.user.id
                        where u.id = :userId and (t.expired = false or t.revoked = false)
            """)
    List<AccessToken> findAllValidTokensByUser(Long userId);

    AccessToken findByToken(String accessToken);
}
