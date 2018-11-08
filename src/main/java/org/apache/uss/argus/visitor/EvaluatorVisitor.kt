package org.apache.uss.argus.visitor

import com.alibaba.druid.sql.ast.SQLExpr
import com.alibaba.druid.sql.ast.expr.*
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator.*
import com.alibaba.druid.sql.ast.expr.SQLUnaryOperator.*
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter
import org.apache.uss.argus.TypeMismatchException
import org.apache.uss.argus.UnsupportedFeatureException
import org.apache.uss.argus.function.FunctionCalls
import org.apache.uss.argus.operand.EvalObject
import org.apache.uss.argus.operand.EvaluatedOperand
import org.apache.uss.argus.operand.Operand
import org.springframework.util.NumberUtils
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*

class EvaluatorVisitor : SQLASTVisitorAdapter {
    object Nil

    var value: Any? = null
        get() = stack.peek()

    private val stack: Stack<Any> = Stack()
    private val source: EvalObject?

    constructor() : this(null)

    constructor(source: EvalObject?) : super() {
        this.source = source
    }

    override fun visit(expr: SQLIntegerExpr): Boolean {
        stack.push(EvaluatedOperand(expr, toBigDecimal(expr.number)))
        return false
    }

    override fun visit(expr: SQLNumberExpr): Boolean {
        stack.push(EvaluatedOperand(expr, toBigDecimal(expr.number)))
        return false
    }

    private fun toBigDecimal(number: Number) =
            NumberUtils.convertNumberToTargetClass(number, BigDecimal::class.java)

    override fun visit(expr: SQLCharExpr): Boolean {
        stack.push(EvaluatedOperand(expr, expr.text))
        return false
    }

    override fun visit(expr: SQLBooleanExpr): Boolean {
        stack.push(EvaluatedOperand(expr, expr.value))
        return false
    }

    override fun visit(expr: SQLIdentifierExpr): Boolean {
        stack.push(source)
        stack.push(expr)
        return false
    }

    override fun endVisit(expr: SQLIdentifierExpr) {
        popPlaceholderFromStack()
        val source = stack.pop() as EvalObject
        val operand = when (expr.name) {
            source.objectName -> source
            source.alias -> source
            else -> source[expr.name, expr]
        }
        stack.push(operand)
    }

    override fun visit(expr: SQLPropertyExpr): Boolean {
        stack.push(expr)
        return true
    }

    override fun endVisit(expr: SQLPropertyExpr) {
        val operand = stack.pop() as EvalObject
        popPlaceholderFromStack()
        stack.push(operand[expr.name, expr])
    }

    override fun visit(expr: SQLArrayExpr): Boolean {
        val indexes = expr.values
        if (indexes.size != 1) {
            throw UnsupportedFeatureException("One demension array only")
        }
        stack.push(expr)
        return true
    }

    override fun endVisit(expr: SQLArrayExpr) {
        val index = popStackOperand<BigDecimal>()
        { TypeMismatchException("Required Int, got: $expr") }
        val operand = stack.pop() as EvalObject
        if (!isArray(operand)) {
            throw TypeMismatchException("Required array, got " + expr.toString())
        }
        popPlaceholderFromStack()
        val roundedIndex = index.setScale(0, RoundingMode.HALF_UP).toInt()
        val evalObject = operand[roundedIndex, expr]
        stack.push(evalObject)
    }

    private fun isArray(operand: Operand) = operand.isType<Array<Any>>()

    override fun visit(x: SQLNotExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLNotExpr) {
        val operand = popStackOperand<Boolean>()
        { operandExpr -> TypeMismatchException("Require Boolean, got :" + operandExpr.toString()) }
        popPlaceholderFromStack()
        stack.push(EvaluatedOperand(x, !operand))
    }

    override fun visit(x: SQLNullExpr): Boolean {
        stack.push(EvaluatedOperand(x, Nil))
        return false
    }

    override fun visit(x: SQLUnaryExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLUnaryExpr) {
        val operandExpr = stack.pop() as Operand
        val func: ((Operand) -> Any) = when (x.operator) {
            Not -> this::flipBoolean
            NOT -> this::flipBoolean
            Plus -> this::asNumber
            Negative -> this::negative
            else -> throw UnsupportedFeatureException(x.toString())
        }
        popPlaceholderFromStack()
        stack.push(EvaluatedOperand(x, func(operandExpr)))
    }

    override fun visit(x: SQLBinaryOpExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLBinaryOpExpr) {
        val right = stack.pop() as Operand
        val left = stack.pop() as Operand
        popPlaceholderFromStack()
        when {
            processIsAndIsNot(left, right, x) -> {
            }
            nullInBinaryOp(left, right, x) -> {
            }
            processNumericOperation(left, right, x) -> {
            }
            processBooleanOperation(left, right, x) -> {
            }
            processEqualityOperation(left, right, x) -> {
            }
            else -> throw UnsupportedFeatureException(x.toString())
        }
    }

