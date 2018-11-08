package org.apache.uss.argus

import com.alibaba.druid.sql.ast.SQLExpr
import kotlin.reflect.KClass

@Suppress("unused")
class TypeParadoxException(typeInferenced: KClass<*>, val inferenceSource: SQLExpr, paradoxType: KClass<*>)
    : RuntimeException("Type Paradox: ${typeInferenced.simpleName} is not ${paradoxType.simpleName}")