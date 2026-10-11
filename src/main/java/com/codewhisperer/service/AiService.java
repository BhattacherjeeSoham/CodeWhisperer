
package com.codewhisperer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final HttpClient client =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper();

    public String improveCode(String originalCode)
            throws Exception {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is missing."
            );
        }

        String model = "gemini-3-flash-preview";

        String prompt = """
                        You are an expert Java code optimization assistant.
                
                        Analyze and improve the following Java code.
                
                        Requirements:
                        1. Preserve the original functionality and output.
                        2. Reduce unnecessary code length wherever possible.
                        3. Remove redundant statements and repeated logic.
                        4. Simplify loops and conditional statements when safe.
                        5. Use concise but meaningful variable names.
                        6. Improve readability and maintainability.
                        7. Avoid unnecessary classes, methods, and variables.
                        8. Keep existing public class and method signatures unchanged.
                        9. Do not introduce external dependencies.
                        10. Prefer shorter code only when it remains clear and correct.
                        11. Return ONLY complete Java code.
                        12. Do not include Markdown or explanations.
                
                        Important:
                        The improved code should be shorter than the original
                        whenever safe simplifications are possible.
                        Do not sacrifice correctness just to reduce line count.
                Java code:
                """ + originalCode;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 8192

                )
        );

        String json = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://generativelanguage.googleapis.com"
                                + "/v1beta/models/" + model
                                + ":generateContent"
                ))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Gemini API returned HTTP "
                            + response.statusCode()
            );
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini returned no code."
            );
        }

        JsonNode candidate = candidates.get(0);

        if (!"STOP".equals(
                candidate.path("finishReason").asText())) {
            throw new IllegalStateException(
                    "Gemini did not finish generating code."
            );
        }

        StringBuilder result = new StringBuilder();

        for (JsonNode part :
                candidate.path("content").path("parts")) {
            result.append(part.path("text").asText(""));
        }

        String improvedCode = result.toString().trim();

        // Remove Markdown code fences if present.
        improvedCode = improvedCode.replaceFirst(
                "^```(?:java)?\\s*", ""
        ).replaceFirst(
                "\\s*```$", ""
        ).trim();

        if (improvedCode.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini returned empty code."
            );
        }

        return improvedCode;
    }
}
