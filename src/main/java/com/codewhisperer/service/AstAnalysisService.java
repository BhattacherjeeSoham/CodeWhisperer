package com.codewhisperer.service;

import com.codewhisperer.model.Issue;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AstAnalysisService {
    public CompilationUnit parse(String code) { return StaticJavaParser.parse(code); }

    public List<Issue> analyze(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();
        cu.findAll(VariableDeclarator.class).forEach(v -> {
            String n = v.getNameAsString();
            if (n.length() == 1 && !Set.of("i","j","k","x","y").contains(n))
                issues.add(issue("NAMING", "LOW", v.getBegin().map(p -> p.line).orElse(0),
                        "Variable name '" + n + "' is not descriptive.", "Use a meaningful camelCase name."));
            if (n.matches(".*[_].*"))
                issues.add(issue("NAMING", "LOW", v.getBegin().map(p -> p.line).orElse(0),
                        "Java local variable '" + n + "' uses underscores.", "Prefer camelCase."));
        });
        cu.findAll(MethodDeclaration.class).forEach(m -> {
            if (m.getNameAsString().length() > 30)
                issues.add(issue("NAMING", "LOW", m.getBegin().map(p -> p.line).orElse(0),
                        "Method name is unusually long.", "Extract smaller responsibilities or rename it."));
            int complexity = complexity(m);
            if (complexity > 8)
                issues.add(issue("COMPLEXITY", "MEDIUM", m.getBegin().map(p -> p.line).orElse(0),
                        "Method has cyclomatic complexity " + complexity + ".", "Split branches into smaller methods."));
        });
        Map<String, Long> strings = cu.findAll(StringLiteralExpr.class).stream()
                .map(StringLiteralExpr::asString).filter(s -> s.length() >= 4)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        strings.forEach((s, count) -> { if (count >= 3)
            issues.add(issue("DUPLICATION", "LOW", 0, "String literal repeated " + count + " times.", "Extract it into a constant.")); });
        return issues;
    }

    public int complexity(MethodDeclaration m) {
        return 1 + m.findAll(IfStmt.class).size() + m.findAll(ForStmt.class).size() + m.findAll(WhileStmt.class).size()
                + m.findAll(com.github.javaparser.ast.stmt.DoStmt.class).size()
                + m.findAll(com.github.javaparser.ast.expr.ConditionalExpr.class).size();
    }

    public String inferIntent(CompilationUnit cu) {
        String names = cu.findAll(NameExpr.class).stream().map(NameExpr::getNameAsString).map(String::toLowerCase).collect(Collectors.joining(" "));
        if (names.matches(".*(sort|sorted|comparator).*")) return "Likely sorts or orders data.";
        if (names.matches(".*(find|search|lookup|contains).*")) return "Likely searches for or retrieves data.";
        if (names.matches(".*(calculate|sum|total|average|count).*")) return "Likely performs a calculation or aggregation.";
        if (cu.findAll(ForStmt.class).size() + cu.findAll(WhileStmt.class).size() > 0) return "Likely iterates over a collection or range to process data.";
        return "General-purpose Java logic; intent could not be determined confidently from syntax alone.";
    }

    private Issue issue(String t, String s, int line, String m, String sug) { return new Issue(t,s,line,m,sug); }
}
