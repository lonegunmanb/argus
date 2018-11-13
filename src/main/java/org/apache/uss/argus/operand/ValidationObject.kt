package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import org.apache.uss.argus.TypeParadoxException
import org.apache.uss.argus.UnsupportedFeatureException
import org.apache.uss.argus.visitor.EvaluatorVisitor
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

internal class ValidationObject(objectName: String, alias: String?, expr: SQLExpr?) : EvalObject(objectName, alias, expr) {
    override fun getProperties(): List<Pair<String, Any?>>? {
        throw UnsupportedFeatureException("Cannot get validation object's properties")
    }

    override fun getArray(): Array<*> {
        throw UnsupportedOperationException()
    }

    companion object {
        private object Obj

        private val ObjType = Obj.javaClass.kotlin
    }

    private var type: KClass<*>? = null
    private val properties: HashMap<String, ValidationObject> = HashMap()

    override fun get(property: String, expr: SQLExpr): EvalObject {
        checkType(ObjType)
        return properties.getOrPut(property) { ValidationObject(property, null, expr) }
    }

    override fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject {
        checkType<Array<Any>>()
        val propertyName = "$objectName[$sqlArrayIndex]"
        return properties.getOrPut(propertyName) { ValidationObject(propertyName, null, expr) }
    }

    override fun operand(clazz: KClass<*>): Any? {
        checkType(clazz)
        return when (clazz) {
            Any::class -> ""
            String::class -> ""
            BigDecimal::class -> BigDecimal.ONE
            BigInteger::class -> BigInteger.ONE
            Boolean::class -> false
            else -> EvaluatorVisitor.Nil
        }
    }

    private fun checkType(clazz: KClass<*>) {
        when {
            clazz == Any::class -> {
            }
            type == null -> type = clazz
            type == Any::class -> type = clazz
            isNumber(type) -> checkNumberType(clazz)
            type == String::class -> checkTypeOrThrow<String>(clazz)
            type == Boolean::class -> checkTypeOrThrow<Boolean>(clazz)
            type == ObjType -> if (clazz != ObjType) throw TypeParadoxException(ObjType, this.expr, clazz)
            type == Array<Any>::class -> checkTypeOrThrow<Array<Any>>(clazz)
            else -> throw UnsupportedFeatureException("unsupported type:${clazz.simpleName}")
        }
    }

    private inline fun <reified T> checkType() {
        when (type) {
            null -> type = T::class
            Any::class -> type = T::class
            T::class -> {
            }
            else -> throw TypeParadoxException(type!!, this.expr, T::class)
        }
    }

    private inline fun <reified T> checkTypeOrThrow(clazz: KClass<*>) {
        if (clazz != T::class) throw TypeParadoxException(type!!, expr, clazz)
    }

    private fun isNumber(t: KClass<*>?) = t == Number::class || Number::class.java.isAssignableFrom(t!!.java)

    private fun checkNumberType(clazz: KClass<*>) {
        if (!isNumber(clazz)) {
            throw TypeParadoxException(type!!, this.expr, clazz)
        }
    }

    override fun isType(clazz: KClass<*>): Boolean {
        return true
    }

    override fun isNil(): Boolean {
        return false
    }

}