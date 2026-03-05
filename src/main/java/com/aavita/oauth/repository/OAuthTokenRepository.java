package com.aavita.oauth.repository;

import com.aavita.oauth.model.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

    Optional<OAuthToken> findByTokenValueAndTokenType(String tokenValue, String tokenType);

    void deleteByTokenValueAndTokenType(String tokenValue, String tokenType);

    void deleteByUserId(String userId);
}