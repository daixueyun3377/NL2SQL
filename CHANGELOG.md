# Changelog

## [1.0.0](https://github.com/daixueyun3377/NL2SQL/releases/tag/v1.0.0) - 2026-05-19

### Added

- Maven 多模块：`nl2sql-core`、`nl2sql-spring-boot-starter`、`nl2sql-bom`
- 可扩展流水线：Schema → LLM → Validator → Executor
- Markdown 语义层（`MarkdownSemanticCatalog`）与 LLM Prompt 模板（`LlmPromptTemplate`）
- Spring Boot 自动配置（`nl2sql.enabled=true`）
- 默认 SQL 安全校验（仅 SELECT、表白名单）
- H2 单元测试

### Documentation

- README 使用说明与模板路径 `docs/templates/`
- 飞书设计文档同步
