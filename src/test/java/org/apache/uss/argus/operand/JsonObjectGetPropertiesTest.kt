package org.apache.uss.argus.operand

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class JsonObjectGetPropertiesTest {

    private operator fun List<Pair<String, Any?>>.get(key:String):Any? {
        return this.first { pair->pair.first == key }.second
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun simpleTest() {
        val obj = object{
            val number = 1
            val text = "abc"
            val bool = true
            val subProperty = object{
                val subNumber=2
                val subText = "def"
                val subBool = false
            }
            val nullString:String? = null
        }
        val json = JSON.toJSONString(obj)
        val jsonObject = JsonObject(json, "test", null, null)
        val properties = jsonObject.getProperties()!!
        assertEquals(4, properties.size)
        assertEquals(BigDecimal(obj.number), properties["number"])
        assertEquals(obj.text, properties["text"])
        assertEquals(obj.bool, properties["bool"])
        val subProperty = properties["subProperty"] as List<Pair<String, Any?>>
        assertEquals(BigDecimal(obj.subProperty.subNumber), subProperty["subNumber"])
        assertEquals(obj.subProperty.subText, subProperty["subText"])
        assertEquals(obj.subProperty.subBool, subProperty["subBool"])
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun arrayTest() {
        val arrayItem = object {
            val name = "abc"
        }
        val obj = object {
            val names = arrayOf(arrayItem, arrayItem)
        }
        val json = JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect)
        val jsonObject = JsonObject(json, "test", null, null)
        val properties = jsonObject.getProperties()!!
        val arrayPair = properties["names"] as Array<*>
        assertEquals(obj.names.size, arrayPair.size)
        assertEquals(arrayItem.name, (arrayPair[0] as List<Pair<String, Any?>>)["name"])
        assertEquals(arrayItem.name, (arrayPair[1] as List<Pair<String, Any?>>)["name"])
    }
}