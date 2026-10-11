package com.codewhisperer.model;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(@NotBlank String code, String language, boolean useAi) {}
