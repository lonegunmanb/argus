package org.apache.uss.argus.function;

import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import org.apache.uss.argus.OperandExpr;

public interface Function {
    Object eval(SQLMethodInvokeExpr expr, OperandExpr... args);
}
