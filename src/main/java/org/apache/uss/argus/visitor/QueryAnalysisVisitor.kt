package org.apache.uss.argus.visitor

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter

class QueryAnalysisVisitor : SQLASTVisitorAdapter() {
    var source: String = ""
        private set
    var alias: String? = null
        private set
    var where: SQLExpr? = null
        private set

    override fun visit(x: SQLSelectQueryBlock): Boolean {
        this.where = x.where
        return true
    }

    override fun visit(x: SQLExprTableSource): Boolean {
        this.source = x.name.simpleName
        this.alias = x.alias
        return false
    }
}