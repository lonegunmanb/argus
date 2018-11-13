package org.apache.uss.argus.operand

class PojoObjectGetPropertiesTest : GetPropertiesTestBase(){
    override fun getArray(array: Array<*>): Array<*> {
        val pojoObject = PojoObject(array, "test", null)
        return pojoObject.getArray()
    }

    override fun getProperties(obj: Any): Properties {
        val pojoObject = PojoObject(obj, "test", null)
        return pojoObject.getProperties()!!
    }
}