package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr

abstract class EvalObject : Operand {

    constructor(objectName: String, alias: String?, expr: SQLExpr?) : super(expr) {
        this.objectName = objectName
        this.alias = alias
    }

    abstract operator fun get(property: String, expr: SQLExpr): EvalObject
    abstract operator fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject
}