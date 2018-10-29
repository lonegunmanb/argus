package org.apache.uss.argus;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class UnaryOpExprTest {

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
        assertEquals(expected, ((OperandExpr)visitor.getValue()).getOperand());
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
        assertEquals(new BigDecimal(expected), ((OperandExpr)visitor.getValue()).getOperand());
    }
}
