package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.expr.*
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter
import org.apache.uss.argus.function.Functions
import org.springframework.util.NumberUtils

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

data class OperandExpr(val expr: SQLExpr, val operand: Any)

class EvaluatorVisitor : SQLASTVisitorAdapter() {
    object Nil

    var value: Any? = null
        get() = stack.peek()

    private val stack: Stack<Any> = Stack()

    override fun visit(expr: SQLIntegerExpr): Boolean {
        stack.push(OperandExpr(expr, NumberUtils.convertNumberToTargetClass(expr.number, BigDecimal::class.java)))
        return true
    }

    override fun visit(expr: SQLNumberExpr): Boolean {
        stack.push(OperandExpr(expr, NumberUtils.convertNumberToTargetClass(expr.number, BigDecimal::class.java)))
        return true
    }

    override fun visit(expr: SQLCharExpr): Boolean {
        stack.push(OperandExpr(expr, expr.text))
        return true
    }

    override fun visit(expr: SQLBooleanExpr): Boolean {
        stack.push(OperandExpr(expr, expr.value))
        return true
    }

    override fun visit(x: SQLNotExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLNotExpr) {
        val operandExpr = stack.pop() as OperandExpr
        if (operandExpr.operand !is Boolean) {
            throw TypeMismatchException("Require Boolean, got :" + operandExpr.toString())
        }
        stack.pop()
        stack.push(OperandExpr(x, !operandExpr.operand))
    }

    override fun visit(x: SQLNullExpr): Boolean {
        stack.push(OperandExpr(x, Nil))
        return true
    }

    override fun visit(x: SQLUnaryExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLUnaryExpr) {
        val operandExpr = stack.pop() as OperandExpr
        val func: ((OperandExpr) -> Any) = when (x.operator) {
            SQLUnaryOperator.Not -> this::flipBoolean
            SQLUnaryOperator.NOT -> this::flipBoolean
            SQLUnaryOperator.Negative -> this::negative
            SQLUnaryOperator.Plus -> this::numberItSelf
            else -> throw UnsupportedFeatureException(x.toString())
        }
        stack.pop()
        stack.push(OperandExpr(x, func(operandExpr)))
    }

    override fun visit(x: SQLBinaryOpExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLBinaryOpExpr) {
        val right = stack.pop() as OperandExpr
        val left = stack.pop() as OperandExpr
        @Suppress("UNUSED_VARIABLE")
        val popBinaryExprFromStack = stack.pop()
        when {
            processIsAndIsNot(left, right, x) -> {
            }
            nullInBinaryOp(left, right, x) -> {
            }
            processNumericOperation(x, left, right) -> {
            }
            processBooleanOperation(x, left, right) -> {
            }
            processEqualOperation(x, left, right) -> {
            }
            else -> throw UnsupportedFeatureException(x.toString())
        }
    }

    override fun visit(x: SQLMethodInvokeExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLMethodInvokeExpr) {
        val args = LinkedList<OperandExpr?>()
        do {
            val frame = stack.pop()
            if (frame is OperandExpr) {
                args.push(frame)
            }
        } while (frame != x)

        val function = Functions.get(x.methodName) ?: throw UnsupportedFeatureException(x.toString())
        val eval = function.eval(x, *args.toTypedArray())
        stack.push(OperandExpr(x, eval))
    }

    private fun processIsAndIsNot(left: OperandExpr, right: OperandExpr, x: SQLBinaryOpExpr): Boolean {
        if ((x.operator == SQLBinaryOperator.Is || x.operator == SQLBinaryOperator.IsNot) && right.operand != Nil) {
            throw TypeMismatchException("Required NULL, got:" + right.expr.toString())
        }
        when {
            x.operator == SQLBinaryOperator.Is -> {
                stack.push(OperandExpr(x, left.operand == Nil))
                return true
            }
            x.operator == SQLBinaryOperator.IsNot -> {
                stack.push(OperandExpr(x, left.operand != Nil))
                return true
            }
        }
        return false
    }

