# NL2SQL

[![Java](https://img.shields.io/badge/Java-8-orange)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

可扩展的 **Java NL2SQL 引擎**：将自然语言问题转为 **安全的只读 SQL** 并执行，返回结构化结果。适用于 Spring Boot 服务、AgentMark Tool、BI/运营后台等场景。

- **core 无 Spring 依赖**，可单独嵌入或单测
- **语义层 + Prompt** 由应用方用 **Markdown 本地维护**，改文件即可调优
- **流水线可插拔**：Schema / LLM / 校验 / 执行均可替换实现

详细设计见：[NL2SQL 独立库设计文档（Maven）](https://gingjqcjzc.feishu.cn/docx/L7gBdr1ekorRLzxVxl6cYXjonPd)

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
  - [Spring Boot（推荐）](#spring-boot推荐)
  - [纯 Java（无 Spring）](#纯-java无-spring)
  - [AgentMark Tool](#agentmark-tool)
- [配置说明](#配置说明)
- [Markdown 模板](#markdown-模板)
- [维护语义层与 Prompt](#维护语义层与-prompt)
- [查询结果](#查询结果)
- [架构](#架构)
- [扩展点](#扩展点)
- [开发](#开发)
- [Roadmap](#roadmap)
- [License](#license)

---

## Features

| 能力 | 说明 |
|------|------|
| NL → SQL | OpenAI 兼容 API（DeepSeek 等）生成 `SELECT` |
| Schema 策略 | `FULL` / `RELEVANT` / `CUSTOM` 三种模式 |
| 安全校验 | 仅 SELECT、表白名单、禁多语句与注释 |
| 执行保护 | 自动 `LIMIT`、`queryTimeout` |
| 语义层 | 域、表、JOIN、业务规则、Few-shot（Markdown） |
| Spring Boot | `nl2sql.enabled=true` 自动注册 `NL2SqlEngine` |

---

## Requirements

- **JDK 8+**
- **Maven 3.6+**
- 可访问的 **MySQL**（或兼容 `information_schema` 的数据源）
- **OpenAI 兼容** LLM API（如 DeepSeek）

---

## Installation

在业务项目 `pom.xml` 中引入（需先安装到本地或私服）：

```xml
<dependency>
  <groupId>io.github.daixueyun3377</groupId>
  <artifactId>nl2sql-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

仅使用引擎 API、自行装配 Bean 时：

```xml
<dependency>
  <groupId>io.github.daixueyun3377</groupId>
  <artifactId>nl2sql-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

本地安装本仓库（**自动安装到** `/Users/qianhua/workspaces/tools/repository`，由 `.mvn/settings.xml` 配置）：

```bash
git clone git@github.com:daixueyun3377/NL2SQL.git
cd NL2SQL
mvn clean install
```

> 在 NL2SQL 目录下执行 `mvn` 时会自动加载 `.mvn/maven.config`。Sponge 等其它项目需在 `settings.xml` 中配置同一 `localRepository`，才能解析到 `0.1.0-SNAPSHOT`。

---

## Quick Start

### Spring Boot（推荐）

**1. 添加依赖**（见 [Installation](#installation)）。

**2. 复制两个 Markdown 模板**到应用工程（见 [Markdown 模板](#markdown-模板)）：

```bash
mkdir -p src/main/resources/nl2sql
cp docs/templates/semantic-catalog.template.md src/main/resources/nl2sql/semantic-catalog.md
cp docs/templates/llm-prompt.template.md src/main/resources/nl2sql/llm-prompt.md
```

目标结构：

```
src/main/resources/nl2sql/
├── semantic-catalog.md    ← 语义层（表、JOIN、规则、示例）
└── llm-prompt.md          ← LLM System / User Prompt
```

**3. 配置 `application.yml`：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db
    username: reader
    password: ***

nl2sql:
  enabled: true
  max-rows: 100
  timeout-seconds: 30
  schema-mode: FULL          # FULL | RELEVANT | CUSTOM
  tables:
    - job
    - job_address
    - job_basic_info
    - city
    - brand
  llm:
    api-key: ${LLM_API_KEY}
    model: deepseek-chat
    base-url: https://api.deepseek.com/v1/
    prompt-resource: nl2sql/llm-prompt.md
  semantic:
    provider: markdown
    markdown-resource: nl2sql/semantic-catalog.md
```

**4. 注入并调用：**

```java
import io.github.daixueyun3377.nl2sql.api.NL2SqlEngine;
import io.github.daixueyun3377.nl2sql.api.QueryResult;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DataQueryService {

    @Resource
    private NL2SqlEngine nl2SqlEngine;

    public QueryResult ask(String question) {
        QueryResult result = nl2SqlEngine.query(question);
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getError());
        }
        return result;
    }
}
```

---

### 纯 Java（无 Spring）

```java
import io.github.daixueyun3377.nl2sql.api.NL2SqlEngine;
import io.github.daixueyun3377.nl2sql.api.QueryResult;
import io.github.daixueyun3377.nl2sql.config.NL2SqlConfig;
import io.github.daixueyun3377.nl2sql.llm.LlmConfig;
import io.github.daixueyun3377.nl2sql.llm.LlmPromptTemplate;
import io.github.daixueyun3377.nl2sql.schema.SchemaMode;
import io.github.daixueyun3377.nl2sql.support.NL2SqlFactories;

import javax.sql.DataSource;
import java.util.Arrays;

public class Example {

    public static void main(String[] args) {
        DataSource dataSource = createDataSource();

        LlmPromptTemplate prompt = NL2SqlFactories.loadLlmPromptTemplate(
                "nl2sql/llm-prompt.md", null);

        LlmConfig llmConfig = new LlmConfig(
                System.getenv("LLM_API_KEY"),
                "deepseek-chat",
                "https://api.deepseek.com/v1/",
                null,
                prompt);

        NL2SqlConfig config = NL2SqlFactories.buildConfig(
                dataSource,
                SchemaMode.FULL,
                Arrays.asList("job", "job_basic_info"),
                null,
                100,
                30,
                llmConfig,
                NL2SqlFactories.markdownSemanticCatalog("nl2sql/semantic-catalog.md", null));

        NL2SqlEngine engine = NL2SqlEngine.create(config);
        QueryResult result = engine.query("有多少在招岗位");

        System.out.println("sql: " + result.getSql());
        System.out.println("rows: " + result.getRowCount());
    }

    private static DataSource createDataSource() {
        // 使用 HikariCP、DriverManager 等创建 DataSource
        throw new UnsupportedOperationException("implement me");
    }
}
```

---

### AgentMark Tool

在 Sponge 等业务应用中，将 `NL2SqlEngine` 封装为 Agent Tool（路由与 Function Calling 仍由 AgentMark 负责）：

```java
import io.github.daixueyun3377.nl2sql.api.NL2SqlEngine;
import io.github.daixueyun3377.nl2sql.api.QueryResult;
// import com.xxx.agentmark.annotations.*;

@Resource
private NL2SqlEngine nl2SqlEngine;

@AgentMark(name = "queryDatabase", description = "用自然语言查询数据库")
public QueryResult queryDatabase(@ParamDesc("用户问题") String question) {
    return nl2SqlEngine.query(question);
}
```

> AgentMark 与当前技术栈已兼容；完整 sponge 集成见 [Roadmap](#roadmap)。

---

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `nl2sql.enabled` | `false` | 是否启用自动配置 |
| `nl2sql.max-rows` | `100` | 最大返回行数（无 LIMIT 时自动追加） |
| `nl2sql.timeout-seconds` | `30` | SQL 查询超时（秒） |
| `nl2sql.schema-mode` | `FULL` | `FULL` / `RELEVANT` / `CUSTOM` |
| `nl2sql.tables` | `[]` | 表白名单（LLM 与校验共用） |
| `nl2sql.custom-schema-ddl` | - | `CUSTOM` 模式下的 DDL 文本 |
| `nl2sql.llm.api-key` | - | LLM API Key（必填） |
| `nl2sql.llm.model` | `deepseek-chat` | 模型名 |
| `nl2sql.llm.base-url` | DeepSeek v1 | OpenAI 兼容 base URL |
| `nl2sql.llm.prompt-resource` | `nl2sql/llm-prompt.md` | classpath Prompt 文件 |
| `nl2sql.llm.prompt-file` | - | 本地 Prompt 路径（优先于 resource） |
| `nl2sql.semantic.provider` | `markdown` | `none` / `markdown` / `classpath` |
| `nl2sql.semantic.markdown-resource` | `nl2sql/semantic-catalog.md` | classpath 语义层文件 |
| `nl2sql.semantic.markdown-file` | - | 本地语义层路径（优先于 resource） |

**Schema 模式简述：**

| 模式 | 适用场景 |
|------|----------|
| `FULL` | 核心表 &lt; 30，全量 DDL 给 LLM |
| `RELEVANT` | 表较多，按问题 + 语义层筛选子集 |
| `CUSTOM` | 临时分析，使用 `custom-schema-ddl` |

---

## Markdown 模板

仓库提供两个模板，**复制到业务项目后改名使用**（去掉路径中的 `templates/` 与 `.template` 后缀）。

| 模板 | 仓库路径 | 复制为 |
|------|----------|--------|
| 语义层 | [`docs/templates/semantic-catalog.template.md`](docs/templates/semantic-catalog.template.md) | `src/main/resources/nl2sql/semantic-catalog.md` |
| LLM Prompt | [`docs/templates/llm-prompt.template.md`](docs/templates/llm-prompt.template.md) | `src/main/resources/nl2sql/llm-prompt.md` |

更多说明见 [`docs/templates/README.md`](docs/templates/README.md)。

**打包进 jar 的同名路径**（`mvn install` 后也可从依赖提取）：

- `nl2sql-core` → `nl2sql/templates/semantic-catalog.template.md`
- `nl2sql-core` → `nl2sql/templates/llm-prompt.template.md`

**库内默认示例**（不配模板、直接用 classpath 默认值时）：

- `nl2sql-core` → `nl2sql/semantic-catalog.md`
- `nl2sql-core` → `nl2sql/llm-prompt.md`

---

## 维护语义层与 Prompt

无需改 Java 代码、无需 Nacos：在应用 `resources/nl2sql/` 下维护上述两个文件，**修改后重启应用**即可生效。格式以模板为准，下面为摘要。

### semantic-catalog.md

按业务域划分（每个 `##` 一节），示例：

```markdown
## job
domain: job
keywords: 岗位, 招聘, job
tables: job, job_address, job_basic_info, city, brand

### relation_hints
job JOIN job_address ON job.id = job_address.job_id ...

### business_rules
1. job_basic_info.status = 1 表示在招
2. 软删除：job.delete_at IS NULL ...

### examples
question: 有多少在招岗位
```sql
SELECT COUNT(*) FROM job_basic_info WHERE status = 1 LIMIT 100
```
```

### llm-prompt.md

包含 `## System` 与 `## User` 两段，User 段可使用占位符：

| 占位符 | 说明 |
|--------|------|
| `{{schema}}` | 表结构 DDL |
| `{{question}}` | 用户问题 |
| `{{semantic.block}}` | 语义层汇总 |
| `{{semantic.businessRules}}` | 业务规则 |
| `{{semantic.relationHints}}` | JOIN 说明 |
| `{{semantic.examples}}` | Few-shot 示例 |

库内默认示例已与 Sponge Demo `Nl2SqlLlmClient` 岗位域规则对齐，可按业务覆盖。

---

## 查询结果

`QueryResult` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | `boolean` | `isSuccess()`，无 error 即为成功 |
| `sql` | `String` | 实际执行的 SQL |
| `rows` | `List<Map<String,Object>>` | 结果行 |
| `rowCount` | `int` | 行数 |
| `error` | `String` | 失败原因（校验/LLM/执行） |
| `durationMs` | `long` | 耗时（毫秒） |

```java
QueryResult result = nl2SqlEngine.query("上海有多少在招岗位");
if (result.isSuccess()) {
    String sql = result.getSql();
    List<Map<String, Object>> rows = result.getRows();
} else {
    log.warn("NL2SQL failed: {}", result.getError());
}
```

---

## 架构

```
自然语言 question
       │
       ▼
┌──────────────────────────┐
│ SemanticContextResolver   │  ← semantic-catalog.md（可选）
└──────────────────────────┘
       │
       ▼
┌──────────────────────────┐
│ SchemaProvider            │  ← FULL / RELEVANT / CUSTOM
└──────────────────────────┘
       │
       ▼
┌──────────────────────────┐
│ LlmClient                 │  ← llm-prompt.md + OpenAI 兼容 API
└──────────────────────────┘
       │
       ▼
┌──────────────────────────┐
│ SqlValidator              │  ← 仅 SELECT、表白名单
└──────────────────────────┘
       │
       ▼
┌──────────────────────────┐
│ SqlExecutor               │  ← LIMIT + timeout
└──────────────────────────┘
       │
       ▼
   QueryResult
```

---

## 扩展点

实现以下接口并传入 `NL2SqlConfig.builder()` 即可替换默认行为：

| 接口 | 默认实现 |
|------|----------|
| `SchemaProvider` | `JdbcSchemaProvider` |
| `LlmClient` | `OpenAiCompatibleLlmClient` |
| `SqlValidator` | `DefaultSqlValidator` |
| `SqlExecutor` | `JdbcSqlExecutor` |
| `SemanticCatalogProvider` | `MarkdownSemanticCatalog` |

---

## 开发

### 模块

| 模块 | 说明 |
|------|------|
| `nl2sql-core` | 引擎 API + 默认实现 |
| `nl2sql-spring-boot-starter` | Spring Boot 自动配置 |
| `nl2sql-bom` | 依赖版本对齐 |

### 构建与测试

```bash
mvn clean test
mvn clean install   # 安装到 /Users/qianhua/workspaces/tools/repository
```

**本地仓库路径**：`/Users/qianhua/workspaces/tools/repository`（项目 `.mvn/settings.xml`，仅在本仓库执行 Maven 时生效）。

---

## Roadmap

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 | Maven 多模块骨架 | ✅ |
| P0.5 | MD 语义层 + Prompt 模板 | ✅ |
| P1 | 与 Sponge Demo 行为对齐 | ⏳ |
| P2 | sponge 引入 starter、删除重复代码 | ⏳ |
| P3 | Nacos / JDBC 语义层（可选） | ⏳ |
| P4 | 发布 Maven Central / 私服 | ⏳ |

---

## License

[Apache License 2.0](LICENSE)
