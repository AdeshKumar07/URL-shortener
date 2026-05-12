package com.devops.urlshortener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST API consumed by the static Tailwind UI (index.html).
 *
 * Endpoints
 *   POST /shorten      – shorten a URL, returns {code, short, original}
 *   GET  /{code}       – 302 redirect to the original URL
 *   GET  /health       – health-check used by Docker / CI
 *   GET  /api          – endpoint summary
 */
@RestController
public class UrlController {

    @Autowired
    private UrlRepository repo;

    // ── POST /shorten ─────────────────────────────────────────────

    /**
     * Accepts : { "url": "https://very.long/path" }
     * Returns : { "code": "a3f9c1", "short": "http://host/a3f9c1", "original": "..." }
     */
    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shorten(@RequestBody Map<String, String> body) {

        String longUrl = body.get("url");

        // FIX: guard against both null (missing key) and blank string
        if (longUrl == null || longUrl.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing or empty 'url' field in request body"));
        }

        URI originalUri;
        try {
            originalUri = URI.create(longUrl.trim());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please enter a valid URL"));
        }

        // Require an explicit http / https scheme
        String scheme = originalUri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "URL must start with http:// or https://"));
        }

        // FIX: also require a host — URI.create("https://") has no host
        if (originalUri.getHost() == null || originalUri.getHost().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "URL must contain a valid hostname"));
        }

        // Generate a unique 6-char lowercase-hex short code
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6); // always [a-f0-9]{6}
        } while (repo.findByShortCode(code).isPresent());

        repo.save(new UrlMapping(code, originalUri.toString()));

        // Build the full short URL from the current request context so it
        // works correctly behind proxies, in Docker, or on any hostname/port.
        String shortUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(code)
                .toUriString();

        Map<String, String> response = new HashMap<>();
        response.put("code",     code);
        response.put("short",    shortUrl);
        response.put("original", originalUri.toString());
        return ResponseEntity.ok(response);
    }

    // ── GET /{code} ───────────────────────────────────────────────

    /**
     * Regex [a-f0-9]{6} ensures only valid 6-char hex codes are routed here.
     * Returns 302 → original URL, or 404 if the code is unknown.
     */
    @GetMapping("/{code:[a-f0-9]{6}}")
    public ResponseEntity<?> redirect(@PathVariable String code) {
        return repo.findByShortCode(code)
                .map(mapping -> ResponseEntity
                        .status(302)
                        .location(URI.create(mapping.getOriginalUrl()))
                        .<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET /health ───────────────────────────────────────────────

    /**
     * Used by Docker HEALTHCHECK, docker-compose, and CI pipelines.
     * Returns HTTP 200 + { "status": "UP", ... }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status",  "UP");
        status.put("service", "URL Shortener");
        status.put("version", "1.0.0");
        return ResponseEntity.ok(status);
    }

    // ── GET /api ──────────────────────────────────────────────────

    /** Endpoint summary for scripts and dashboards. */
    @GetMapping("/api")
    public ResponseEntity<Map<String, String>> api() {
        Map<String, String> info = new HashMap<>();
        info.put("app",      "URL Shortener - INT332 DevOps Project");
        info.put("shorten",  "POST /shorten  body: {\"url\":\"<long_url>\"}");
        info.put("redirect", "GET  /{code}");
        info.put("health",   "GET  /health");
        return ResponseEntity.ok(info);
    }
}
