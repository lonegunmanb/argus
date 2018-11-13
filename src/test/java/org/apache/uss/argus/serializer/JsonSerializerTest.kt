package org.apache.uss.argus.serializer

import com.alibaba.fastjson.JSON
import org.apache.uss.argus.operand.PojoObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import com.google.gson.JsonObject as JObject
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JsonSerializerTest {

    var sut: JsonSerializer = JsonSerializer()
    @BeforeEach
    fun setup() {
        sut = JsonSerializer()
    }

    @Test
    fun numberTest() {
        sut.writeNumber(123, "column1")
        assertEquals(123.toBigDecimal(), sut.jsonInstance["column1"].asBigDecimal)
    }

    @Test
    fun stringTest() {
        sut.writeString("abc", "column1")
        assertEquals("abc", sut.jsonInstance["column1"].asString)
    }

    @Test
    fun booleanTest() {
        sut.writeBoolean(true, "column1")
        assertEquals(true, sut.jsonInstance["column1"].asBoolean)
    }

    @Test
    fun valueArrayTest() {
        sut.writeArray(arrayOf(1, 2), "column1")
        val property = sut.jsonInstance["column1"]
        assertTrue(property.isJsonArray)
        assertEquals(2, property.asJsonArray.size())
        assertEquals(1.toBigDecimal(), property.asJsonArray[0].asBigDecimal)
        assertEquals(2.toBigDecimal(), property.asJsonArray[1].asBigDecimal)
    }

    @Test
    fun simpleObjectTest() {
        val plainObject = object {
            val number = 123
            val text = "abc"
            val bool = true
        }
        val input = PojoObject(plainObject, "test", null)
        sut.writeObject(input, "column1")
        val json = sut.jsonInstance
        val obj = json["column1"] as JObject
        assertEquals(plainObject.number.toBigDecimal(), obj["number"].asBigDecimal)
        assertEquals(plainObject.text, obj["text"].asString)
        assertEquals(plainObject.bool, obj["bool"].asBoolean)
    }

    @Test
    fun objectArrayTest() {
        data class test(val name: String)

        val array = arrayOf(test("abc"), test("def"))
        val pojoObject = PojoObject(array, "test", null)
        sut.writeArray(pojoObject.getArray(), "column1")
        val property = sut.jsonInstance["column1"]
        assertTrue(property.isJsonArray)
        assertEquals(array.size, property.asJsonArray.size())
        assertEquals(test("abc"), JSON.parseObject(property.asJsonArray[0].toString(), test::class.java))
        assertEquals(test("def"), JSON.parseObject(property.asJsonArray[1].toString(), test::class.java))
    }

    @Test
    fun nestObjectTest() {
        data class propertyClass(val name: String)
        data class test(val id: Int, val isValid:Boolean, val property: propertyClass)

        val obj = test(1, true, propertyClass("abc"))
        sut.writeObject(PojoObject(obj, "test", null), "column1")
        val json = sut.jsonInstance["column1"].toString()
        val obj2 = JSON.parseObject(json, test::class.java)
        assertEquals(obj.property, obj2.property)
        assertEquals(obj, obj2)
    }

    @Suppress("ArrayInDataClass")
    @Test
    fun objectWithArrayPropertyTest() {
        data class test(val array: Array<Int>)

        val obj = test(arrayOf(1, 2, 3))
        sut.writeObject(PojoObject(obj, "test", null), "column1")
        val json = sut.jsonInstance["column1"].toString()
        val obj2 = JSON.parseObject(json, test::class.java)
        assertArrayEquals(obj.array, obj2.array)
    }

    @Suppress("ArrayInDataClass")
    @Test
    fun nestArrayTest() {
        data class test(val array: Array<Array<Int>>)

        val obj = test(arrayOf(arrayOf(1, 1), arrayOf(2, 2)))
        sut.writeObject(PojoObject(obj, "test", null), "column1")
        val json = sut.jsonInstance["column1"].toString()
        val obj2 = JSON.parseObject(json, test::class.java)
        assertEquals(2, obj2.array.size)
        assertArrayEquals(obj.array[0], obj2.array[0])
        assertArrayEquals(obj.array[1], obj2.array[1])
    }

    @Test
    fun writeNullTest() {
        sut.writeNull("test")
        val json = sut.jsonInstance.toString()
        assertEquals("""{"test":null}""", json)
    }
}