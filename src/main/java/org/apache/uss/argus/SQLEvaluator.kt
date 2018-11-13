package org.apache.uss.argus

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.SQLStatement
import org.apache.uss.argus.operand.EvalObject
import org.apache.uss.argus.operand.Operand
import org.apache.uss.argus.operand.ValidationObject
import org.apache.uss.argus.serializer.OperandSerializer
import org.apache.uss.argus.visitor.EvaluatorVisitor
import org.apache.uss.argus.visitor.QueryAnalysisVisitor
import java.util.*

internal class SQLEvaluator<TSerializer : OperandSerializer>(private val statement: SQLStatement, val sql: String, val dbType: String, val source: String, val sourceAlias: String?, private val hasWhere: Boolean, private val serializer: TSerializer) {

    companion object {
        fun <T : OperandSerializer> compile(sql: String, dbType: String, serializer: T): SQLEvaluator<T> {
            val statement = parseStatement(sql, dbType)
            val analysisVisitor = QueryAnalysisVisitor()
            statement.accept(analysisVisitor)
            val evaluator = SQLEvaluator<T>(statement, sql, dbType, analysisVisitor.source, analysisVisitor.alias, analysisVisitor.where != null, serializer)
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

    fun eval(source: EvalObject):TSerializer {
        val outputs = evalOutputs(source)
        var increments = 0
        outputs.forEach { operand-> run{
            val columnName = operand.alias ?: operand.objectName ?: "column${increments++}"
            when {
                operand.isNil() -> serializer.writeNull(columnName)
                operand.isType<Number>() -> serializer.writeNumber(operand.getOperand<Number>()!!, columnName)
                operand.isType<Boolean>() -> serializer.writeBoolean(operand.getOperand<Boolean>()!!, columnName)
                operand.isType<Array<*>>() -> serializer.writeArray((operand as EvalObject).getArray(), columnName)
                operand.isType<String>() -> serializer.writeString(operand.getOperand<String>()!!, columnName)
                else -> serializer.writeObject(operand as EvalObject, columnName)
            }
        } }
        return serializer
    }
}