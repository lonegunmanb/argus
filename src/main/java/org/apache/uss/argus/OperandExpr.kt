package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr

open class OperandExpr(val expr: SQLExpr, private val operand: Any) {
    open fun <T> getOperand() : T?{
        return operand as T?
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