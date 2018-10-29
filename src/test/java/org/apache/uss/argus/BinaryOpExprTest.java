package org.apache.uss.argus;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinaryOpExprTest {
    @ParameterizedTest
    @CsvSource({"2>1, true"
            , "1>2, false"
            , "1>1, false"
            , "2.1>1, true"
            , "2>=1, true"
            , "1>=2, false"
            , "1>=1, true"
            , "2.1>=1, true"
            , "1<2, true"
            , "2<1, false"
            , "1<1, false"
            , "1.1<2, true"
            , "1<=2, true"
            , "2<=1, false"
            , "1<=1, true"
            , "1.1<=2, true"})
    void compareTest(String sql, boolean expected) {
        testBinaryOp(sql, expected);
    }

    private void testBinaryOp(String sql, Object expected) {
        var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
        var visitor = new EvaluatorVisitor();
        expr.accept(visitor);
        assertEquals(expected, ((OperandExpr) visitor.getValue()).getOperand());
    }
}
