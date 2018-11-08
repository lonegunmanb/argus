package org.apache.uss.argus.visitor

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.util.JdbcConstants
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class SourceVisitorTest {
    @ParameterizedTest
    @CsvSource("SELECT * FROM TABLE, TABLE, ",
            "SELECT * FROM TABLE AS T, TABLE, T")
    fun testGetSourceNameAndAlias(sql:String, source:String, alias:String?) {
        val selectStatement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL)[0]
        val sourceVisitor = SourceVisitor()
        selectStatement.accept(sourceVisitor)
        assertEquals(source, sourceVisitor.source)
        assertEquals(alias, sourceVisitor.alias)
    }
}