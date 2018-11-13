package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import org.apache.uss.argus.visitor.EvaluatorVisitor
import org.springframework.util.NumberUtils
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.Array as KArray

class PojoObject(private val `object`: Any, objectName: String, alias: String?, expr: SQLExpr?) : EvalObject(objectName, alias, expr) {
    constructor(`object`: Any, objectName: String, expr: SQLExpr?) : this(`object`, objectName, null, expr)

    override fun operand(clazz: KClass<*>): Any? {
        return when {
            isType(clazz) -> `object`
            else -> null
        }
    }

    override fun isType(clazz: KClass<*>): Boolean {
        return when (`object`) {
            EvaluatorVisitor.Nil -> true
            clazz.java.isArray -> `object`.javaClass.isArray
            else -> clazz.java.isAssignableFrom(`object`.javaClass)
        }
    }

    override fun isNil(): Boolean {
        return `object` === EvaluatorVisitor.Nil
    }

    override fun get(property: String, expr: SQLExpr): EvalObject {
        return PojoObject(get(property), property, expr)
    }

    private fun get(property: String): Any {
        return get(`object`, property)
    }

    private fun get(obj:Any, property: String):Any {
        if (obj === EvaluatorVisitor.Nil) {
            return EvaluatorVisitor.Nil
        }

        val getMethodName = if (property.startsWith("is")) property else "get${property.capitalize()}"
        val method: Method = try {
            obj.javaClass.getMethod(getMethodName)
        } catch (e: NoSuchMethodException) {
            return EvaluatorVisitor.Nil
        }
        return method.invoke(obj) ?: EvaluatorVisitor.Nil
    }

    override fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject {
        val name = "$objectName[$sqlArrayIndex]"
        return PojoObject(get(sqlArrayIndex), name, expr)
    }

    override fun getArray(): KArray<*> {
        val array = `object` as? KArray<*> ?: throw TypeCastException()
        return array.map { item-> convertObject(item) }.toTypedArray()
    }

    private fun get(sqlArrayIndex: Int): Any {
        return try{
            Array.get(`object`, sqlArrayIndex-1) ?: EvaluatorVisitor.Nil
        }catch (e: ArrayIndexOutOfBoundsException) {
            EvaluatorVisitor.Nil
        }
    }

    override fun getProperties(): Properties? {
        return getProperties(`object`)
    }

    private fun getProperties(obj: Any): Properties? {
        if(obj.javaClass.isPrimitive){
            return null
        }
        return obj::class.memberProperties.map { property-> Pair(property.name, getProperty(obj, property.name)) }
    }

    private fun getProperty(obj:Any, propertyName: String): Any? {
        val value = get(obj, propertyName)
        return convertObject(value)
    }

    private fun convertObject(value: Any?): Any? {
        return when (value) {
            null -> null
            EvaluatorVisitor.Nil -> null
            is BigDecimal -> value
            is Number -> NumberUtils.convertNumberToTargetClass(value, BigDecimal::class.java)
            is String -> value
            is Boolean -> value
            is KArray<*> -> getArrayProperty(value)
            else -> getProperties(value)
        }
    }

    private fun getArrayProperty(array: kotlin.Array<*>): kotlin.Array<*>{
        return array.map { value -> this.convertObject(value) }.toTypedArray()
    }
}
