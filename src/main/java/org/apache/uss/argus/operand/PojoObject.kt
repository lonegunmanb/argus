package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import org.apache.uss.argus.EvaluatorVisitor
import java.lang.reflect.Array
import java.lang.reflect.Method
import kotlin.reflect.KClass

class PojoObject(private val `object`: Any, objectName: String, expr: SQLExpr) : EvalObject(objectName, expr) {
    override fun operand(clazz: KClass<*>): Any? {
        return when {
            isType(clazz) -> `object`
            else -> null
        }
    }

    override fun isType(clazz: KClass<*>): Boolean {
        return when (`object`) {
            EvaluatorVisitor.Nil -> true
            clazz == Array::class -> `object`.javaClass.isArray
            else -> clazz.java.isAssignableFrom(`object`.javaClass)
        }
    }

    override fun isNil(): Boolean {
        return `object` === EvaluatorVisitor.Nil
    }

    override fun get(property: String, expr: SQLExpr): EvalObject {
        if (isNil()) {
            return PojoObject(EvaluatorVisitor.Nil, property, expr)
        }
        val getMethodName = "get${property.capitalize()}"
        val method: Method = try {
            `object`.javaClass.getMethod(getMethodName)
        } catch (e: NoSuchMethodException) {
            return PojoObject(EvaluatorVisitor.Nil, property, expr)
        }
        val value = method.invoke(`object`)
        return PojoObject(value, property, expr)
    }

    override fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject {
        val name = "$objectName[$sqlArrayIndex]"
        return try {
            PojoObject(Array.get(`object`, /*in pg, array sqlArrayIndex start at 1*/sqlArrayIndex - 1), name, expr)
        } catch (e: ArrayIndexOutOfBoundsException) {
            PojoObject(EvaluatorVisitor.Nil, name, expr)
        }
    }
}
