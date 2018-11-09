package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr

abstract class EvalObject(val objectName: String, val alias: String?, expr: SQLExpr?) : Operand(expr) {
    abstract operator fun get(property: String, expr: SQLExpr): EvalObject
    abstract operator fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject
}