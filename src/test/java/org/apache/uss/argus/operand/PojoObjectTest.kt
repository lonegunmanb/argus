package org.apache.uss.argus.operand

import org.apache.uss.argus.EvaluatorVisitor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PojoObjectTest {
    @Test
    fun objectIsTypeTest() {
        val booleanParameter = pojoObject(true)
        assertTrue(booleanParameter.isType<Boolean>())
        assertFalse(booleanParameter.isType<String>())

        val decimalParameter = pojoObject(BigDecimal("123.456"))
        assertTrue(decimalParameter.isType<BigDecimal>())
        assertTrue(decimalParameter.isType<Number>())
        assertTrue(decimalParameter.isType<Any>())
        assertFalse(decimalParameter.isType<Boolean>())
    }

    @Test
    fun nilShouldCompatibleWithAllTypesTest() {
        val nullParameter = pojoObject(EvaluatorVisitor.Nil)
        assertTrue(nullParameter.isType<Boolean>())
        assertTrue(nullParameter.isType<BigDecimal>())
        assertTrue(nullParameter.isType<String>())
    }


    @Test
    @ValueSource()
    fun objectIsNilTest() {
        val p1 = pojoObject(1)
        assertFalse(p1.isNil())
        val p2 = pojoObject(true)
        assertFalse(p2.isNil())
        val p3 = pojoObject(EvaluatorVisitor.Nil)
        assertTrue(p3.isNil())
    }

    @Test
    fun objectToOperandTest() {
        val decimal = BigDecimal("123.456")
        val p1 = pojoObject(decimal)
        assertEquals(decimal, p1.operand(Number::class))
        assertEquals(decimal, p1.operand(BigDecimal::class))
        assertNull(p1.operand(Boolean::class))
    }

    @Test
    fun nullShouldReturnNilOperandTest() {
        val nullParameter = pojoObject(EvaluatorVisitor.Nil)
        assertEquals(EvaluatorVisitor.Nil, nullParameter.operand(Boolean::class))
        assertEquals(EvaluatorVisitor.Nil, nullParameter.operand(String::class))
        assertEquals(EvaluatorVisitor.Nil, nullParameter.operand(BigDecimal::class))
    }

    @Test
    fun accessNilObjectPropertyShouldReturnNilObjectTest() {
        val nullParameter = pojoObject(EvaluatorVisitor.Nil)
        val property = nullParameter["test", DummyExpr()]
        assertTrue(property.isNil())
    }

    @Test
    fun accessNotExistedPropertyShouldReturnNilObjectTest() {
        val person = Person("name", 10, null, true)
        val parameter = pojoObject(person)
        assertTrue(parameter["not existed", DummyExpr()].isNil())
    }

    @Test
    fun getPropertyFromObjectTest() {
        val expr = DummyExpr()
        val name = "Peter"
        val age = 20
        val person = Person(name, age, null, true)
        val p = pojoObject(person)
        val nameProperty = p["name", expr]
        assertEquals(name, nameProperty.getOperand<String>())
        assertEquals(expr, nameProperty.expr)
        val ageProperty = p["age", expr]
        assertEquals(age, ageProperty.getOperand<Int>())
        assertEquals(expr, ageProperty.expr)

        val nilProperty = p["notExisted", expr]
        assertEquals(EvaluatorVisitor.Nil, nilProperty.operand(Any::class))
        assertTrue(nilProperty.isNil())
    }

    @Test
    fun getNestedPropertyTest() {
        val expr = DummyExpr()
        val nestedExpr = DummyExpr()
        val exactlyAddress = "somewhere"
        val city = "over the rainbow"
        val address = Address(exactlyAddress, city)
        val person = Person("Peter", 20, address, true)

        val p = pojoObject(person)
        val addressNameProperty = p["address", expr]["address", nestedExpr]
        val cityProperty = p["address", expr]["city", nestedExpr]
        assertEquals(exactlyAddress, addressNameProperty.getOperand<String>())
        assertEquals(nestedExpr, addressNameProperty.expr)
        assertEquals(city, cityProperty.getOperand<String>())
        assertEquals(nestedExpr, cityProperty.expr)
    }

    private fun pojoObject(i: Any) = PojoObject(i, "test", DummyExpr())
}
