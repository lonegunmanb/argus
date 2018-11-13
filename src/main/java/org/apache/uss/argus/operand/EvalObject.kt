package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLExpr
typealias Properties = List<Pair<String, Any?>>
abstract class EvalObject : Operand {

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(objectName: String, alias: String?, expr: SQLExpr?) : super(expr) {
        this.objectName = objectName
        this.alias = alias
    }

    abstract operator fun get(property: String, expr: SQLExpr): EvalObject
    abstract operator fun get(sqlArrayIndex: Int, expr: SQLExpr): EvalObject
    abstract fun getProperties(): Properties?
    abstract fun getArray(): Array<*>
}