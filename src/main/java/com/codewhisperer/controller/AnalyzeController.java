
package com.codewhisperer.controller;

import com.codewhisperer.model.*;
import com.codewhisperer.service.*;
import com.github.javaparser.ast.CompilationUnit;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AstAnalysisService ast;
    private final AiService ai;

    public AnalyzeController(
            AstAnalysisService ast,
            AiService ai) {
        this.ast = ast;
        this.ai = ai;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(
            @Valid @RequestBody AnalyzeRequest req) {

        if (req.language() != null
                && !req.language().isBlank()
                && !req.language().equalsIgnoreCase("java")) {

            return ResponseEntity.badRequest()
                    .body("Only Java is supported.");
        }

        try {
            CompilationUnit cu = ast.parse(req.code());

            List<Issue> issues = ast.analyze(cu);
            String intent = ast.inferIntent(cu);

            int methods = cu.findAll(
                    com.github.javaparser.ast.body
                            .MethodDeclaration.class
            ).size();

            int classes = cu.findAll(
                    com.github.javaparser.ast.body
                            .ClassOrInterfaceDeclaration.class
            ).size();

            double complexity = methods == 0 ? 0 :
                    cu.findAll(
                                    com.github.javaparser.ast.body
                                            .MethodDeclaration.class
                            ).stream()
                            .mapToInt(ast::complexity)
                            .average()
                            .orElse(0);

            String rewritten = req.code();
            String aiText = "AI not requested.";

            if (req.useAi()) {
                try {
                    String candidate =
                            ai.improveCode(req.code());

                    // Validate AI-generated Java syntax.
                    ast.parse(candidate);

                    rewritten = candidate;
                    aiText = "Gemini AI generated the improved code. "
                            + "Syntax parsing passed.";

                } catch (Exception e) {
                    aiText = "AI improvement failed: "
                            + e.getMessage();
                }
            }

            return ResponseEntity.ok(
                    new AnalyzeResponse(
                            "Java",
                            intent,
                            classes,
                            methods,
                            req.code().split("\\R", -1).length,
                            complexity,
                            issues,
                            rewritten,
                            aiText
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Could not parse Java: "
                            + e.getMessage());
        }
    }
}
