package org.apache.uss.argus

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter

class SourceVisitor : SQLASTVisitorAdapter() {
    private var source: String? = null
        get
        private set

    override fun visit(x: SQLExprTableSource): Boolean {
        source = x.name.simpleName
        return false
    }
}