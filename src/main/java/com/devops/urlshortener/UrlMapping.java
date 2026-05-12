package com.devops.urlshortener;

import jakarta.persistence.*;

/**
 * JPA Entity - maps to the URL_MAPPING table in H2.
 * Stores short_code → original_url pairs.
 */
@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    // ── Constructors ──────────────────────────────────────────────
    public UrlMapping() {}

    public UrlMapping(String shortCode, String originalUrl) {
        this.shortCode   = shortCode;
        this.originalUrl = originalUrl;
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long   getId()          { return id; }
    public String getShortCode()   { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
}
