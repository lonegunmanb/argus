package org.apache.uss.argus.operand

import org.apache.uss.argus.EvaluatorVisitor
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class NativeObjectTest {
    @Test
    fun objectIsTypeTest() {
        val booleanParameter = nativeObject(true)
        assertTrue(booleanParameter.isType<Boolean>())
        assertFalse(booleanParameter.isType<String>())

        val decimalParameter = nativeObject(BigDecimal("123.456"))
        assertTrue(decimalParameter.isType<BigDecimal>())
        assertTrue(decimalParameter.isType<Number>())
        assertTrue(decimalParameter.isType<Any>())
        assertFalse(decimalParameter.isType<Boolean>())
    }

    @Test
    fun nilShouldCompatibleWithAllTypesTest() {
        val nullParameter = nativeObject(EvaluatorVisitor.Nil)
        assertTrue(nullParameter.isType<Boolean>())
        assertTrue(nullParameter.isType<BigDecimal>())
        assertTrue(nullParameter.isType<String>())
    }


    @Test
    @ValueSource()
    fun objectIsNilTest() {
        val p1 = nativeObject(1)
        assertFalse(p1.isNil())
        val p2 = nativeObject(true)
        assertFalse(p2.isNil())
        val p3 = nativeObject(EvaluatorVisitor.Nil)
        assertTrue(p3.isNil())
    }

    @Test
    fun objectToOperandTest() {
        val decimal = BigDecimal("123.456")
        val p1 = nativeObject(decimal)
        assertEquals(decimal, p1.operand(Number::class))
        assertEquals(decimal, p1.operand(BigDecimal::class))
        assertNull(p1.operand(Boolean::class))
    }

    @Test
    fun nullShouldReturnNilOperandTest() {
        val nullParameter = nativeObject(EvaluatorVisitor.Nil)
        assertEquals(EvaluatorVisitor.Nil, nullParameter.operand(Boolean::class))
        assertEquals(EvaluatorVisitor.Nil, nullParameter.operand(String::class))
        assertEquals(EvaluatorVisitor.Nil, nullParameter.operand(BigDecimal::class))
    }

    @Test
    fun accessNilObjectPropertyShouldReturnNilObjectTest() {
        val nullParameter = nativeObject(EvaluatorVisitor.Nil)
        val property = nullParameter["test", DummyExpr()]
        assertTrue(property.isNil())
    }

    @Test
    fun getPropertyFromObjectTest() {
        val expr = DummyExpr()
        val name = "Peter"
        val age = 20
        val person = Person(name, age, null)
        val p = nativeObject(person)
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
        val person = Person("Peter", 20, address)

        val p = nativeObject(person)
        val addressNameProperty = p["address", expr]["address", nestedExpr]
        val cityProperty = p["address", expr]["city", nestedExpr]
        assertEquals(exactlyAddress, addressNameProperty.getOperand<String>())
        assertEquals(nestedExpr, addressNameProperty.expr)
        assertEquals(city, cityProperty.getOperand<String>())
        assertEquals(nestedExpr, cityProperty.expr)
    }

    private data class Person(val name: String, val age: Int, val address: Address?)
    private data class Address(val address: String, val city: String)

    private fun nativeObject(i: Any) = NativeObject(i, "test", DummyExpr())
}
