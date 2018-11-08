package org.apache.uss.argus;

import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SQLEvaluatorTest {
    @Test
    void testCompileSql() {
        var sql = "SELECT * FROM Table as T";
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        assertEquals(sql, evaluator.getSql());
        assertEquals(JdbcConstants.POSTGRESQL, evaluator.getDbType());
        assertEquals("Table", evaluator.getSource());
        assertEquals("T", evaluator.getSourceAlias());
    }
}
