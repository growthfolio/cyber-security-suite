package com.codexraziel.cybersec.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackConfig {
    private String profile;
    private String target;
    private String protocol;
    private Integer threads;
    private Boolean stealth;
    private List<String> ports;
    private String wordlistPath;
    private Integer delay;
    private Integer timeout;
}