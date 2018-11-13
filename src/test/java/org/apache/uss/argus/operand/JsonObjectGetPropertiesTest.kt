package org.apache.uss.argus.operand

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class JsonObjectGetPropertiesTest : GetPropertiesTestBase(){
    override fun getArray(array: Array<*>): Array<*> {
        val json = JSON.toJSONString(array)
        val jsonObject = JsonObject(json, "test", null, null)
        return jsonObject.getArray()
    }

    override fun getProperties(obj: Any): Properties {
        val json = JSON.toJSONString(obj)
        val jsonObject = JsonObject(json, "test", null, null)
        return jsonObject.getProperties()!!
    }
}