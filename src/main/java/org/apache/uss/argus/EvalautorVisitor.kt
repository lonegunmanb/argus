package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.expr.*
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter
import org.springframework.util.NumberUtils

import java.math.BigDecimal
import java.util.*

data class OperandExpr(val expr: SQLExpr, val operand: Any)

class EvaluatorVisitor : SQLASTVisitorAdapter() {
    var value: Any? = null
        get() = stack.peek()

    private val stack: Stack<Any> = Stack()

    override fun visit(expr: SQLIntegerExpr?): Boolean {
        stack.push(OperandExpr(expr!!, NumberUtils.convertNumberToTargetClass(expr.number, BigDecimal::class.java)))
        return true
    }

    override fun visit(expr: SQLNumberExpr?): Boolean {
        stack.push(OperandExpr(expr!!, NumberUtils.convertNumberToTargetClass(expr.number, BigDecimal::class.java)))
        return true
    }

    override fun visit(expr: SQLCharExpr?): Boolean {
        stack.push(OperandExpr(expr!!, expr.text))
        return true
    }

    override fun visit(expr: SQLBooleanExpr?): Boolean {
        stack.push(OperandExpr(expr!!, expr.value))
        return true
    }

    override fun visit(x: SQLBinaryOpExpr?): Boolean {
        val op = x!!.operator
        stack.push(op)
        return true
    }

    override fun endVisit(x: SQLBinaryOpExpr?) {
        val right = stack.pop() as OperandExpr
        val left = stack.pop() as OperandExpr
        @Suppress("UNUSED_VARIABLE")
        val opHereIsNotUsefulButJustWantMakeStackReadableWithOps = stack.pop() as SQLBinaryOperator
        if (processNumericOperation(x!!, left, right)) {
            return
        }
        if (processBooleanOperation(x, left, right)) {
            return
        }
    }

    private fun processBooleanOperation(expr: SQLBinaryOpExpr, left: OperandExpr, right: OperandExpr): Boolean {
        var action: ((SQLExpr, Boolean, Boolean) -> Unit)? = null
        when (expr.operator) {
            SQLBinaryOperator.BooleanAnd -> {
                action = this::booleanAnd
            }
            SQLBinaryOperator.BooleanOr -> {
                action = this::booleanOr
            }
            SQLBinaryOperator.BooleanXor -> {
                action = this::booleanXor
            }
            else -> {
            }
        }
        if (action != null) {
            if (left.operand !is Boolean) {
                throw TypeCastException("Required Boolean, got " + left.expr.toString())
            }
            if (right.operand !is Boolean) {
                throw TypeCastException("Required Boolean, got " + right.expr.toString())
            }
            action(expr, left.operand, right.operand)
            return true
        }
        return false
    }

    private fun processNumericOperation(expr: SQLBinaryOpExpr, left: OperandExpr, right: OperandExpr): Boolean {
        var action: ((SQLExpr, BigDecimal, BigDecimal) -> Unit)? = null
        when (expr.operator) {
            SQLBinaryOperator.Add -> {
                action = this::add
            }
            SQLBinaryOperator.Subtract -> {
                action = this::subtract
            }
            SQLBinaryOperator.Multiply -> {
                action = this::multiply
            }
            SQLBinaryOperator.Divide -> {
                action = this::divide
            }
            SQLBinaryOperator.Mod -> {
                action = this::mod
            }
            SQLBinaryOperator.GreaterThan -> {
                action = this::greaterThan
            }
            SQLBinaryOperator.GreaterThanOrEqual -> {
                action = this::greaterThanOrEqual
            }
            SQLBinaryOperator.LessThan -> {
                action = this::lessThan
            }
            SQLBinaryOperator.LessThanOrEqual -> {
                action = this::lessThanOrEqual
            }
            else -> {
            }
        }
        if (action != null) {
            action(expr, toNumber(left), toNumber(right))
            return true
        }
        return false
    }

    private fun booleanXor(expr: SQLExpr, left: Boolean, right: Boolean) {
        stack.push(OperandExpr(expr, left xor right))
    }

    private fun booleanAnd(expr: SQLExpr, left: Boolean, right: Boolean) {
        stack.push(OperandExpr(expr, left && right))
    }

    private fun booleanOr(expr: SQLExpr, left: Boolean, right: Boolean) {
        stack.push(OperandExpr(expr, left || right))
    }

    private fun lessThanOrEqual(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left <= right))
    }

    private fun lessThan(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left < right))
    }

    private fun greaterThanOrEqual(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left >= right))
    }

    private fun greaterThan(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left > right))
    }

    private fun mod(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left.toBigInteger().mod(right.toBigInteger())))
    }

    private fun divide(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left.divide(right)))
    }

    private fun multiply(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left.multiply(right)))
    }

    private fun subtract(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left.subtract(right)))
    }

    private fun add(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(OperandExpr(expr, left.add(right)))
    }

    private fun toNumber(operandExpr: OperandExpr): BigDecimal {
        val operand = operandExpr.operand
        if (operand is BigDecimal) {
            return operand
        }
        if (operand is Number) {
            return NumberUtils.convertNumberToTargetClass(operand, BigDecimal::class.java) as BigDecimal
        }
        try {
            return NumberUtils.parseNumber(operand.toString(), BigDecimal::class.java) as BigDecimal
        } catch (e: IllegalArgumentException) {
            throw TypeCastException("Required Number, got " + operandExpr.expr.toString())
        }
    }
}