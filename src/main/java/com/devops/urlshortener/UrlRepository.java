package com.devops.urlshortener;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA Repository for UrlMapping.
 * No implementation needed - Spring generates it automatically.
 */
@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Find a URL mapping by its short code.
     * Used by the redirect endpoint GET /{code}
     */
    Optional<UrlMapping> findByShortCode(String shortCode);
}
