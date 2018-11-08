package org.apache.uss.argus.visitor

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter

class SourceVisitor : SQLASTVisitorAdapter() {
    var source: String = ""
        private set
    var alias: String? = null
        private set

    override fun visit(x: SQLExprTableSource): Boolean {
        source = x.name.simpleName
        alias = x.alias
        return false
    }
}