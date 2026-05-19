package io.github.daixueyun3377.nl2sql.api;

import io.github.daixueyun3377.nl2sql.config.NL2SqlConfig;
import io.github.daixueyun3377.nl2sql.engine.DefaultNL2SqlEngine;

/**
 * NL2SQL 门面：给定自然语言问题，返回查询结果。
 */
public interface NL2SqlEngine {

    QueryResult query(String question);

    static NL2SqlEngine create(NL2SqlConfig config) {
        return new DefaultNL2SqlEngine(config);
    }
}
