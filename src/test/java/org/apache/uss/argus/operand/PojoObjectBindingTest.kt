package org.apache.uss.argus.operand

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement
import com.alibaba.druid.util.JdbcConstants
import org.apache.uss.argus.EvaluatorVisitor
import org.junit.jupiter.api.Assertions.*
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
        val expr = SQLUtils.toSQLExpr("p", JdbcConstants.POSTGRESQL)
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

    @ParameterizedTest
    @CsvSource("p.address.city, city1",
            "p.address.address, address1",
            "p.address.notexisted, null",
            "p.notexisted.city, null")
    fun propertyGetTest(sql: String, expected: String) {
        val address = Address("address1", "city1")
        val person = Person("name1", 10, address, true)
        val expr = SQLUtils.toSQLExpr(sql, JdbcConstants.POSTGRESQL)
        val visitor = EvaluatorVisitor(parameter(person))
        expr.accept(visitor)
        val r = visitor.value as Operand
        when (expected) {
            "null" -> assertTrue(r.isNil())
            else -> assertEquals(expected, r.operand(String::class))
        }
    }

    @ParameterizedTest
    @CsvSource("select * from person as p where p.name='name1', true",
            "select * from person as p where p.address.city='city1', true",
            "select * from person as p where p.age > 9 and p.address.address='address1', true",
            "select * from person as p where p.age > 10, false",
            "select * from person as p where p.notexist='notexist', null")
    fun pojoValidationTest(sql: String, expected: String) {
        val address = Address("address1", "city1")
        val person = Person("name1", 10, address, true)
        val statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL)[0]
        val expr = ((statement as SQLSelectStatement).select.query as SQLSelectQueryBlock).where!!
        val visitor = EvaluatorVisitor(parameter(person))
        expr.accept(visitor)
        val r = (visitor.value as Operand).operand(Any::class)
        when (expected) {
            "null" -> assertEquals(EvaluatorVisitor.Nil, r)
            "true" -> assertTrue(r as Boolean)
            "false" -> assertFalse(r as Boolean)
        }
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
