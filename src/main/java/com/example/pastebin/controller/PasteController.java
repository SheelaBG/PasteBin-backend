package com.example.pastebin.controller;

import com.example.pastebin.dto.PasteRequest;
import com.example.pastebin.service.PasteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
public class PasteController {

    private final PasteService pasteService;

    public PasteController(PasteService pasteService) {
        this.pasteService = pasteService;
    }

    @GetMapping("/api/healthz")
    public ResponseEntity<?> health() {
        if (pasteService.isHealthy()) {
            return ResponseEntity.ok(Map.of("ok", true));
        }
        return ResponseEntity.status(500)
                .body(Map.of("ok", false));
    }

    @PostMapping("/api/pastes")
    public ResponseEntity<?> create(
            @RequestBody PasteRequest req,
            HttpServletRequest http) {

        return pasteService.createPaste(req, http);
    }

    @GetMapping("/api/pastes/{id}")
    public ResponseEntity<?> fetch(
            @PathVariable String id,
            HttpServletRequest req) {

        return pasteService.fetchPaste(id, req);
    }

    @GetMapping("/p/{id}")
    public ResponseEntity<String> view(
            @PathVariable String id,
            HttpServletRequest req) {

        return pasteService.viewPaste(id, req);
    }
}
