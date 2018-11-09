package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import kotlin.reflect.KClass

abstract class Operand(val expr: SQLExpr?) {

    var alias: String? = null
        internal set
    var objectName: String? = null
        internal set

    inline fun <reified T> getOperand(): T? {
        return this.operand(T::class) as? T
    }

    inline fun <reified T> isType(): Boolean {
        return this.isType(T::class)
    }

    @PublishedApi
    internal abstract fun operand(clazz: KClass<*>): Any?

    @PublishedApi
    internal abstract fun isType(clazz: KClass<*>): Boolean


    internal abstract fun isNil(): Boolean

    operator fun component1(): SQLExpr? {
        return expr
    }

    operator fun component2(): Any? {
        return this.operand(Any::class)
    }
}

