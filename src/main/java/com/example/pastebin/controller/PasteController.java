package com.example.pastebin.controller;

import com.example.pastebin.dto.PasteRequest;
import com.example.pastebin.repo.PasteRepository;

import java.util.Map;

@RestController
public class PasteController {

    private final PasteRepository repo;

    public PasteController(PasteRepository repo) {
        this.repo = repo;
    }

    private long now(HttpServletRequest req) {
        if ("1".equals(System.getenv("TEST_MODE"))) {
            String h = req.getHeader("x-test-now-ms");
            if (h != null) return Long.parseLong(h);
        }
        return System.currentTimeMillis();
    }

    @GetMapping("/api/healthz")
    public Map<String, Boolean> health() {
        return Map.of("ok", true);
    }

    @PostMapping("/api/pastes")
    public ResponseEntity<?> create(
            @RequestBody PasteRequest req,
            HttpServletRequest http) {

        if (req.getContent() == null || req.getContent().trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid content"));

        if (req.getTtl_seconds() != null && req.getTtl_seconds() < 1)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid ttl"));

        if (req.getMax_views() != null && req.getMax_views() < 1)
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid max_views"));

        long now = now(http);

        Paste p = new Paste();
        p.setId(UUID.randomUUID().toString());
        p.setContent(req.getContent());
        p.setMaxViews(req.getMax_views());
        p.setCurrentViews(0);
        p.setCreatedAt(now);

        if (req.getTtl_seconds() != null)
            p.setExpiresAt(now + req.getTtl_seconds() * 1000);

        repo.save(p);

        return ResponseEntity.ok(Map.of(
                "id", p.getId(),
                "url", "https://your-domain/p/" + p.getId()
        ));
    }

    @GetMapping("/api/pastes/{id}")
    public ResponseEntity<?> fetch(
            @PathVariable String id,
            HttpServletRequest req) {

        Paste p = repo.findById(id).orElse(null);
        if (p == null) return notFound();

        long now = now(req);

        if (p.getExpiresAt() != null && now >= p.getExpiresAt())
            return notFound();

        if (p.getMaxViews() != null && p.getCurrentViews() >= p.getMaxViews())
            return notFound();

        p.setCurrentViews(p.getCurrentViews() + 1);
        repo.save(p);

        Integer remaining = p.getMaxViews() == null
                ? null
                : p.getMaxViews() - p.getCurrentViews();

        return ResponseEntity.ok(Map.of(
                "content", p.getContent(),
                "remaining_views", remaining,
                "expires_at", p.getExpiresAt() == null
                        ? null
                        : Instant.ofEpochMilli(p.getExpiresAt()).toString()
        ));
    }

    @GetMapping("/p/{id}")
    public ResponseEntity<String> view(
            @PathVariable String id,
            HttpServletRequest req) {

        ResponseEntity<?> api = fetch(id, req);
        if (!api.getStatusCode().is2xxSuccessful())
            return ResponseEntity.status(404).body("Not Found");

        Map<?, ?> body = (Map<?, ?>) api.getBody();
        String safe = HtmlUtils.htmlEscape(body.get("content").toString());

        return ResponseEntity.ok("""
            <html><body><pre>%s</pre></body></html>
        """.formatted(safe));
    }

    private ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(404)
                .body(Map.of("error", "Not found"));
    }
}
