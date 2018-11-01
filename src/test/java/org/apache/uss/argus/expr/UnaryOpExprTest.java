package org.apache.uss.argus.expr;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.uss.argus.EvaluatorVisitor;
import org.apache.uss.argus.TypeMismatchException;
import org.apache.uss.argus.operand.EvaluatedOperand;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SuppressWarnings("ConstantConditions")
class UnaryOpExprTest {

    @ParameterizedTest
    @CsvSource({
            "NOT TRUE, false",
            "NOT FALSE, true",
            "!TRUE, false",
            "!FALSE, true"
    })
    void flipBooleanTest(String sql, boolean expected){
        var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
        var visitor = new EvaluatorVisitor();
        expr.accept(visitor);
        assertEquals(expected, ((EvaluatedOperand) visitor.getValue()).getJavaOperand());
    }

    @ParameterizedTest
    @CsvSource({
            "-(1+1), -2",
            "+(1+1), 2",
            "-(-1), 1",
            "+(1+1), 2"
    })
    void negative_plus_test(String sql, String expected){
        var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
        var visitor = new EvaluatorVisitor();
        expr.accept(visitor);
        assertEquals(new BigDecimal(expected), ((EvaluatedOperand) visitor.getValue()).getJavaOperand());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-(1=1)",
            "NOT(1+1)"
    })
    void invalid_negative_test(String sql) {
        assertThrows(TypeMismatchException.class, () -> {
            var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
            var visitor = new EvaluatorVisitor();
            expr.accept(visitor);
        });
    }
}
