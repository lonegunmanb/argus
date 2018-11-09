package org.apache.uss.argus

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.util.JdbcConstants
import org.apache.uss.argus.operand.Address
import org.apache.uss.argus.operand.Operand
import org.apache.uss.argus.operand.Person
import org.apache.uss.argus.operand.PojoObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

import java.util.LinkedList

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows

internal class SQLEvaluatorTest {
    private val address = Address("address", "city", null)
    private val personWithAddress = Person("tom", 10, address, true)

    @Test
    fun testCompileSql() {
        val sql = "SELECT * FROM Table as T"
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        assertEquals(sql, evaluator.sql)
        assertEquals(JdbcConstants.POSTGRESQL, evaluator.dbType)
        assertEquals("Table", evaluator.source)
        assertEquals("T", evaluator.sourceAlias)
    }

    @ParameterizedTest
    @CsvSource("SELECT * FROM Table WHERE name=1 OR name='a',",
            "SELECT * FROM Table WHERE 1/(name-1)>0 OR name=true,",
            "SELECT * FROM Table as t WHERE name=1 OR t.name='a',t",
            "SELECT * FROM Table WHERE name=1 OR name[1]=1,",
            "SELECT * FROM Table WHERE name=true and name='a',",
            "SELECT * FROM Table WHERE name.valid=true OR name.valid=1,",
            "SELECT * FROM Table WHERE name.valid=true OR name=true,",
            "SELECT * FROM Table WHERE name[1] IS NULL OR name.valid=true,",
            "SELECT * FROM Table as t WHERE t.name.valid IS NULL OR name[1]=true,t")
    fun testValidateBadSql(sql: String, alias: String?) {
        val statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL)[0]
        assertThrows(TypeParadoxException::class.java
        ) { SQLEvaluator.validateStatement(statement, "Table", alias) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["SELECT * FROM Table WHERE name=1 OR name<0.5",
        "SELECT * FROM Table WHERE name IS NULL OR name=TRUE",
        "SELECT * FROM Table WHERE name=true OR concat(name, 1)='1'",
        "SELECT * FROM Table WHERE concat(name, 1)='1' OR name[1]='1'",
        "SELECT * FROM Table WHERE name.valid IS NULL OR name.valid=true",
        "SELECT * FROM Table WHERE 1/(name-1)>0 OR name=1"])
    fun testValidateGoodSql(sql: String) {
        val statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL)[0]
        SQLEvaluator.validateStatement(statement, "Table", null)
    }

    @Test
    fun testSelectAll() {
        val sql = "SELECT * FROM Person"
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithoutAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach { outputList.add(it) }
        assertEquals(1, outputList.size)
        assertEquals(personWithoutAddress, outputList[0].component2())
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("SELECT * FROM Person Where address.city='shanghai'",
            "SELECT * FROM Person Where isMale=false"))
    fun testFilteredSelect(sql: String) {
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithoutAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach{ outputList.add(it) }
        assertEquals(0, outputList.size)
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("SELECT name as n FROM Person", "SELECT address.city as n FROM Person"))
    fun testAlias(sql: String) {
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach{ outputList.add(it) }
        assertEquals("n", outputList[0].alias)
    }

    @ParameterizedTest
    @CsvSource("SELECT name FROM Person, name",
            "SELECT address.city FROM Person, city")
    fun testColumnName(sql: String, name: String) {
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach{ outputList.add(it) }
        assertEquals(name, outputList[0].objectName)
    }

    @Test
    fun testMultipleColumn() {
        val sql = "SELECT name, age, address, isMale FROM Person"
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach{ outputList.add(it) }
        assertEquals(4, outputList.size)
        val nameColumn = outputList[0]
        assertEquals("name", nameColumn.objectName)
        assertEquals(personWithAddress.name, nameColumn.component2())
        val ageColumn = outputList[1]
        assertEquals("age", ageColumn.objectName)
        assertEquals(personWithAddress.age, ageColumn.component2())
        val isMaleColumn = outputList[3]
        assertEquals("isMale", isMaleColumn.objectName)
        assertEquals(personWithAddress.isMale, isMaleColumn.component2())
        val addressColumn = outputList[2]
        assertEquals("address", addressColumn.objectName)
        assertEquals(address, addressColumn.component2())
    }

    @Test
    fun testColumnWithoutNameButAlias() {
        val sql = "SELECT 1+1 as r FROM Person"
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach{ outputList.add(it) }
        val column = outputList[0]
        assertNull(column.objectName)
        assertEquals("r", column.alias)
    }

    @Test
    fun testColumnWithoutNameOrAlias() {
        val sql = "SELECT 1+1 FROM Person"
        val evaluator = SQLEvaluator.compile(sql, JdbcConstants.POSTGRESQL)
        val outputs = evaluator.evalOutputs(PojoObject(personWithAddress, "Person", null))
        val outputList = LinkedList<Operand>()
        outputs.forEach{ outputList.add(it) }
        val column = outputList[0]
        assertNull(column.objectName)
        assertNull(column.alias)
    }

    companion object {

        private val personWithoutAddress = Person("tom", 10, null, true)
    }
}