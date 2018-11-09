package org.apache.uss.argus;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.util.JdbcConstants;
import org.apache.uss.argus.operand.Address;
import org.apache.uss.argus.operand.Operand;
import org.apache.uss.argus.operand.Person;
import org.apache.uss.argus.operand.PojoObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SQLEvaluatorTest {

    private static Person personWithoutAddress = new Person("tom", 10, null, true);
    private Address address = new Address("address", "city", null);
    private Person personWithAddress = new Person("tom", 10, address, true);

    @Test
    void testCompileSql() {
        var sql = "SELECT * FROM Table as T";
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        assertEquals(sql, evaluator.getSql());
        assertEquals(JdbcConstants.POSTGRESQL, evaluator.getDbType());
        assertEquals("Table", evaluator.getSource());
        assertEquals("T", evaluator.getSourceAlias());
    }

    @ParameterizedTest
    @CsvSource({
            "SELECT * FROM Table WHERE name=1 OR name='a',",
            "SELECT * FROM Table WHERE 1/(name-1)>0 OR name=true,",
            "SELECT * FROM Table as t WHERE name=1 OR t.name='a',t",
            "SELECT * FROM Table WHERE name=1 OR name[1]=1,",
            "SELECT * FROM Table WHERE name=true and name='a',",
            "SELECT * FROM Table WHERE name.valid=true OR name.valid=1,",
            "SELECT * FROM Table WHERE name.valid=true OR name=true,",
            "SELECT * FROM Table WHERE name[1] IS NULL OR name.valid=true,",
            "SELECT * FROM Table as t WHERE t.name.valid IS NULL OR name[1]=true,t"
    })
    void testValidateBadSql(String sql, String alias) {
        var statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        assertThrows(TypeParadoxException.class,
                () -> SQLEvaluator.Companion.validateStatement(statement, "Table", alias));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM Table WHERE name=1 OR name<0.5",
            "SELECT * FROM Table WHERE name IS NULL OR name=TRUE",
            "SELECT * FROM Table WHERE name=true OR concat(name, 1)='1'",
            "SELECT * FROM Table WHERE concat(name, 1)='1' OR name[1]='1'",
            "SELECT * FROM Table WHERE name.valid IS NULL OR name.valid=true",
            "SELECT * FROM Table WHERE 1/(name-1)>0 OR name=1",
    })
    void testValidateGoodSql(String sql) {
        var statement = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        SQLEvaluator.Companion.validateStatement(statement, "Table", null);
    }

    @Test
    void testSelectAll(){
        var sql = "SELECT * FROM Person";
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithoutAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        assertEquals(1, outputList.size());
        assertEquals(personWithoutAddress, outputList.get(0).component2());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM Person Where address.city='shanghai'",
            "SELECT * FROM Person Where isMale=false"
    })
    void testFilteredSelect(String sql){
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithoutAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        assertEquals(0, outputList.size());
    }

    @ParameterizedTest
    @ValueSource(strings={
            "SELECT name as n FROM Person",
            "SELECT address.city as n FROM Person"
    })
    void testAlias(String sql){
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        assertEquals("n", outputList.get(0).getAlias());
    }

    @ParameterizedTest
    @CsvSource({
            "SELECT name FROM Person, name",
            "SELECT address.city FROM Person, city"
    })
    void testColumnName(String sql, String name){
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        assertEquals(name, outputList.get(0).getObjectName());
    }

    @Test
    void testMultipleColumn(){
        var sql = "SELECT name, age, address, isMale FROM Person";
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        assertEquals(4, outputList.size());
        var nameColumn = outputList.get(0);
        assertEquals("name", nameColumn.getObjectName());
        assertEquals(personWithAddress.getName(), nameColumn.component2());
        var ageColumn = outputList.get(1);
        assertEquals("age", ageColumn.getObjectName());
        assertEquals(personWithAddress.getAge(), ageColumn.component2());
        var isMaleColumn = outputList.get(3);
        assertEquals("isMale", isMaleColumn.getObjectName());
        assertEquals(personWithAddress.isMale(), isMaleColumn.component2());
        var addressColumn = outputList.get(2);
        assertEquals("address", addressColumn.getObjectName());
        assertEquals(address, addressColumn.component2());
    }

    @Test
    void testColumnWithoutNameButAlias(){
        var sql = "SELECT 1+1 as r FROM Person";
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        var column = outputList.get(0);
        assertNull(column.getObjectName());
        assertEquals("r", column.getAlias());
    }

    @Test
    void testColumnWithoutNameOrAlias(){
        var sql = "SELECT 1+1 FROM Person";
        var evaluator = SQLEvaluator.Companion.compile(sql, JdbcConstants.POSTGRESQL);
        var outputs = evaluator.evalOutputs(new PojoObject(personWithAddress, "Person", null));
        var outputList = new LinkedList<Operand>();
        outputs.forEach(outputList::add);
        var column = outputList.get(0);
        assertNull(column.getObjectName());
        assertNull(column.getAlias());
    }
}
