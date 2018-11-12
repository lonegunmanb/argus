package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.util.StringUtils.isNumber
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.apache.uss.argus.TypeMismatchException
import org.apache.uss.argus.UnsupportedFeatureException
import org.apache.uss.argus.visitor.EvaluatorVisitor
import java.util.AbstractMap
import kotlin.reflect.KClass
import com.google.gson.JsonObject as JObject

class JsonObject : EvalObject {
    private val json: String?

    private object gson {

        val instance = Gson()

    }

    private val jsonElement: JsonElement

    constructor(json: String?, name: String, alias: String?, expr: SQLExpr?) : super(name, alias, expr) {
        this.json = json
        this.jsonElement = gson.instance.fromJson(json, JsonElement::class.java)
    }
    private constructor(jsonElement: JsonElement, name: String, expr: SQLExpr?) : super(name, null, expr) {
        json = null
        this.jsonElement = jsonElement
    }

    override fun getProperties(): List<Pair<String, Any?>>? {
        return getProperties(this.jsonElement)
    }

    private fun getProperties(jsonElement: JsonElement):List<Pair<String, Any?>>? {
        if(jsonElement.isJsonNull){
            return null
        }
        return when (jsonElement) {
            is JsonPrimitive -> ArrayList()
            is JObject -> jsonElement.entrySet().map { entry-> Pair<String, Any?>(entry.key, getObj(entry.value)) }
            else -> throw UnsupportedFeatureException(jsonElement.asString)
        }
    }

    private fun getObj(jsonElement: JsonElement): Any?{
        if(jsonElement.isJsonNull){
            return null
        }
        return when (jsonElement) {
            is JsonPrimitive-> when {
                jsonElement.isBoolean -> jsonElement.asBoolean
                jsonElement.isNumber -> jsonElement.asBigDecimal
                jsonElement.isString -> jsonElement.asString
                else -> throw UnsupportedFeatureException(jsonElement.asString)
            }
            is JObject -> getProperties(jsonElement)
            is JsonArray -> getArrayObj(jsonElement)
            else -> throw UnsupportedFeatureException(jsonElement.asString)
        }
    }

    private fun getArrayObj(jsonArray: JsonArray): Array<Any?>{
        return jsonArray.map { jsonElement -> getObj(jsonElement) }.toTypedArray()
    }

    override fun get(property: String, expr: SQLExpr): EvalObject {
        return when (jsonElement) {
            is JsonPrimitive -> PojoObject(EvaluatorVisitor.Nil, property, expr)
            is JObject -> {
                when (jsonElement.has(property)) {
                    true -> JsonObject(jsonElement.get(property), property, expr)
                    false -> PojoObject(EvaluatorVisitor.Nil, property, expr)
                }
            }
            else -> {
                throw UnsupportedOperationException(jsonElement.toString())
            }
        }
    }

    override fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject {
        return when (jsonElement) {
            is JsonArray -> JsonObject(jsonElement[sqlArrayIndex - 1], "$objectName[$sqlArrayIndex]", expr)
            else -> throw TypeMismatchException("Required array, got: ${jsonElement.asString}")
        }
    }

    override fun operand(clazz: KClass<*>): Any? {
        if (jsonElement.isJsonNull) {
            return EvaluatorVisitor.Nil
        }
        return when {
            clazz == Boolean::class -> jsonElement.asBoolean
            Number::class.java.isAssignableFrom(clazz.java) -> if (isType(Number::class)) jsonElement.asBigDecimal else null
            clazz == String::class -> jsonElement.asString
            else -> throw UnsupportedOperationException("Don't support ${clazz.simpleName}")
        }
    }

    override fun isType(clazz: KClass<*>): Boolean {
        if (jsonElement.isJsonNull) {
            return true
        }
        return when {
            clazz == Boolean::class -> isBoolean(jsonElement.asString)
            Number::class.java.isAssignableFrom(clazz.java) -> isNumber(jsonElement.asString)
            clazz == String::class -> true
            clazz.java.isArray -> jsonElement.isJsonArray
            else -> throw UnsupportedOperationException("Don't support ${clazz.simpleName}")
        }
    }

    override fun isNil(): Boolean {
        return jsonElement.isJsonNull
    }

    private fun isBoolean(content: String?) =
            content == "true" || content == "false"

}