    override fun visit(x: SQLMethodInvokeExpr): Boolean {
        stack.push(x)
        return true
    }

    override fun endVisit(x: SQLMethodInvokeExpr) {
        val args = popArguments(x)
        val function = FunctionCalls.get(x.methodName) ?: throw UnsupportedFeatureException(x.toString())
        val eval = function.eval(x, *args.toTypedArray())
        stack.push(EvaluatedOperand(x, eval))
    }

    private fun popArguments(x: SQLMethodInvokeExpr): Collection<Operand> {
        val args = LinkedList<Operand>()
        do {
            val frame = stack.pop()
            if (frame is Operand) {
                args.push(frame)
            }
        } while (frame !== x)
        return args
    }

    private fun processIsAndIsNot(left: Operand, right: Operand, x: SQLBinaryOpExpr): Boolean {
        if ((x.operator == Is || x.operator == IsNot) && !right.isNil()) {
            throw TypeMismatchException("Required NULL, got:${right.expr}")
        }
        return when (x.operator) {
            Is -> {
                stack.push(EvaluatedOperand(x, left.isNil()))
                true
            }
            IsNot -> {
                stack.push(EvaluatedOperand(x, !left.isNil()))
                true
            }
            else -> false
        }
    }

    private fun processEqualityOperation(left: Operand, right: Operand, expr: SQLBinaryOpExpr): Boolean {
        return when (expr.operator) {
            Equality -> {
                evalEqual(expr, left, right)
                true
            }
            NotEqual -> {
                evalNotEqual(expr, left, right)
                true
            }
            LessThanOrGreater -> {
                evalNotEqual(expr, left, right)
                true
            }
            else -> false
        }
    }

    private fun evalEqual(expr: SQLBinaryOpExpr, left: Operand, right: Operand) {
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
            else -> throw TypeMismatchException("Incompatible type to compare, left:${left.expr} right:${right.expr}")
        }
    }

    private fun evalNotEqual(expr: SQLBinaryOpExpr, left: Operand, right: Operand) {
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
            else -> throw TypeMismatchException("Incompatible type to compare, left:${left.expr} right:${right.expr}")
        }
    }

    private fun processBooleanOperation(left: Operand, right: Operand, expr: SQLBinaryOpExpr): Boolean {
        val func: ((SQLExpr, Boolean, Boolean) -> Operand) =
                when (expr.operator) {
                    BooleanAnd -> {
                        this::booleanAnd
                    }
                    BooleanOr -> {
                        this::booleanOr
                    }
                    BooleanXor -> {
                        this::booleanXor
                    }
                    else -> {
                        return false
                    }
                }

        val leftOperand = left.getOperand<Boolean>()
                ?: throw TypeMismatchException("Required Boolean, got ${left.expr}")

        val rightOperand = right.getOperand<Boolean>()
                ?: throw TypeMismatchException("Required Boolean, got ${right.expr}")

        stack.push(func(expr, leftOperand, rightOperand))
        return true
    }

    private fun processNumericOperation(left: Operand, right: Operand, expr: SQLBinaryOpExpr): Boolean {
        val action: ((SQLExpr, BigDecimal, BigDecimal) -> Unit) =
                when (expr.operator) {
                    Add -> this::add
                    Subtract -> this::subtract
                    Multiply -> this::multiply
                    Modulus -> this::mod
                    GreaterThan -> this::greaterThan
                    GreaterThanOrEqual -> this::greaterThanOrEqual
                    LessThan -> this::lessThan
                    LessThanOrEqual -> this::lessThanOrEqual
                    Divide -> this::divide
                    else -> return false
                }
        action(expr, toNumber(left), toNumber(right))
        return true
    }

    private fun nullInBinaryOp(left: Operand, right: Operand, expr: SQLBinaryOpExpr): Boolean {
        if (left.isNil() || right.isNil()) {
            stack.push(EvaluatedOperand(expr, Nil))
            return true
        }
        return false
    }

    private fun asNumber(operand: Operand): Number {
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

    private fun booleanXor(expr: SQLExpr, left: Boolean, right: Boolean): Operand {
        return EvaluatedOperand(expr, left xor right)
    }

    private fun booleanAnd(expr: SQLExpr, left: Boolean, right: Boolean): Operand {
        return EvaluatedOperand(expr, left && right)
    }

    private fun booleanOr(expr: SQLExpr, left: Boolean, right: Boolean): Operand {
        return EvaluatedOperand(expr, left || right)
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
        val mod = left.toBigInteger().mod(right.toBigInteger())
        stack.push(EvaluatedOperand(expr, mod.toBigDecimal()))
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

    private fun popPlaceholderFromStack() {
        stack.pop()
    }

    private inline fun <reified T> popStackOperand(onNull: (Operand) -> Exception): T {
        val operand = stack.pop() as Operand
        return operand.getOperand<T>() ?: throw onNull(operand)
    }
}
