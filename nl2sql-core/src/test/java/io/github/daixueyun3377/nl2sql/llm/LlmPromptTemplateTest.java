package io.github.daixueyun3377.nl2sql.llm;

import io.github.daixueyun3377.nl2sql.semantic.SemanticContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmPromptTemplateTest {

  @Test
  void rendersPlaceholders() {
    String md =
        "## System\n"
            + "system text\n"
            + "## User\n"
            + "schema={{schema}}\n"
            + "q={{question}}\n"
            + "rules={{semantic.businessRules}}\n";

    LlmPromptTemplate template = LlmPromptTemplate.parseMarkdown(md);
    SqlGenerateRequest request =
        new SqlGenerateRequest(
            "有多少岗位",
            "CREATE TABLE t(id INT);",
            new SemanticContext("job", null, null, "status=1", null));

    String user = template.renderUserPrompt(request);
    assertTrue(user.contains("CREATE TABLE t"));
    assertTrue(user.contains("有多少岗位"));
    assertTrue(user.contains("status=1"));
    assertTrue(template.getSystemPrompt().contains("system text"));
  }
}
