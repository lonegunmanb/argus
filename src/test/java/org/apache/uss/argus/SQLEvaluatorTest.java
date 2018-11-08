package org.apache.uss.argus;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @ParameterizedTest
    @CsvSource({
            "SELECT * FROM Table WHERE name=1 OR name='a',",
            "SELECT * FROM Table WHERE 1/(name-1)>0 OR name=true,",
            "SELECT * FROM Table as t WHERE name=1 OR t.name='a',t",
            "SELECT * FROM Table WHERE name=1 OR name[1]=1,",
            "SELECT * FROM Table WHERE name=true and name='a',",
            "SELECT * FROM Table WHERE name.valid=true OR name.valid=1,",
            "SELECT * FROM Table WHERE name.valid=true OR name=true,",
            "SELECT * FROM Table WHERE name[1] IS NULL OR name.valid=true,",
            "SELECT * FROM Table as t WHERE t.name.valid IS NULL OR name[1]=true,t"
    })
    void testValidateBadSql(String sql, String alias) {
        var statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        assertThrows(TypeParadoxException.class,
                () -> SQLEvaluator.Companion.validateStatement(statement, "Table", alias));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM Table WHERE name=1 OR name<0.5",
            "SELECT * FROM Table WHERE name IS NULL OR name=TRUE",
            "SELECT * FROM Table WHERE name=true OR concat(name, 1)='1'",
            "SELECT * FROM Table WHERE concat(name, 1)='1' OR name[1]='1'",
            "SELECT * FROM Table WHERE name.valid IS NULL OR name.valid=true",
            "SELECT * FROM Table WHERE 1/(name-1)>0 OR name=1",
    })
    void testValidateGoodSql(String sql) {
        var statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        SQLEvaluator.Companion.validateStatement(statement, "Table", null);
    }
}
