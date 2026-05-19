package io.github.daixueyun3377.nl2sql.executor;

import java.util.List;
import java.util.Map;

/**
 * 扩展点：执行已校验的 SQL。
 */
public interface SqlExecutor {

    List<Map<String, Object>> execute(String sql, ExecuteOptions options);
}
