# CodeWhisperer — Java Code Analyzer

An MVP that accepts messy Java code and:
- parses it into an AST using JavaParser
- checks naming conventions
- estimates cyclomatic complexity
- detects repeated string literals
- infers intent heuristically
- generates a deterministic cleaned rewrite
- optionally asks an AI model for explanations/refactoring advice

## Requirements
- Java 21
- Maven 3.9+

## Run
```bash
mvn spring-boot:run
```

## API
POST `http://localhost:8080/api/analyze`

Example:
```json
{
  "language": "java",
  "useAi": false,
  "code": "class Test { int calc(int[] arr){ int res=0; for(int x:arr){res+=x;} return res;} }"
}
```

## AI mode
Set `OPENAI_API_KEY`. Optionally set `OPENAI_MODEL`. The project uses the OpenAI Responses API endpoint; the older Assistants API should not be used for a new integration.

## Production roadmap
1. Add a React/Monaco frontend.
2. Add Checkstyle/PMD/SpotBugs adapters.
3. Add JavaParser symbol/type resolution for stronger bug detection.
4. Add AST-safe transformations instead of regex/string rewrites.
5. Add tests that compile the rewritten code before showing it to users.
6. Add GitHub repository upload and PR diff support.
7. Add authentication, rate limiting and code-retention controls.
