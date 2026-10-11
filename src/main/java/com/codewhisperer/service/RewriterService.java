package com.codewhisperer.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.VariableDeclarator;
import org.springframework.stereotype.Service;

@Service
public class RewriterService {
    public String rewrite(CompilationUnit cu) {
        cu.findAll(VariableDeclarator.class).forEach(v -> {
            String n = v.getNameAsString();
            if (n.equals("str")) v.setName("text");
            else if (n.equals("num")) v.setName("number");
            else if (n.equals("res")) v.setName("result");
            else if (n.equals("arr")) v.setName("values");
            else if (n.equals("temp")) v.setName("temporaryValue");
        });
        return cu.toString();
    }
}
