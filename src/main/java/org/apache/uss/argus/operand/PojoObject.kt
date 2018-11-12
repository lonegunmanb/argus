package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import jdk.jshell.spi.ExecutionControl
import org.apache.uss.argus.visitor.EvaluatorVisitor
import java.lang.reflect.Array
import java.lang.reflect.Method
import kotlin.reflect.KClass

class PojoObject(private val `object`: Any, objectName: String, alias: String?, expr: SQLExpr?) : EvalObject(objectName, alias, expr) {
    override fun getProperties(): List<Pair<String, Any?>>? {
        throw NotImplementedError()
    }

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
        if (isNil()) {
            return PojoObject(EvaluatorVisitor.Nil, property, expr)
        }
        val getMethodName = if (property.startsWith("is")) property else "get${property.capitalize()}"
        val method: Method = try {
            `object`.javaClass.getMethod(getMethodName)
        } catch (e: NoSuchMethodException) {
            return PojoObject(EvaluatorVisitor.Nil, property, expr)
        }
        val value = method.invoke(`object`) ?: EvaluatorVisitor.Nil
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
