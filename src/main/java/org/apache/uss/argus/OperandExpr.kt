package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import kotlin.reflect.KClass

open class OperandExpr(val expr: SQLExpr, private val operand: Any) {

    @Suppress("PROTECTED_CALL_FROM_PUBLIC_INLINE")
    inline fun <reified T> getOperand(): T? {
        return this.operand(T::class) as? T
    }

    @Suppress("PROTECTED_CALL_FROM_PUBLIC_INLINE")
    inline fun <reified T> isType(): Boolean {
        return this.isType(T::class)
    }

    @Suppress("ProtectedInFinal", "UNUSED_PARAMETER")
    protected open fun operand(clazz: KClass<*>): Any? {
        return operand
    }

    protected open fun isType(clazz: KClass<*>): Boolean {
        return clazz.java.isAssignableFrom(operand.javaClass)
    }

    fun getJavaOperand(): Any? {
        return getOperand()
    }
    fun isNil():Boolean {
        return operand == EvaluatorVisitor.Nil
    }

    operator fun component1():SQLExpr {
        return expr
    }

    operator fun component2():Any {
        return operand
    }
}