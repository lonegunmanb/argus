package org.apache.uss.argus.serializer

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import org.apache.uss.argus.UnsupportedFeatureException
import com.google.gson.JsonObject as JObject
import org.apache.uss.argus.operand.EvalObject
import org.apache.uss.argus.operand.Properties

class JsonSerializer : OperandSerializer {

    val jsonInstance = JObject()

    override fun writeNumber(number: Number, name: String) {
        jsonInstance.writeNumber(number, name)
    }

    override fun writeBoolean(bool: Boolean, name: String) {
        jsonInstance.writeBoolean(bool, name)
    }

    override fun writeString(text: String, name: String) {
        jsonInstance.writeString(text, name)
    }

    override fun writeArray(array: Array<*>, name: String) {
        jsonInstance.writeArray(array, name)
    }

    override fun writeObject(`object`: EvalObject, name: String) {
        jsonInstance.writeObject(`object`.getProperties(), name)
    }

    private fun JObject.writeObject(properties:Properties?, name: String?) {
        if(properties!=null) {
            val objectJson = JObject()
            objectJson.writeProperties(properties)
            this.add(name, objectJson)
        }
    }

    private fun JObject.writeProperties(properties:Properties) {
        properties.forEach { pair ->
            run {
                val propertyName = pair.first
                val property = pair.second
                this.write(property, propertyName)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun JObject.write(property:Any?, propertyName:String){
        when(property){
            null -> {}
            is Number -> this.writeNumber(property, propertyName)
            is String -> this.writeString(property, propertyName)
            is Boolean -> this.writeBoolean(property, propertyName)
            is Array<*> -> this.writeArray(property, propertyName)
            else -> this.writeObject(property as? Properties ?: throw UnknownError(), propertyName)
        }
    }

    private fun JObject.writeArray(array: Array<*>, name: String) {
        this.add(name, array.toJsonArray())
    }

    @Suppress("UNCHECKED_CAST")
    private fun Array<*>.toJsonArray(): JsonArray{
        val jsonArray = JsonArray(this.size)
        this.forEach { item ->
            run {
                when (item) {
                    null -> jsonArray.add(JsonNull.INSTANCE)
                    is Number -> jsonArray.add(item)
                    is String -> jsonArray.add(item)
                    is Boolean -> jsonArray.add(item)
                    is Array<*> -> jsonArray.add(item.toJsonArray())
                    else -> {
                        val jsonObject = JObject()
                        jsonObject.writeProperties(item as? Properties ?: throw UnsupportedFeatureException(item.javaClass.name))
                        jsonArray.add(jsonObject)
                    }
                }
            }
        }
        return jsonArray
    }

    private fun JObject.writeNumber(number: Number, name: String){
        this.addProperty(name, number)
    }

    private fun JObject.writeBoolean(bool: Boolean, name:String) {
        this.addProperty(name, bool)
    }

    private fun JObject.writeString(text: String, name:String) {
        this.addProperty(name, text)
    }
}