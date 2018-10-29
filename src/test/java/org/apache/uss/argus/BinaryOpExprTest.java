package org.apache.uss.argus;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            , "1.1<=2, true"
            , "1 IS NULL, false"
            , "NULL IS NULL, true"
            , "1 IS NOT NULL, true"
            , "NULL IS NOT NULL, false"
    })
    void compareTest(String sql, boolean expected) {
        testBinaryOp(sql, expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 IS 1"
            , "1 IS NOT 1"
            , "NULL IS 1"
            , "NULL IS NOT 1"})
    void nonNullInIsOrIsNot(String sql) {
        assertThrows(TypeMismatchException.class, () -> {
            var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
            var visitor = new EvaluatorVisitor();
            expr.accept(visitor);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"1>NULL"
            , "1<NULL"
            , "1=NULL"
            , "1>=NULL"
            , "1<=NULL"
            , "1=NULL"
            , "NULL=NULL"})
    void compareNullTest(String sql) {
        testBinaryOp(sql, EvaluatorVisitor.Nil.INSTANCE);
    }

    @ParameterizedTest
    @CsvSource({"1=1, true"
            , "'a'='a', true"
            , "true=true, true"
            , "'a'='b', false"
            , "1=2, false"
            , "true=false, false"})
    void equalTest(String sql, boolean expected) {
        testBinaryOp(sql, expected);
    }

    @ParameterizedTest
    @CsvSource(value = {"1+1| 2"
            , "1-1| 0"
            , "2*3| 6"
            , "4/2| 2"
            , "1.1+1.1| 2.2"
            , "2.2-1.1| 1.1"
            , "1.1*1.1| 1.21"
            , "1.21/1.1| 1.1"
            , "1+1.1| 2.1"
            , "1.1-1| 0.1"
            , "1.1*2| 2.2"
            , "2.2/2| 1.1"
            , "5 mod 3| 2"
            , "5.1 mod 3.1|2"
            , "5/2|2.5"
            , "5.0 / 2|2.5"
            , "5 / 2.0|2.5"
    }, delimiter = '|')
    void numericCalculateTest(String sql, String expected) {
        testBinaryOp(sql, new BigDecimal(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "5/0",
            "5.0/0",
            "5/0.0"
    })
    void dividByZeroTest(String sql) {
        assertThrows(ArithmeticException.class, () -> {
            var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
            var visitor = new EvaluatorVisitor();
            expr.accept(visitor);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1+'a'",
            "1+true",
            "'a'+true",
            "'a'+'b'"
    })
    void numericCalculateTypeMismatchTest(String sql) {
        assertThrows(TypeMismatchException.class, () -> {
            var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
            var visitor = new EvaluatorVisitor();
            expr.accept(visitor);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1+NULL",
            "'a'+NULL",
            "NULL + 1",
            "true + NULL",
            "NULL + NULL"
    })
    void numericCalculate_Encounter_Null(String sql) {
        testBinaryOp(sql, EvaluatorVisitor.Nil.INSTANCE);
    }

    @ParameterizedTest
    @CsvSource({
            "true, true",
            "false, false",
            "true and true, true",
            "true and false, false",
            "true or true, true",
            "true or false, true",
            "true xor true, false",
            "true xor false, true",
            "false xor true, true",
            "false xor false, false"
    })
    void booleanTest(String sql, boolean expected) {
        testBinaryOp(sql, expected);
    }

    @ParameterizedTest
    @CsvSource({
            "1+1, 2",
            "(1+2)*3, 9",
            "1+2*3, 7",
            "(1+2*3)/2+1, 4.5"
    })
    void compositNumericTest(String sql, String expected) {
        testBinaryOp(sql, new BigDecimal(expected));
    }

    private void testBinaryOp(String sql, Object expected) {
        var expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL);
        var visitor = new EvaluatorVisitor();
        expr.accept(visitor);
        assertEquals(expected, ((OperandExpr) visitor.getValue()).getOperand());
    }
}
