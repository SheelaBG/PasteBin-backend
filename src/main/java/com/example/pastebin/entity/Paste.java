package com.example.pastebin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Paste {

    @Id
    private String id;

    @Lob
    @Column(nullable = false)
    private String content;

    private Integer maxViews;
    private Integer currentViews;

    private Long expiresAt;
    private Long createdAt;

}
