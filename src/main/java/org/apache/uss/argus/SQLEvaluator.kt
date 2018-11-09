package org.apache.uss.argus

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.SQLStatement
import org.apache.uss.argus.operand.ValidationObject
import org.apache.uss.argus.visitor.EvaluatorVisitor
import org.apache.uss.argus.visitor.SourceVisitor

class SQLEvaluator(val sql: String, val dbType: String) {
    private var statement: SQLStatement? = null
    var source: String? = null
        private set
    var sourceAlias: String? = null
        private set

    companion object {
        fun compile(sql: String, dbType: String): SQLEvaluator {
            val evaluator = SQLEvaluator(sql, dbType)
            val statement = parseStatement(sql, dbType)
            evaluator.statement = statement
            val sourceVisitor = SourceVisitor()
            evaluator.statement!!.accept(sourceVisitor)
            evaluator.source = sourceVisitor.source
            evaluator.sourceAlias = sourceVisitor.alias
            validateStatement(statement, sourceVisitor.source, evaluator.sourceAlias)
            return evaluator
        }

        private fun parseStatement(sql: String, dbType: String): SQLStatement {
            val statements =
                    try {
                        SQLUtils.parseStatements(sql, dbType)
                    } catch (e: com.alibaba.druid.sql.parser.ParserException) {
                        throw ParserException(e.message)
                    }
            if (statements.size != 1) {
                throw ParserException("only one sql supported.")
            }
            return statements[0]
        }

        fun validateStatement(statement: SQLStatement, sourceName: String, sourceAlias: String?) {
            val visitor = EvaluatorVisitor(ValidationObject(sourceName, sourceAlias, null))
            statement.accept(visitor)
        }
    }
}