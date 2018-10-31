package org.apache.uss.argus;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ConstantConditions")
class LiteralExprTest {
    @ParameterizedTest
    @ValueSource(strings = {"1.1", /*beyond Long.MAX_VALUE*/"19223372036854775807.1"})
    void numericLiteralTest(String decimal) {
        testLiteral(decimal, new BigDecimal(decimal));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "-1", /*beyond Long.MAX_VALUE*/"19223372036854775807"})
    void integerLiteralTest(String integer) {
        testLiteral(integer, new BigDecimal(integer));
    }

    @Test
    void stringLiteralTest() {
        testLiteral("'abc'", "abc");
    }

    @ParameterizedTest
    @CsvSource({"TRUE, true", "FALSE, false", "true, true", "false, false"})
    void booleanLiteralTest(String sql, boolean expected) {
        testLiteral(sql, expected);
    }

    @Test
    void nullLiteralTest() {
        testLiteral("NULL", EvaluatorVisitor.Nil.INSTANCE);
        testLiteral("null", EvaluatorVisitor.Nil.INSTANCE);
    }

    private void testLiteral(String sql, Object expected) {
        var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
        var visitor = new EvaluatorVisitor();
        expr.accept(visitor);
        assertEquals(expected, ((OperandExpr) visitor.getValue()).getJavaOperand());
    }
}
