package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.expr.*
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter
import org.apache.uss.argus.function.Functions
import org.springframework.util.NumberUtils

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

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
        val operand = operandExpr.getOperand<Boolean>()
                ?: throw TypeMismatchException("Require Boolean, got :" + operandExpr.toString())
        stack.pop()
        stack.push(OperandExpr(x, !operand))
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
        if ((x.operator == SQLBinaryOperator.Is || x.operator == SQLBinaryOperator.IsNot) && !right.isNil()) {
            throw TypeMismatchException("Required NULL, got:" + right.expr.toString())
        }
        when {
            x.operator == SQLBinaryOperator.Is -> {
                stack.push(OperandExpr(x, left.isNil()))
                return true
            }
            x.operator == SQLBinaryOperator.IsNot -> {
                stack.push(OperandExpr(x, !left.isNil()))
                return true
            }
        }
        return false
    }

    private fun processEqualOperation(expr: SQLBinaryOpExpr, left: OperandExpr, right: OperandExpr): Boolean {
        if (expr.operator == SQLBinaryOperator.Equality) {
            when {
                left.isType<Number>() && right.isType<Number>() -> {
                    stack.push(OperandExpr(expr, left.getOperand<Number>() == right.getOperand<Number>()))
                }
                left.isType<Boolean>() && right.isType<Boolean>() -> {
                    stack.push(OperandExpr(expr, left.getOperand<Boolean>() == right.getOperand<Boolean>()))
                }
                left.isType<String>() && right.isType<String>() -> {
                    stack.push(OperandExpr(expr, left.getOperand<String>() == right.getOperand<String>()))
                }
                left.isNil() && right.isNil() -> {
                    stack.push(OperandExpr(expr, true))
                }
                left.isNil() || right.isNil() -> {
                    stack.push(OperandExpr(expr, false))
                }
                else -> {
                    throw TypeMismatchException("Incompatible type to compare, left:" + left.expr.toString()
                            + " right:" + right.expr.toString())
                }
            }
            return true
        }
        return false
    }


    private fun processBooleanOperation(expr: SQLBinaryOpExpr, left: OperandExpr, right: OperandExpr): Boolean {
        val action: ((SQLExpr, Boolean, Boolean) -> Unit) =
                when (expr.operator) {
                    SQLBinaryOperator.BooleanAnd -> {
                        this::booleanAnd
                    }
                    SQLBinaryOperator.BooleanOr -> {
                        this::booleanOr
                    }
                    SQLBinaryOperator.BooleanXor -> {
                        this::booleanXor
                    }
                    else -> {
                        return false
                    }
                }

        val leftOperand = left.getOperand<Boolean>()
                ?: throw TypeMismatchException("Required Boolean, got " + left.expr.toString())

        val rightOperand = right.getOperand<Boolean>()
                ?: throw TypeMismatchException("Required Boolean, got " + right.expr.toString())

        action(expr, leftOperand, rightOperand)
        return true
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
        if (left.isNil() || right.isNil()) {
            stack.push(OperandExpr(expr, Nil))
            return true
        }
        return false
    }

    private fun numberItSelf(operandExpr: OperandExpr): Number {
        return operandExpr.getOperand<BigDecimal>()
                ?: throw TypeMismatchException("Require Number, got: " + operandExpr.toString())
    }

    private fun negative(operandExpr: OperandExpr): Number {
        val operand = operandExpr.getOperand<Number>()
        return when (operand) {
            is BigDecimal -> {
                -operand
            }
            is BigInteger -> {
                -operand
            }
            else -> {
                throw TypeMismatchException("Require Number, got: " + operandExpr.toString())
            }
        }
    }

    private fun flipBoolean(operandExpr: OperandExpr): Boolean {
        val operand = operandExpr.getOperand<Boolean>()
                ?: throw TypeMismatchException("Require Boolean, got: " + operandExpr.toString())
        return !operand
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
        return when {
            operandExpr.isType<BigDecimal>() -> operandExpr.getOperand<BigDecimal>()!!
            operandExpr.isType<Number>() -> NumberUtils.convertNumberToTargetClass(operandExpr.getOperand<Number>(), BigDecimal::class.java) as BigDecimal
            else -> {
                try {
                    NumberUtils.parseNumber(operandExpr.getJavaOperand().toString(), BigDecimal::class.java) as BigDecimal
                } catch (e: IllegalArgumentException) {
                    throw TypeMismatchException("Required Number, got " + operandExpr.expr.toString())
                }
            }
        }
    }
}