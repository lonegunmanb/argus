package org.apache.uss.argus.operand

import com.alibaba.druid.sql.SQLUtils
import org.apache.uss.argus.EvaluatorVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

internal class PojoObjectBindingTest {

    @ParameterizedTest
    @CsvSource("p+1=2, 1", "p>1, 2", "p*0=0, 10", "p=p, 1", "NOT(p<>p), 1", "NOT(p!=p), 1")
    fun numericBindingTest(sql: String, decimal: String) {
        val expr = SQLUtils.toSQLExpr(sql)
        val parameter = parameter(BigDecimal(decimal))
        val visitor = EvaluatorVisitor(parameter)
        expr.accept(visitor)

        assertTrue((getOperand(visitor) as Boolean?)!!)
    }

    @Test
    fun identifierBindingSingleValueTest() {
        val expr = SQLUtils.toSQLExpr("p")
        val boolParameter = parameter(true)
        val boolVisitor = EvaluatorVisitor(boolParameter)
        expr.accept(boolVisitor)
        assertTrue((getOperand(boolVisitor) as Boolean?)!!)
        val stringParameter = parameter("hello")
        val stringVisitor = EvaluatorVisitor(stringParameter)
        expr.accept(stringVisitor)
        assertEquals("hello", getOperand(stringVisitor))
        val decimal = BigDecimal("123.456")
        val decimalParameter = parameter(decimal)
        val decimalVisitor = EvaluatorVisitor(decimalParameter)
        expr.accept(decimalVisitor)
        assertEquals(decimal, getOperand(decimalVisitor))
    }


    private fun getOperand(visitor: EvaluatorVisitor): Any? {
        val v = visitor.value
        return when (v) {
            is Operand -> v.operand(Any::class)
            else -> throw NullPointerException()
        }
    }

    private fun parameter(parameter: Any): PojoObject {
        return PojoObject(parameter, "p", DummyExpr())
    }
}
