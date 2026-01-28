package com.example.pastebin.service.impl;

import com.example.pastebin.dto.PasteRequest;
import com.example.pastebin.entity.Paste;
import com.example.pastebin.repo.PasteRepository;
import com.example.pastebin.service.PasteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class PasteServiceImpl implements PasteService {

    private final PasteRepository repo;

    public PasteServiceImpl(PasteRepository repo) {
        this.repo = repo;
    }

    private long now(HttpServletRequest req) {
        if ("1".equals(System.getenv("TEST_MODE"))) {
            String h = req.getHeader("x-test-now-ms");
            if (h != null) return Long.parseLong(h);
        }
        return System.currentTimeMillis();
    }

    @Override
    public ResponseEntity<?> createPaste(PasteRequest req, HttpServletRequest http) {

        if (req.getContent() == null || req.getContent().trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid content"));

        if (req.getTtl_seconds() != null && req.getTtl_seconds() < 1)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ttl"));

        if (req.getMax_views() != null && req.getMax_views() < 1)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid max_views"));

        long now = now(http);

        Paste paste = new Paste();
        paste.setId(UUID.randomUUID().toString());
        paste.setContent(req.getContent());
        paste.setMaxViews(req.getMax_views());
        paste.setCurrentViews(0);
        paste.setCreatedAt(now);

        if (req.getTtl_seconds() != null) {
            paste.setExpiresAt(now + req.getTtl_seconds() * 1000);
        }

        repo.save(paste);

        return ResponseEntity.ok(Map.of(
                "id", paste.getId(),
                "url", req.getFrontendUrl() + "/" + paste.getId()
        ));
    }

    @Override
    public ResponseEntity<?> fetchPaste(String id, HttpServletRequest req) {

        Paste paste = repo.findById(id).orElse(null);
        if (paste == null) return notFound();

        long now = now(req);

        if (paste.getExpiresAt() != null && now >= paste.getExpiresAt())
            return notFound();

        if (paste.getMaxViews() != null && paste.getCurrentViews() >= paste.getMaxViews())
            return notFound();

        paste.setCurrentViews(paste.getCurrentViews() + 1);
        repo.save(paste);

        Integer remaining = paste.getMaxViews() == null
                ? null
                : paste.getMaxViews() - paste.getCurrentViews();

        return ResponseEntity.ok(Map.of(
                "content", paste.getContent(),
                "remaining_views", remaining,
                "expires_at", paste.getExpiresAt() == null
                        ? null
                        : Instant.ofEpochMilli(paste.getExpiresAt()).toString()
        ));
    }

    @Override
    public ResponseEntity<String> viewPaste(String id, HttpServletRequest req) {

        ResponseEntity<?> apiResponse = fetchPaste(id, req);
        if (!apiResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(404).body("Not Found");
        }

        Map<?, ?> body = (Map<?, ?>) apiResponse.getBody();
        String safeContent = HtmlUtils.htmlEscape(body.get("content").toString());

        return ResponseEntity.ok("""
            <html>
              <body>
                <pre>%s</pre>
              </body>
            </html>
        """.formatted(safeContent));
    }

    private ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(404)
                .body(Map.of("error", "Not found"));
    }
}
