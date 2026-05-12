package com.devops.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test suite for UrlController and the static UI.
 *
 * Every test exercises the real Spring context (H2 in-memory DB) so that
 * controller → repository → DB → response is tested end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Static UI ─────────────────────────────────────────────────

    /**
     * FIX (original): forwardedUrl("index.html") tests a Servlet forward.
     * Spring Boot's static resource handler serves files directly without a
     * forward, so that assertion always failed.
     * Now: assert 200 + expected page content directly on /index.html.
     */
    @Test
    void indexHtmlIsServedWithCorrectContent() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Shorten a URL")));
    }

    // ── /health ───────────────────────────────────────────────────

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("URL Shortener"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    // ── /api ──────────────────────────────────────────────────────

    @Test
    void apiInfoReturnsAllExpectedFields() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shorten").exists())
                .andExpect(jsonPath("$.redirect").exists())
                .andExpect(jsonPath("$.health").value("GET  /health"));
    }

    // ── POST /shorten — happy path ────────────────────────────────

    @Test
    void shortenValidHttpsUrlReturnsCodeAndShortAndOriginal() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://www.google.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.short").exists())
                .andExpect(jsonPath("$.original").value("https://www.google.com"));
    }

    @Test
    void shortenValidHttpUrlAlsoWorks() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"http://example.com/path?q=1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    // ── POST /shorten → GET /{code} round-trip ────────────────────

    @Test
    void shortenThenRedirectFollowsToOriginalUrl() throws Exception {
        MvcResult result = mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://www.github.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Extract the code from the JSON response string
        String code = body.split("\"code\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/" + code))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.github.com"));
    }

    // ── POST /shorten — validation errors ────────────────────────

    @Test
    void emptyUrlReturnsBadRequestWithError() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingUrlKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void urlWithoutSchemReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void ftpSchemeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"ftp://files.example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * FIX (new): "https://" alone has no hostname — the controller now
     * validates the host is present and non-blank.
     */
    @Test
    void urlWithNoHostReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * FIX (new): Malformed JSON body (no quotes around value) must return
     * a JSON 400, not a 500 HTML white-label page, thanks to GlobalExceptionHandler.
     */
    @Test
    void malformedJsonBodyReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not-json-at-all"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── GET /{code} ───────────────────────────────────────────────

    /**
     * FIX (original): "/xxxxxx" bypasses the controller regex [a-f0-9]{6}
     * because 'x' is not a hex digit. Spring returns no-handler 404, not the
     * controller's own 404. Using "000000" (valid hex, absent from DB) ensures
     * the controller's findByShortCode → empty → 404 path is actually tested.
     */
    @Test
    void unknownValidHexCodeReturns404() throws Exception {
        mockMvc.perform(get("/000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonHexCodeIsNotRoutedToController() throws Exception {
        // "zzzzzz" doesn't match [a-f0-9]{6} so no controller handles it —
        // Spring Boot's default 404 applies (not our controller 404, which is fine).
        mockMvc.perform(get("/zzzzzz"))
                .andExpect(status().isNotFound());
    }
}
