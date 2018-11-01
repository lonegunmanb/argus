package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr

abstract class EvalObject(val objectName: String, expr: SQLExpr) : Operand(expr) {
    abstract operator fun get(index: String, expr: SQLExpr): EvalObject
}