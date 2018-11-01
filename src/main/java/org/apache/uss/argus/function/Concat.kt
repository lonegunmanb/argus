package org.apache.uss.argus.function

import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr
import org.apache.uss.argus.operand.Operand

class Concat : FunctionCall {
    override fun eval(expr: SQLMethodInvokeExpr, vararg args: Operand): Any {
        val buf = StringBuilder()
        for ((_, operand) in args.filter { o -> !o.isNil() }) {
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
