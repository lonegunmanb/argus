package org.apache.uss.argus.operand

data class Person(val name: String, val age: Int, val address: Address?, val isMale: Boolean?)
data class Address(val address: String, val city: String)