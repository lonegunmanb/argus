package org.apache.uss.argus

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.SQLStatement
import org.apache.uss.argus.operand.EvalObject
import org.apache.uss.argus.operand.Operand
import org.apache.uss.argus.operand.ValidationObject
import org.apache.uss.argus.visitor.EvaluatorVisitor
import org.apache.uss.argus.visitor.QueryAnalysisVisitor
import java.util.*

class SQLEvaluator {
    private val statement: SQLStatement
    private val hasWhere: Boolean
    val sql: String
    val dbType: String
    val source: String
    val sourceAlias: String?

    constructor(statement: SQLStatement, sql: String, dbType: String, source: String, sourceAlias: String?, hasWhere: Boolean) {
        this.statement = statement
        this.sql = sql
        this.dbType = dbType
        this.source = source
        this.sourceAlias = sourceAlias
        this.hasWhere = hasWhere
    }

    companion object {
        fun compile(sql: String, dbType: String): SQLEvaluator {
            val statement = parseStatement(sql, dbType)
            val analysisVisitor = QueryAnalysisVisitor()
            statement.accept(analysisVisitor)
            val evaluator = SQLEvaluator(statement, sql, dbType, analysisVisitor.source, analysisVisitor.alias, analysisVisitor.where != null)
            validateStatement(statement, analysisVisitor.source, evaluator.sourceAlias)
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

    internal fun evalOutputs(source: EvalObject): Iterable<Operand> {
        source.objectName = this.source
        source.alias = this.sourceAlias
        val visitor = EvaluatorVisitor(source)
        this.statement.accept(visitor)
        if (hasWhere) {
            val valid = (visitor.value as Operand).getOperand<Boolean>() ?: false
            if (!valid) {
                return Iterable { Collections.emptyIterator<EvalObject>() }
            }
        }
        return visitor.outputs
    }
}