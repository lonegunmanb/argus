package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import kotlin.reflect.KClass

@Suppress("unused")
class TypeParadoxException(typeInferenced: KClass<*>, inferenceSource: SQLExpr?, paradoxType: KClass<*>)
    : RuntimeException("Type Paradox: ${typeInferenced.simpleName} base on ${inferenceSource.toString()} is not ${paradoxType.simpleName}")