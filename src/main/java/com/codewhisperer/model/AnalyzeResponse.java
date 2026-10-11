package com.codewhisperer.model;

import java.util.List;

public record AnalyzeResponse(String language, String intent, int classes, int methods, int lines,
                              double cyclomaticComplexity, List<Issue> issues, String rewrittenCode,
                              String aiExplanation) {}
