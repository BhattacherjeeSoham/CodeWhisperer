package com.codewhisperer.model;

public record Issue(String type, String severity, int line, String message, String suggestion) {}
