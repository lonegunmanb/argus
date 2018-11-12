package org.apache.uss.argus.operand

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import java.math.BigDecimal
import kotlin.test.assertEquals

class JsonObjectGetPropertiesTest {
    val gson = Gson()

    @Test
    fun simpleTest(){
        val latlong1 = BigDecimal("1.1")
        val latlong2 = BigDecimal("2.2")
        val address = Address("address1", "city1", arrayOf(latlong1, latlong2))
        val jsonObject = JsonObject(gson.toJson(address), "address", null, null)
        val properties = jsonObject.getProperties()!!
        assertEquals(3, properties.size)
        assertEquals("address1", properties.first { pair->pair.first == "address" }.second)
        assertEquals("city1", properties.first { pair->pair.first == "city"}.second)
        val array = properties.first { pair->pair.first == "latlong" }.second as Array<*>
        assertEquals(2, array.size)
        assertEquals(latlong1, array[0])
        assertEquals(latlong2, array[1])
    }

//    @Test
//    fun simpleTest2() {
//        data class SubClass(val subNumber:Int?, val subText:String?, val subBool:Boolean?)
//        data class Class1(val number:Int?, val text:String?, val bool:Boolean?, val subProperty:SubClass?)
//        val obj = Class1(1, "abc", true, SubClass(2, "dev", true))
//        //gson.
//        val json = gson.toJson(obj, Class1::class.java)
//    }
}