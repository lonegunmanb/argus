package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.expr.*
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter
import org.apache.uss.argus.function.FunctionCalls
import org.apache.uss.argus.operand.EvalObject
import org.apache.uss.argus.operand.EvaluatedOperand
import org.apache.uss.argus.operand.Operand
import org.springframework.util.NumberUtils

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class EvaluatorVisitor() : SQLASTVisitorAdapter() {
    object Nil

    var value: Any? = null
        get() = stack.peek()

    private val stack: Stack<Any> = Stack()
    private var source: EvalObject? = null

    constructor(source: EvalObject) : this() {
        this.source = source
    }

    override fun visit(expr: SQLIntegerExpr): Boolean {
        stack.push(EvaluatedOperand(expr, NumberUtils.convertNumberToTargetClass(expr.number, BigDecimal::class.java)))
        return true
    }

    override fun visit(expr: SQLNumberExpr): Boolean {
        stack.push(EvaluatedOperand(expr, NumberUtils.convertNumberToTargetClass(expr.number, BigDecimal::class.java)))
        return true
    }

    override fun visit(expr: SQLCharExpr): Boolean {
        stack.push(EvaluatedOperand(expr, expr.text))
        return true
    }

    override fun visit(expr: SQLBooleanExpr): Boolean {
        stack.push(EvaluatedOperand(expr, expr.value))
        return true
    }

    override fun visit(expr: SQLIdentifierExpr): Boolean {
        stack.push(source!!)
        stack.push(expr)
        return true
    }

    override fun endVisit(expr: SQLIdentifierExpr) {
        stack.pop()
        val obj = stack.pop() as EvalObject
        val operand = when (expr.name) {
            obj.objectName -> obj
            else -> obj[obj.objectName, expr]
        }
        stack.push(operand)
    }

    override fun visit(expr: SQLPropertyExpr): Boolean {
        stack.push(expr)
        return true
    }

    override fun endVisit(expr: SQLPropertyExpr) {
        val operand = stack.pop() as EvalObject
        stack.pop()
        stack.push(operand[expr.name, expr])
    }

    override fun visit(x: SQLNotExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLNotExpr) {
        val operandExpr = stack.pop() as Operand
        val operand = operandExpr.getOperand<Boolean>()
                ?: throw TypeMismatchException("Require Boolean, got :" + operandExpr.toString())
        stack.pop()
        stack.push(EvaluatedOperand(x, !operand))
    }

    override fun visit(x: SQLNullExpr): Boolean {
        stack.push(EvaluatedOperand(x, Nil))
        return true
    }

    override fun visit(x: SQLUnaryExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLUnaryExpr) {
        val operandExpr = stack.pop() as Operand
        val func: ((Operand) -> Any) = when (x.operator) {
            SQLUnaryOperator.Not -> this::flipBoolean
            SQLUnaryOperator.NOT -> this::flipBoolean
            SQLUnaryOperator.Negative -> this::negative
            SQLUnaryOperator.Plus -> this::numberItSelf
            else -> throw UnsupportedFeatureException(x.toString())
        }
        stack.pop()
        stack.push(EvaluatedOperand(x, func(operandExpr)))
    }

    override fun visit(x: SQLBinaryOpExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLBinaryOpExpr) {
        val right = stack.pop() as Operand
        val left = stack.pop() as Operand
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
            processEqualityOperation(x, left, right) -> {
            }
            else -> throw UnsupportedFeatureException(x.toString())
        }
    }

    override fun visit(x: SQLMethodInvokeExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLMethodInvokeExpr) {
        val args = LinkedList<Operand>()
        do {
            val frame = stack.pop()
            if (frame is Operand) {
                args.push(frame)
            }
        } while (frame != x)

        val function = FunctionCalls.get(x.methodName) ?: throw UnsupportedFeatureException(x.toString())
        val eval = function.eval(x, *args.toTypedArray())
        stack.push(EvaluatedOperand(x, eval))
    }

    private fun processIsAndIsNot(left: Operand, right: Operand, x: SQLBinaryOpExpr): Boolean {
        if ((x.operator == SQLBinaryOperator.Is || x.operator == SQLBinaryOperator.IsNot) && !right.isNil()) {
            throw TypeMismatchException("Required NULL, got:" + right.expr.toString())
        }
        when {
            x.operator == SQLBinaryOperator.Is -> {
                stack.push(EvaluatedOperand(x, left.isNil()))
                return true
            }
            x.operator == SQLBinaryOperator.IsNot -> {
                stack.push(EvaluatedOperand(x, !left.isNil()))
                return true
            }
        }
        return false
    }

    private fun processEqualityOperation(expr: SQLBinaryOpExpr, left: Operand, right: Operand): Boolean {
        return when (expr.operator) {
            SQLBinaryOperator.Equality -> equal(expr, left, right)
            SQLBinaryOperator.NotEqual -> notEqual(expr, left, right)
            SQLBinaryOperator.LessThanOrGreater -> notEqual(expr, left, right)
            else -> false
        }
    }

    private fun equal(expr: SQLBinaryOpExpr, left: Operand, right: Operand): Boolean {
        when {
            left.isType<Number>() && right.isType<Number>() -> {
                stack.push(EvaluatedOperand(expr, left.getOperand<Number>() == right.getOperand<Number>()))
            }
            left.isType<Boolean>() && right.isType<Boolean>() -> {
                stack.push(EvaluatedOperand(expr, left.getOperand<Boolean>() == right.getOperand<Boolean>()))
            }
            left.isType<String>() && right.isType<String>() -> {
                stack.push(EvaluatedOperand(expr, left.getOperand<String>() == right.getOperand<String>()))
            }
            else -> {
                throw TypeMismatchException("Incompatible type to compare, left:" + left.expr.toString()
                        + " right:" + right.expr.toString())
            }
        }
        return true
    }

    private fun notEqual(expr: SQLBinaryOpExpr, left: Operand, right: Operand): Boolean {
        when {
            left.isType<Number>() && right.isType<Number>() -> {
                stack.push(EvaluatedOperand(expr, left.getOperand<Number>() != right.getOperand<Number>()))
            }
            left.isType<Boolean>() && right.isType<Boolean>() -> {
                stack.push(EvaluatedOperand(expr, left.getOperand<Boolean>() != right.getOperand<Boolean>()))
            }
            left.isType<String>() && right.isType<String>() -> {
                stack.push(EvaluatedOperand(expr, left.getOperand<String>() != right.getOperand<String>()))
            }
            else -> {
                throw TypeMismatchException("Incompatible type to compare, left:" + left.expr.toString()
                        + " right:" + right.expr.toString())
            }
        }
        return true
    }

    private fun processBooleanOperation(expr: SQLBinaryOpExpr, left: Operand, right: Operand): Boolean {
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

    private fun processNumericOperation(expr: SQLBinaryOpExpr, left: Operand, right: Operand): Boolean {
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

    private fun nullInBinaryOp(left: Operand, right: Operand, expr: SQLBinaryOpExpr): Boolean {
        if (left.isNil() || right.isNil()) {
            stack.push(EvaluatedOperand(expr, Nil))
            return true
        }
        return false
    }

    private fun numberItSelf(operand: Operand): Number {
        return operand.getOperand<BigDecimal>()
                ?: throw TypeMismatchException("Require Number, got: " + operand.toString())
    }

    private fun negative(operand: Operand): Number {
        val value = operand.getOperand<Number>()
        return when (value) {
            is BigDecimal -> {
                -value
            }
            is BigInteger -> {
                -value
            }
            else -> {
                throw TypeMismatchException("Require Number, got: " + value.toString())
            }
        }
    }

    private fun flipBoolean(operand: Operand): Boolean {
        val value = operand.getOperand<Boolean>()
                ?: throw TypeMismatchException("Require Boolean, got: " + operand.toString())
        return !value
    }

    private fun booleanXor(expr: SQLExpr, left: Boolean, right: Boolean) {
        stack.push(EvaluatedOperand(expr, left xor right))
    }

    private fun booleanAnd(expr: SQLExpr, left: Boolean, right: Boolean) {
        stack.push(EvaluatedOperand(expr, left && right))
    }

    private fun booleanOr(expr: SQLExpr, left: Boolean, right: Boolean) {
        stack.push(EvaluatedOperand(expr, left || right))
    }

    private fun lessThanOrEqual(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left <= right))
    }

    private fun lessThan(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left < right))
    }

    private fun greaterThanOrEqual(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left >= right))
    }

    private fun greaterThan(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left > right))
    }

    private fun mod(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left.toBigInteger().mod(right.toBigInteger()).toBigDecimal()))
    }

    private fun divide(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        try {
            stack.push(EvaluatedOperand(expr, left.divide(right)))
        } catch (e: ArithmeticException) {
            throw ArithmeticException(expr.toString())
        }
    }

    private fun multiply(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left.multiply(right)))
    }

    private fun subtract(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left.subtract(right)))
    }

    private fun add(expr: SQLExpr, left: BigDecimal, right: BigDecimal) {
        stack.push(EvaluatedOperand(expr, left.add(right)))
    }

    private fun toNumber(operand: Operand): BigDecimal {
        return when {
            operand.isType<BigDecimal>() -> operand.getOperand<BigDecimal>()!!
            operand.isType<Number>() -> NumberUtils.convertNumberToTargetClass(operand.getOperand<Number>(), BigDecimal::class.java) as BigDecimal
            else -> {
                try {
                    NumberUtils.parseNumber(operand.getOperand<Any>().toString(), BigDecimal::class.java) as BigDecimal
                } catch (e: IllegalArgumentException) {
                    throw TypeMismatchException("Required Number, got " + operand.expr.toString())
                }
            }
        }
    }
}