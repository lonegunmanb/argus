package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.util.StringUtils.isNumber
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.apache.uss.argus.EvaluatorVisitor
import kotlin.reflect.KClass
import com.google.gson.JsonObject as JObject

class JsonObject : EvalObject {

    private val json: String?

    object gson {
        val instance = Gson()
    }

    private val jsonElement: JsonElement

    constructor(json: String?, name: String, expr: SQLExpr) : super(name, expr) {
        this.json = json
        this.jsonElement = gson.instance.fromJson(json, JsonElement::class.java)
    }

    private constructor(jsonElement: JsonElement, name: String, expr: SQLExpr) : super(name, expr) {
        json = null
        this.jsonElement = jsonElement
    }

    override fun get(index: String, expr: SQLExpr): EvalObject {
        return when (jsonElement) {
            is JsonPrimitive -> PojoObject(EvaluatorVisitor.Nil, index, expr)
            is JObject -> {
                when (jsonElement.has(index)) {
                    true -> JsonObject(jsonElement.get(index), index, expr)
                    false -> PojoObject(EvaluatorVisitor.Nil, index, expr)
                }
            }
            else -> {
                throw UnsupportedOperationException()
            }
        }
    }

    override fun operand(clazz: KClass<*>): Any? {
        if (jsonElement.isJsonNull) {
            return EvaluatorVisitor.Nil
        }
        return when {
            clazz == Boolean::class -> jsonElement.asBoolean
            Number::class.java.isAssignableFrom(clazz.java) -> jsonElement.asBigDecimal
            clazz == String::class -> jsonElement.asString
            else -> throw UnsupportedOperationException("Don't support ${clazz.simpleName}")
        }
    }

    override fun isType(clazz: KClass<*>): Boolean {
        if (jsonElement.isJsonNull) {
            return true
        }
        val content = jsonElement.asString
        return when {
            clazz == Boolean::class -> isBoolean(content)
            Number::class.java.isAssignableFrom(clazz.java) -> isNumber(content)
            clazz == String::class -> true
            else -> throw UnsupportedOperationException("Don't support ${clazz.simpleName}")
        }
    }

    override fun isNil(): Boolean {
        return jsonElement.isJsonNull
    }

    private fun isBoolean(content: String?) =
            content == "true" || content == "false"

}