package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import org.apache.uss.argus.EvaluatorVisitor
import java.lang.reflect.Method
import kotlin.reflect.KClass

class PojoObject(private val `object`: Any, parameterName: String, expr: SQLExpr) : EvalObject(parameterName, expr) {
    override fun operand(clazz: KClass<*>): Any? {
        return when {
            isType(clazz) -> `object`
            else -> null
        }
    }

    override fun isType(clazz: KClass<*>): Boolean {
        return when (`object`) {
            EvaluatorVisitor.Nil -> true
            else -> clazz.java.isAssignableFrom(`object`.javaClass)
        }
    }

    override fun isNil(): Boolean {
        return `object` == EvaluatorVisitor.Nil
    }

    override fun get(index: String, expr: SQLExpr): EvalObject {
        if (isNil()) {
            return PojoObject(EvaluatorVisitor.Nil, index, expr)
        }
        val getMethodName = "get${index.capitalize()}"
        val method: Method? = try {
            `object`.javaClass.getMethod(getMethodName)
        } catch (e: NoSuchMethodException) {
            return PojoObject(EvaluatorVisitor.Nil, index, expr)
        }
        val value = when (method) {
            null -> EvaluatorVisitor.Nil
            else -> {
                method.invoke(`object`)
            }
        }
        return PojoObject(value, index, expr)
    }
}