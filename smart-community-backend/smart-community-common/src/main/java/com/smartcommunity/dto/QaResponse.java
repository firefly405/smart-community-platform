package com.smartcommunity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QaResponse {
    private Long id;
    private String question;
    private String answer;
    private String sources;
    private Long userId;
    private String userName;
    private LocalDateTime createTime;
}
