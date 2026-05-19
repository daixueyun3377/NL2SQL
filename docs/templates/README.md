# NL2SQL Markdown 模板

应用集成时，将下列模板复制到业务工程 `src/main/resources/nl2sql/`，去掉 `.template` 后缀即可使用。

| 模板文件 | 复制为 | 作用 |
|----------|--------|------|
| [semantic-catalog.template.md](./semantic-catalog.template.md) | `semantic-catalog.md` | 语义层：域、表、JOIN、业务规则、Few-shot |
| [llm-prompt.template.md](./llm-prompt.template.md) | `llm-prompt.md` | LLM System / User Prompt 与占位符 |

## 一键复制（在业务项目根目录执行）

```bash
mkdir -p src/main/resources/nl2sql
cp /path/to/NL2SQL/docs/templates/semantic-catalog.template.md src/main/resources/nl2sql/semantic-catalog.md
cp /path/to/NL2SQL/docs/templates/llm-prompt.template.md src/main/resources/nl2sql/llm-prompt.md
```

将 `/path/to/NL2SQL` 换成本仓库克隆路径。

## 从 Maven 依赖 jar 提取

安装 `nl2sql-core` 后，模板也在 jar 的 `nl2sql/templates/` 下：

```bash
jar xf /Users/qianhua/workspaces/tools/repository/io/github/daixueyun3377/nl2sql-core/0.1.0-SNAPSHOT/nl2sql-core-0.1.0-SNAPSHOT.jar \
  nl2sql/templates/semantic-catalog.template.md \
  nl2sql/templates/llm-prompt.template.md
```

## 与默认示例的区别

| 路径 | 说明 |
|------|------|
| `docs/templates/*.template.md` | **模板**（带说明，供复制） |
| `nl2sql-core/.../nl2sql/semantic-catalog.md` | 库内**默认示例**（可直接作为 classpath 默认值） |
| `nl2sql-core/.../nl2sql/templates/*.template.md` | 与 `docs/templates/` 内容同步，打包进 jar |

应用方应以 **复制模板到自家 `resources/nl2sql/` 并修改** 为准，避免直接改依赖 jar 内文件。