    private fun processEqualOperation(expr: SQLBinaryOpExpr, left: OperandExpr, right: OperandExpr): Boolean {
        if (expr.operator == SQLBinaryOperator.Equality) {
            val leftOperand = left.operand
            val rightOperand = right.operand
            when {
                leftOperand is Number && rightOperand is Number -> {
                }
                leftOperand is String && rightOperand is String -> {
                }
                leftOperand is Boolean && rightOperand is Boolean -> {
                }
                leftOperand == Nil && rightOperand == Nil -> {
                    stack.push(OperandExpr(expr, true))
                }
                leftOperand == Nil || rightOperand == Nil -> {
                    stack.push(OperandExpr(expr, false))
                }
                else -> {
                    throw TypeMismatchException("Incompatible type to compare, left:" + left.expr.toString()
                            + " right:" + right.expr.toString())
                }
            }
            stack.push(OperandExpr(expr, leftOperand == rightOperand))
            return true
        }
        return false
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
                throw TypeMismatchException("Required Boolean, got " + left.expr.toString())
            }
            if (right.operand !is Boolean) {
                throw TypeMismatchException("Required Boolean, got " + right.expr.toString())
            }
            action(expr, left.operand, right.operand)
            return true
        }
        return false
    }

    private fun processNumericOperation(expr: SQLBinaryOpExpr, left: OperandExpr, right: OperandExpr): Boolean {
        val action: ((SQLExpr, BigDecimal, BigDecimal) -> Unit)? =
                when (expr.operator) {
                    SQLBinaryOperator.Add -> this::add
                    SQLBinaryOperator.Subtract -> this::subtract
                    SQLBinaryOperator.Multiply -> this::multiply
                    SQLBinaryOperator.Modulus -> this::mod
                    SQLBinaryOperator.GreaterThan -> this::greaterThan
                    SQLBinaryOperator.GreaterThanOrEqual -> this::greaterThanOrEqual
                    SQLBinaryOperator.LessThan -> this::lessThan
                    SQLBinaryOperator.LessThanOrEqual -> this::lessThanOrEqual
                    SQLBinaryOperator.Divide -> this::divide
                    else -> null
                }
        if (action != null) {
            action(expr, toNumber(left), toNumber(right))
            return true
        }
        return false
    }

    private fun nullInBinaryOp(left: OperandExpr, right: OperandExpr, expr: SQLBinaryOpExpr): Boolean {
        val leftOperand = left.operand
        val rightOperand = right.operand
        if (leftOperand == Nil || rightOperand == Nil) {
            stack.push(OperandExpr(expr, Nil))
            return true
        }
        return false
    }

    private fun numberItSelf(operandExpr: OperandExpr): Number {
        return when (operandExpr.operand) {
            is BigDecimal -> {
                operandExpr.operand
            }
            else -> {
                throw TypeMismatchException("Require Number, got: " + operandExpr.toString())
            }
        }
    }

    private fun negative(operandExpr: OperandExpr): Number {
        return when (operandExpr.operand) {
            is BigDecimal -> {
                -operandExpr.operand
            }
            is BigInteger -> {
                -operandExpr.operand
            }
            else -> {
                throw TypeMismatchException("Require Number, got: " + operandExpr.toString())
            }
        }
    }

    private fun flipBoolean(operandExpr: OperandExpr): Boolean {
        if (operandExpr.operand !is Boolean) {
            throw TypeMismatchException("Require Boolean, got: " + operandExpr.toString())
        }
        return !operandExpr.operand
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
        stack.push(OperandExpr(expr, left.toBigInteger().mod(right.toBigInteger()).toBigDecimal()))
    }

    private fun divide(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        try {
            stack.push(OperandExpr(expr, left.divide(right)))
        } catch (e: ArithmeticException) {
            throw ArithmeticException(expr.toString())
        }
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
        return when (operand) {
            is BigDecimal -> operand
            is Number -> NumberUtils.convertNumberToTargetClass(operand, BigDecimal::class.java) as BigDecimal
            else -> {
                try {
                    NumberUtils.parseNumber(operand.toString(), BigDecimal::class.java) as BigDecimal
                } catch (e: IllegalArgumentException) {
                    throw TypeMismatchException("Required Number, got " + operandExpr.expr.toString())
                }
            }
        }
    }
}