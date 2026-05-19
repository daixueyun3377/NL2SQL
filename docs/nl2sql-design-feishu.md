# NL2SQL 独立库设计文档（Maven）

> **仓库**：[github.com/daixueyun3377/NL2SQL](https://github.com/daixueyun3377/NL2SQL)（落地路径：`/Users/qianhua/workspaces/codes/NL2SQL`）
>
> **关联**：[Sponge Data Agent 整体设计方案](https://gingjqcjzc.feishu.cn/docx/LMeSdE7eKoWrlQxk81bc4g2SnII) · [单 Agent MVP 测试发布手册](https://gingjqcjzc.feishu.cn/docx/EqKpdAW5LoTEM7xkbLxcU4QjnXc)
>
> **版本**：v0.1-implemented | **日期**：2026-05-19
>
> **说明**：仓库已落地 P0 + P0.5；语义层与 LLM Prompt 由**应用方本地 Markdown** 维护；JDK **Java 8**（暂不升级）；AgentMark 技术栈**当前兼容**。

---

## 1. 背景与目标

### 1.1 为什么要独立成 Maven 库

- **复用**：Sponge 及其他 Java 服务（客服、BI、运营后台）共用同一套 NL2SQL 引擎
- **解耦**：引擎迭代发版不绑死 sponge 业务发版
- **可测**：core 层用 H2 + Mock LLM 单测，无需启动完整 Spring 应用
- **对齐设计**：与 Sponge 方案中的 `nl2sql-core` + `nl2sql-spring-boot-starter` + Maven Central 路线图一致

### 1.2 非目标（本库不负责）

- Agent 路由、Function Calling（由 AgentMark / 业务方负责）
- 具体业务 Tool（`queryJobList` 等留在 sponge-application）
- HTTP / MCP / SDK 暴露（由 sponge 或各业务服务适配）

---

## 2. 仓库与模块结构

### 2.1 Maven 多模块（已实现）

```
NL2SQL/
├── pom.xml                      # parent: packaging pom, Java 8
├── .gitignore                   # Java/Maven/IDE 标准忽略
├── nl2sql-core/                 # 核心引擎（无 Spring 强依赖）
│   └── src/main/java/io/github/daixueyun3377/nl2sql/
│   └── src/main/resources/nl2sql/
│       ├── semantic-catalog.md           # 默认语义层示例
│       ├── llm-prompt.md                 # 默认 Prompt（对齐 Sponge Demo）
│       └── templates/
│           ├── semantic-catalog.template.md
│           └── llm-prompt.template.md
├── nl2sql-spring-boot-starter/  # Spring Boot 自动配置
├── nl2sql-bom/                  # 依赖版本对齐
└── README.md
```

### 2.2 坐标

| artifactId | 说明 |
|------------|------|
| `nl2sql-core` | 引擎 API + 默认实现 |
| `nl2sql-spring-boot-starter` | 自动注册 Bean、读取配置 |
| `nl2sql-bom` | 依赖版本对齐 |

groupId 与 AgentMark 对齐：`io.github.daixueyun3377`，版本 `0.1.0-SNAPSHOT`。

### 2.3 依赖边界

| 模块 | 允许依赖 | 禁止 |
|------|----------|------|
| nl2sql-core | JDK8、JDBC API、Jackson、OkHttp、SLF4J | Spring、MyBatis、AgentMark |
| starter | spring-boot-autoconfigure、nl2sql-core、spring-jdbc（optional） | 业务 domain 类 |

---

## 3. 核心架构

### 3.1 处理流水线

```
自然语言 question
    │
    ▼
┌─────────────────────────────────────┐
│ SemanticContextResolver（可选）      │  ← Markdown 语义层 / 可插拔 Provider
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ SchemaProvider                       │  ← FULL / RELEVANT / CUSTOM
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ LlmClient                            │  ← MD Prompt 模板 + OpenAI 兼容 API
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ SqlValidator                         │  ← 仅 SELECT、表白名单、禁关键字
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ SqlExecutor                          │  ← 执行 + maxRows + timeout
└─────────────────────────────────────┘
    │
    ▼
QueryResult（sql, rows, rowCount, error, durationMs）
```

### 3.2 核心接口（nl2sql-core）

```java
// 门面
public interface NL2SqlEngine {
    QueryResult query(String question);
    static NL2SqlEngine create(NL2SqlConfig config) { ... }
}

// 扩展点
public interface SchemaProvider { String getSchema(SchemaRequest req); }
public interface LlmClient { String generateSql(SqlGenerateRequest req); }
public interface SqlValidator { void validate(String sql, ValidationContext ctx); }
public interface SqlExecutor { List<Map<String,Object>> execute(String sql, ExecuteOptions opt); }
public interface SemanticCatalogProvider { SemanticContext resolve(String question); }
```

### 3.3 与 Sponge Demo 的类映射

| Sponge Demo（`/Users/qianhua/workspaces/codes/sponge`） | nl2sql-core / starter |
|--------------------------------------------------------|------------------------|
| `JdbcSchemaProvider` | `JdbcSchemaProvider` + `FullSchemaProvider` / `RelevantSchemaProvider` |
| `Nl2SqlLlmClient` | `OpenAiCompatibleLlmClient` + **`llm-prompt.md` 模板** |
| `SqlSafetyValidator` | `DefaultSqlValidator` |
| `Nl2SqlService` | `DefaultNL2SqlEngine` |
| `Nl2SqlProperties` | starter `Nl2SqlProperties` → `NL2SqlConfig` |
| `Nl2SqlToolService` | 保留在 sponge（@AgentMark） |

---

## 4. Schema 策略

| 模式 | 类名 | 行为 | 适用 |
|------|------|------|------|
| FULL | `FullSchemaProvider` | 白名单表全量 DDL（information_schema） | <30 张核心表 |
| RELEVANT | `RelevantSchemaProvider` | 按问题 + SemanticCatalog 筛选子集 | 30～200 张表 |
| CUSTOM | `CustomSchemaProvider` | 调用方传入 DDL 文本 | 临时分析、单报表 |

DDL 格式：从 `information_schema.COLUMNS` 生成 `CREATE TABLE` 风格文本，包含 `COLUMN_COMMENT`。

---

## 5. 语义层（应用方本地 Markdown 维护）

### 5.1 设计原则

语义层 = 表业务含义 + JOIN 关系 + 过滤口径 + few-shot SQL，与物理 Schema 分离。**默认由应用工程本地 MD 维护**，改文件后重启即可生效，无需 Nacos 发版（Nacos/DB 为后续可选扩展）。

### 5.2 接口

```java
public class SemanticContext {
    String domain;
    List<String> tables;
    String relationHints;
    String businessRules;
    List<SqlExample> examples;
}

public interface SemanticCatalogProvider {
    SemanticContext resolve(String question);
}
```

### 5.3 实现方式（可插拔）

| 实现 | 存储 | 刷新 | 状态 |
|------|------|------|------|
| **`MarkdownSemanticCatalog`** | 应用 `nl2sql/semantic-catalog.md`（classpath 或 `markdown-file`） | 改 MD + 重启 | **✅ 默认推荐** |
| `ClasspathSemanticCatalog` | `resources/catalog/*.json` | 发版 | ✅ 兼容 |
| `NacosSemanticCatalog` | Nacos YAML | 监听变更 | ⏳ 后续 |
| `JdbcSemanticCatalog` | DB 表 | 缓存 TTL | ⏳ 后续 |

**应用方用法**：

1. 从库内复制模板：`nl2sql/templates/semantic-catalog.template.md`
2. 放到应用：`src/main/resources/nl2sql/semantic-catalog.md`
3. 配置 `nl2sql.semantic.provider=markdown`

**MD 格式概要**（每域一个 `##` 节）：

```markdown
## job
domain: job
keywords: 岗位, 招聘, job
tables: job, job_address, job_basic_info, city, brand

### relation_hints
（JOIN 说明）

### business_rules
（业务口径，如 status=1、软删除）

### examples
question: 有多少在招岗位
```sql
SELECT COUNT(*) FROM job_basic_info WHERE status = 1
```
```

Sponge 岗位域首批规则已从 Demo `Nl2SqlLlmClient` / `JobReadMapper.xml` 提炼进默认示例 `semantic-catalog.md`。

---

## 6. LLM Prompt（应用方本地 Markdown 维护）

### 6.1 设计

Prompt 拆为 **`## System`** 与 **`## User`** 两段，由 `LlmPromptTemplate` 解析；User 段支持占位符渲染。

### 6.2 占位符

| 占位符 | 说明 |
|--------|------|
| `{{schema}}` | 表结构 DDL |
| `{{question}}` | 用户问题 |
| `{{semantic.block}}` | 语义层汇总（JOIN + 规则 + 示例） |
| `{{semantic.businessRules}}` | 业务规则 |
| `{{semantic.relationHints}}` | JOIN 说明 |
| `{{semantic.examples}}` | Few-shot |

### 6.3 应用方用法

1. 复制模板：`nl2sql/templates/llm-prompt.template.md` → `nl2sql/llm-prompt.md`
2. 配置 `nl2sql.llm.prompt-resource` 或 `prompt-file`（本地文件优先）
3. 默认示例已与 Sponge `Nl2SqlLlmClient` 中文规则对齐（软删除、城市 JOIN、LIMIT 等）

---

## 7. 安全设计

| 措施 | 实现位置 |
|------|----------|
| 仅 SELECT | `DefaultSqlValidator` 关键字 + 开头校验 |
| 表白名单 | 配置 `tables` + Validator 解析 FROM/JOIN |
| 禁注释、多语句 | Validator |
| 结果集上限 | 无 LIMIT 时 `JdbcSqlExecutor` 追加 `LIMIT maxRows` |
| 查询超时 | `Statement.setQueryTimeout` |
| 敏感字段 | 可选 `ColumnMasker` 扩展点（后续） |

---

## 8. Spring Boot Starter

### 8.1 自动配置

```java
@Configuration
@EnableConfigurationProperties(Nl2SqlProperties.class)
@ConditionalOnProperty(prefix = "nl2sql", name = "enabled", havingValue = "true")
public class Nl2SqlAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    public NL2SqlEngine nl2SqlEngine(Nl2SqlProperties props, DataSource ds, ...) { ... }
}
```

### 8.2 配置项（当前实现）

```yaml
nl2sql:
  enabled: true
  max-rows: 100
  timeout-seconds: 30
  schema-mode: FULL          # FULL | RELEVANT | CUSTOM
  tables: [job, job_address, job_basic_info, city, brand]
  llm:
    api-key: ${agentmark.api-key:}
    model: ${agentmark.model:deepseek-chat}
    base-url: ${agentmark.base-url:https://api.deepseek.com/v1/}
    prompt-resource: nl2sql/llm-prompt.md
    # prompt-file: ./config/nl2sql/llm-prompt.md   # 本地文件优先
  semantic:
    provider: markdown       # none | markdown | classpath
    markdown-resource: nl2sql/semantic-catalog.md
    # markdown-file: ./config/nl2sql/semantic-catalog.md
```

### 8.3 Sponge 集成方式

```xml
<dependency>
  <groupId>io.github.daixueyun3377</groupId>
  <artifactId>nl2sql-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
@Resource
private NL2SqlEngine nl2SqlEngine;

@AgentMark(name = "queryDatabase", ...)
public QueryResult queryDatabase(@ParamDesc("问题") String question) {
    return nl2SqlEngine.query(question);
}
```

Sponge 本机路径：`/Users/qianhua/workspaces/codes/sponge`

---

## 9. 与 Sponge Data Agent 的关系

| 层级 | 归属 |
|------|------|
| NL2SQL 引擎 | **NL2SQL 仓库**（本库） |
| AgentMark 路由 + 业务 Tools | sponge-application |
| HTTP /api/agent/* | sponge-interfaces |
| MCP / SDK（后续） | sponge 或独立 adapter |

> **路由策略不变**：接口优先 → `queryDatabase` 兜底。NL2SQL 库只保证「给定 question 返回 QueryResult」，不参与 Tool 选择。

---

## 10. 迁移计划（Sponge Demo → 独立库）

| 阶段 | 内容 | 产出 | 状态 |
|------|------|------|------|
| P0 | 克隆仓库、模块骨架、`.gitignore`、可编译 | parent + core + starter + bom | ✅ 已完成 |
| P0.5 | MD 语义层 + MD Prompt 模板、`MarkdownSemanticCatalog`、`LlmPromptTemplate` | 应用方本地可维护 | ✅ 已完成 |
| P1 | 从 sponge 迁移并对齐 Validator / Schema 细节 | core 单测 + 与 Demo 行为一致 | ⏳ 进行中 |
| P2 | sponge 改依赖、删重复 nl2sql 代码 | sponge 集成测试通过 | ⏳ |
| P3 | Nacos / JDBC 语义层（可选，非必须） | 集中配置 + 热更新 | ⏳ |
| P4 | 发布私服 / Maven Central | 其他项目可引依赖 | ⏳ |

---

## 11. 测试策略

- **core 单测**：Validator 拒绝 DELETE；Mock LlmClient；H2 流水线（`mvn clean test` 已通过）
- **starter 集成测**：`@SpringBootTest` 注入 `NL2SqlEngine`（待补）
- **sponge 回归**：沿用 MVP 手册 L3（SQL-01～04）+ L1 路由

---

## 12. 仓库核对清单

- [x] 模块结构与本文 2.1 一致（parent / core / starter / bom）
- [x] `NL2SqlEngine` API 与签名已落地
- [x] `LlmClient` 已抽象，`OpenAiCompatibleLlmClient` + MD Prompt
- [x] AgentMark 技术栈当前兼容（无需额外适配）
- [x] README 与本文迁移计划已对齐
- [ ] P1：与 Sponge `SqlSafetyValidator` 等行为逐条对齐
- [ ] P2：sponge 依赖本库并删除重复实现

---

## 13. 下一步

1. **P1**：从 `/Users/qianhua/workspaces/codes/sponge` 迁移并对齐 core 实现细节
2. **P2**：sponge 引入 `nl2sql-spring-boot-starter`，业务 MD 放到 `sponge-start/src/main/resources/nl2sql/`
3. 发 `0.1.0-SNAPSHOT` 到私服后做 sponge 回归（MVP L3 + L1）
