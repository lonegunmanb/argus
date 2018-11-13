package org.apache.uss.argus.operand

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

abstract class GetPropertiesTestBase{
    protected operator fun List<Pair<String, Any?>>.get(key:String):Any? {
        return this.first { pair->pair.first == key }.second
    }

    @Test
    fun simplePlainObjectTest(){
        val obj = object {
            val number = 123
            val bool = true
            val text = "abc"
        }
        val properties = getProperties(obj)
        Assertions.assertEquals(3, properties.size)
        Assertions.assertEquals(obj.number.toBigDecimal(), properties["number"])
        Assertions.assertEquals(obj.bool, properties["bool"])
        Assertions.assertEquals(obj.text, properties["text"])
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun objectPropertyTest() {
        val obj = object {
            val property = object {
                val name = "abc"
            }
        }
        val properties = getProperties(obj)
        Assertions.assertEquals(1, properties.size)
        val property = properties["property"] as Properties
        Assertions.assertEquals(1, property.size)
        Assertions.assertEquals(obj.property.name, property["name"])
    }

    @Test
    fun objectArrayPropertyTest() {
        val obj = object {
            val array = arrayOf(1, 2)
        }
        val properties = getProperties(obj)
        val arrayProperty = properties["array"]
        Assertions.assertTrue(arrayProperty is Array<*>)
        val array = arrayProperty as Array<*>
        Assertions.assertEquals(obj.array.size, arrayProperty.size)
        Assertions.assertArrayEquals(obj.array.toBigDecimal(), array)
    }

    @Test
    fun nestArrayPropertyTest() {
        val obj = object {
            val array = arrayOf(arrayOf(1, 1), arrayOf(2, 2))
        }
        val properties = getProperties(obj)
        val arrayProperty = properties["array"]
        val array = arrayProperty as Array<*>
        Assertions.assertArrayEquals(obj.array[0].toBigDecimal(), array[0] as Array<*>)
        Assertions.assertArrayEquals(obj.array[1].toBigDecimal(), array[1] as Array<*>)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun arrayOfObjectPropertyTest() {
        val obj = object {
            val array = arrayOf(object {
                val name = "abc"
            })
        }
        val properties = getProperties(obj)
        val arrayProperty = properties["array"] as Array<*>
        Assertions.assertTrue(arrayProperty[0] is List<*>)
        Assertions.assertEquals(obj.array[0].name, (arrayProperty[0] as List<Pair<String, Any?>>)["name"])
    }

    @Test
    fun arrayTest() {
        val array = arrayOf(1, 2)
        val convertedArray = getArray(array)
        Assertions.assertArrayEquals(array.toBigDecimal(), convertedArray)
    }

    protected abstract fun getProperties(obj: Any): Properties
    protected abstract fun getArray(array: Array<*>): Array<*>

    private fun Array<Int>.toBigDecimal(): Array<BigDecimal>{
        return this.map { i->i.toBigDecimal() }.toTypedArray()
    }
}
