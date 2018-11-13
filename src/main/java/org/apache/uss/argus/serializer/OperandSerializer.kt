package org.apache.uss.argus.serializer

import org.apache.uss.argus.operand.EvalObject

interface OperandSerializer {
    fun writeNumber(number: Number, name: String)
    fun writeBoolean(bool: Boolean, name: String)
    fun writeString(text: String, name: String)
    fun writeObject(`object`: EvalObject, name: String)
    fun writeArray(array: Array<*>, name: String)
}