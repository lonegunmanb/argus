package org.apache.uss.argus.expr;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.uss.argus.operand.EvaluatedOperand;
import org.apache.uss.argus.visitor.EvaluatorVisitor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ConstantConditions")
class MethodCallExprTest {

    @ParameterizedTest
    @CsvSource(value = {
            "concat('a', 'b', 'c')| abc",
            "concat('a', 1, null, 3.6, true, 'b')| a13.6tb",
    }, delimiter = '|')
    void concatTest(String sql, String expected) {
        var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
        var visitor = new EvaluatorVisitor();
        expr.accept(visitor);
        assertEquals(expected, ((EvaluatedOperand) visitor.getValue()).getJavaOperand());
    }
}
