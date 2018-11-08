package org.apache.uss.argus.operand

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement
import com.alibaba.druid.util.JdbcConstants
import com.google.gson.Gson
import org.apache.uss.argus.EvaluatorVisitor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.assertEquals

class JsonObjectTest {

    private var json: String
    private val jsonObject: JsonObject
    private val address: Address = Address("address1", "city1", arrayOf(BigDecimal("1.1"), BigDecimal("2.2")))
    private val person: Person = Person("Peter", 10, address, true)

    init {
        val gson = Gson()
        json = gson.toJson(person)
        jsonObject = JsonObject(json, "person", "p", DummyExpr())
    }

    @Test
    fun jsonObjectIsNilTest() {
        assertTrue(jsonObject["notexisted", DummyExpr()].isNil())
        assertFalse(jsonObject["name", DummyExpr()].isNil())
    }

    @Test
    fun nilShouldCompatibleWithAllTypesTest() {
        val nullParameter = jsonObject["notexisted", DummyExpr()]
        assertTrue(nullParameter.isType<Boolean>())
        assertTrue(nullParameter.isType<BigDecimal>())
        assertTrue(nullParameter.isType<String>())
    }

    @Test
    fun isTypeTest() {
        assertType<String>("name", true)
        assertType<Boolean>("name", false)
        assertType<Number>("name", false)
        assertType<Number>("age", true)
        assertType<Boolean>("age", false)
        assertType<String>("age", true)
        assertType<Boolean>("isMale", true)
        assertType<Number>("isMale", false)
        assertType<String>("isMale", true)
    }

    @Test
    fun getPropertyTest() {
        assertEquals(person.name, jsonObject["name", DummyExpr()].getOperand<String>())
        assertEquals(BigDecimal(person.age), jsonObject["age", DummyExpr()].getOperand<Number>())
        assertEquals(person.isMale, jsonObject["isMale", DummyExpr()].getOperand<Boolean>())
    }

    @Test
    fun getNestedPropertyTest() {
        assertEquals(address.city, jsonObject["address", DummyExpr()]["city", DummyExpr()].getOperand<String>())
        assertEquals(address.address, jsonObject["address", DummyExpr()]["address", DummyExpr()].getOperand<String>())
    }

    @Test
    fun getArrayItemTest() {
        val arrayProperty = jsonObject["address", DummyExpr()]["latlong", DummyExpr()]
        assertEquals(BigDecimal("1.1"), arrayProperty[1, DummyExpr()].getOperand<Number>())
        assertEquals(BigDecimal("2.2"), arrayProperty[2, DummyExpr()].getOperand<Number>())
    }

    @ParameterizedTest
    @CsvSource("select * from person as p where p.name='Peter', true",
            "select * from person as p where p.address.city='city1', true",
            "select * from person as p where p.age > 9 and p.address.address='address1', true",
            "select * from person as p where p.age > 10, false",
            "select * from person as p where p.age + 1 > 10, true",
            "select * from person as p where p.notexist='notexist', null",
            "select * from person where address.city='city1', true",
            "select * from person where address.city='city1' and name='Peter', true",
            "select * from p where address.city='city1' and p.name='Peter', true",
            "select * from person where person.address.city='city1', true",
            "select * from person as p where address.city='city1', true")
    fun jsonObjectValidationTest(sql: String, expected: String) {
        val statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL)[0]
        val expr = ((statement as SQLSelectStatement).select.query as SQLSelectQueryBlock).where!!
        val visitor = EvaluatorVisitor(jsonObject)
        expr.accept(visitor)
        val r = (visitor.value as Operand).operand(Any::class)
        when (expected) {
            "null" -> Assertions.assertEquals(EvaluatorVisitor.Nil, r)
            "true" -> assertTrue(r as Boolean)
            "false" -> assertFalse(r as Boolean)
        }
    }

    @Test
    fun jsonObjectArrayAccessTest() {
        val sql = "select * from person where address.latlong[1]='1.1'"
        val statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL)[0]
        val expr = ((statement as SQLSelectStatement).select.query as SQLSelectQueryBlock).where!!
        val visitor = EvaluatorVisitor(jsonObject)
        expr.accept(visitor)
        assertTrue((visitor.value as Operand).getOperand<Boolean>()!!)
    }

    private inline fun <reified T> assertType(index: String, expected: Boolean) {
        assertEquals(expected, jsonObject[index, DummyExpr()].isType<T>())
    }
}