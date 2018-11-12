package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
import org.apache.uss.argus.visitor.EvaluatorVisitor
import kotlin.reflect.KClass

class EvaluatedOperand(expr: SQLExpr, private val operandEvaluated: Any) : Operand(expr) {
    override fun getProperties(): List<Pair<String, Any?>>? {
        return ArrayList(0)
    }

    override fun operand(clazz: KClass<*>): Any? {
        return operandEvaluated
    }

    override fun isType(clazz: KClass<*>): Boolean {
        return clazz.java.isAssignableFrom(operandEvaluated.javaClass)
    }

    override fun isNil(): Boolean {
        return operandEvaluated === EvaluatorVisitor.Nil
    }

    fun getJavaOperand(): Any? {
        return getOperand()
    }
}