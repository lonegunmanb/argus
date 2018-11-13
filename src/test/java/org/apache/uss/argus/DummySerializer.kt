package org.apache.uss.argus

import org.apache.uss.argus.operand.EvalObject
import org.apache.uss.argus.serializer.OperandSerializer

class DummySerializer : OperandSerializer {
    override fun writeNumber(number: Number, name: String) {
        throw UnknownError()
    }

    override fun writeBoolean(bool: Boolean, name: String) {
        throw UnknownError()
    }

    override fun writeString(text: String, name: String) {
        throw UnknownError()
    }

    override fun writeObject(`object`: EvalObject, name: String) {
        throw UnknownError()
    }

    override fun writeArray(array: Array<*>, name: String) {
        throw UnknownError()
    }

    override fun writeNull(name: String) {
        throw UnknownError()
    }
}