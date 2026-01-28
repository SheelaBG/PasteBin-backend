package com.example.pastebin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasteRequest {
    private String content;
    private Integer ttl_seconds;
    private Integer max_views;
    private String frontendUrl;

}
