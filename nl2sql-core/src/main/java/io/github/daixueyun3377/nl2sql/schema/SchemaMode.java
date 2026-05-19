package io.github.daixueyun3377.nl2sql.schema;

/**
 * Schema 提供策略。
 */
public enum SchemaMode {
    /** 白名单表全量 DDL */
    FULL,
    /** 按问题 + 语义层筛选子集 */
    RELEVANT,
    /** 调用方传入 DDL 文本 */
    CUSTOM
}
