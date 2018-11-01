package org.apache.uss.argus.operand

import com.alibaba.druid.sql.ast.SQLDataType
import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.SQLObject
import com.alibaba.druid.sql.visitor.SQLASTVisitor

class DummyExpr : SQLExpr {
    override fun setParent(p0: SQLObject?) {

    }

    override fun getBeforeCommentsDirect(): MutableList<String>? {
        return null
    }

    override fun addBeforeComment(p0: String?) {

    }

    override fun addBeforeComment(p0: MutableList<String>?) {

    }

    override fun getChildren(): MutableList<SQLObject>? {
        return null
    }

    override fun getAfterCommentsDirect(): MutableList<String>? {
        return null
    }

    override fun addAfterComment(p0: String?) {

    }

    override fun addAfterComment(p0: MutableList<String>?) {

    }

    override fun computeDataType(): SQLDataType? {
        return null
    }

    override fun output(p0: StringBuffer?) {

    }

    override fun getAttributes(): MutableMap<String, Any>? {
        return null
    }

    override fun putAttribute(p0: String?, p1: Any?) {

    }

    override fun getAttribute(p0: String?): Any? {
        return null
    }

    override fun hasBeforeComment(): Boolean {
        return false
    }

    override fun accept(p0: SQLASTVisitor?) {

    }

    override fun clone(): SQLExpr {
        return this
    }

    override fun hasAfterComment(): Boolean {
        return false
    }

    override fun getAttributesDirect(): MutableMap<String, Any>? {
        return null
    }

    override fun getParent(): SQLObject? {
        return null
    }

}