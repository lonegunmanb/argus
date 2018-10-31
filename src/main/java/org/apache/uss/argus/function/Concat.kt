package org.apache.uss.argus.function

import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr
import org.apache.uss.argus.OperandExpr

class Concat : Function {
    override fun eval(x: SQLMethodInvokeExpr, vararg args: OperandExpr): Any? {
        val buf = StringBuilder()
        for ((_, operand) in args.filter { operandExpr -> !operandExpr.isNil() }) {
            when (operand) {
                true -> buf.append("t")
                false -> buf.append("f")
                else -> buf.append(operand.toString())
            }
        }
        return buf.toString()
    }

    companion object {
        val instance = Concat()
    }
}
