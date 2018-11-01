package org.apache.uss.argus.function

import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr
import org.apache.uss.argus.operand.Operand

interface FunctionCall {
    fun eval(expr: SQLMethodInvokeExpr, vararg args: Operand): Any
}
