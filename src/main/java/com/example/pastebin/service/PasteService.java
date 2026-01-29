package com.example.pastebin.service;

import com.example.pastebin.dto.PasteRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface PasteService {

    ResponseEntity<?> createPaste(PasteRequest request, HttpServletRequest http);

    ResponseEntity<?> fetchPaste(String id, HttpServletRequest request);

    ResponseEntity<String> viewPaste(String id, HttpServletRequest request);

    boolean isHealthy();
}
