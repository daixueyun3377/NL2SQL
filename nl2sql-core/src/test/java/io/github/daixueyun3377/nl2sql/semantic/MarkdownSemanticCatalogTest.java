package io.github.daixueyun3377.nl2sql.semantic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownSemanticCatalogTest {

  @Test
  void resolvesDomainByKeyword() {
    String md =
        "## default\n"
            + "domain: default\n"
            + "keywords:\n"
            + "tables:\n"
            + "### relation_hints\n\n"
            + "### business_rules\n\n"
            + "### examples\n\n"
            + "## job\n"
            + "domain: job\n"
            + "keywords: 岗位, 招聘\n"
            + "tables: job_basic_info\n"
            + "### relation_hints\n"
            + "JOIN hint text\n"
            + "### business_rules\n"
            + "status = 1\n"
            + "### examples\n"
            + "question: 有多少岗位\n"
            + "```sql\n"
            + "SELECT 1\n"
            + "```\n";

    MarkdownSemanticCatalog catalog = new MarkdownSemanticCatalog(md);
    SemanticContext ctx = catalog.resolve("上海有多少招聘岗位");
    assertEquals("job", ctx.getDomain());
    assertTrue(ctx.getTables().contains("job_basic_info"));
    assertEquals("JOIN hint text", ctx.getRelationHints());
    assertEquals(1, ctx.getExamples().size());
  }
}
