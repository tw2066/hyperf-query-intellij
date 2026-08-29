package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.model.ObjectKind
import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbImplUtil
import com.intellij.database.util.DbUtil
import com.intellij.testFramework.UsefulTestCase
import dev.ekvedaras.hyperfquery.BaseTestCase
import junit.framework.TestCase

internal class SchemaTableColumnReferenceTest : BaseTestCase() {
    fun testResolvesColumnReference() {
        myFixture.configureByFile("inspection/knownColumn.php")

        val column = DasUtil.getTables(dataSource())
            .first { it.name == "users" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "id" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'id'") ?: false)
        TestCase.assertEquals(78, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(78 + column.name.length, usages.first().navigationRange.endOffset)
    }

    fun testResolvesSelectRawColumnReference() {
        myFixture.configureByFile("inspection/knownSelectRawColumn.php")

        val column = DasUtil.getTables(dataSource())
            .first { it.name == "users" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "id" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        val expectedStart = myFixture.file.text.indexOf("'id'") + 1
        TestCase.assertEquals(expectedStart, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(expectedStart + column.name.length, usages.first().navigationRange.endOffset)
    }

    fun testResolvesDbRawColumnReference() {
        myFixture.configureByFile("reference/dbRawColumn.php")

        val column = DasUtil.getTables(dataSource())
            .first { it.name == "users" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "id" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        val expectedStart = myFixture.file.text.indexOf("'id'") + 1
        TestCase.assertEquals(expectedStart, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(expectedStart + column.name.length, usages.first().navigationRange.endOffset)
    }

    fun testResolvesSelectRawCommaSegmentColumnReference() {
        myFixture.configureByFile("reference/selectRawColumns.php")

        val column = DasUtil.getTables(dataSource())
            .first { it.name == "users" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "email" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        val expectedStart = myFixture.file.text.indexOf("email")
        TestCase.assertEquals(expectedStart, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(expectedStart + column.name.length, usages.first().navigationRange.endOffset)
    }

    fun testResolvesPrefixedRawAliasAndTableReference() {
        addPrefixedGoodsConfig()
        myFixture.configureByFile("reference/prefixedRawColumn.php")

        val table = DasUtil.getTables(dataSource()).first { it.name == "jc_goods" }
        val column = table.getDasChildren(ObjectKind.COLUMN).first { it.name == "number" }

        val dbTable = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), table)
            ?: return fail("Failed to resolve DB table")
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val columnUsages = myFixture.findUsages(dbColumn)
        UsefulTestCase.assertSize(1, columnUsages)
        TestCase.assertEquals(ColumnPsiReference::class.java, columnUsages.first().referenceClass)
        val expectedColumnStart = myFixture.file.text.indexOf("number")
        TestCase.assertEquals(expectedColumnStart, columnUsages.first().navigationRange.startOffset)

        // jc_a 段同时通过 TableOrViewPsiReference 解析到 jc_goods 表。
        // 注意: 不能用 findUsages(dbTable) —— 它按元素名 jc_goods 做文本预过滤,
        // 而带前缀的写法文件里只有 jc_a/goods 字样(与 SchemaTableReferenceTest 中注释掉的用例同因)。
        val aliasReferences = myFixture.file.findElementAt(myFixture.file.text.indexOf("jc_a") + 1)
            ?.parent?.references ?: emptyArray()
        TestCase.assertTrue(
            aliasReferences.filterIsInstance<TableOrViewPsiReference>().any { it.resolve() == dbTable }
        )
    }

    fun testItDoesNotResolveColumnsFromOtherTablesBecauseOfTheContext() {
        myFixture.configureByFile("inspection/knownColumn.php")

        val columnFromOtherTable = DasUtil.getTables(dataSource())
            .first { it.name == "customers" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "id" }
        val dbColumnFromOtherTable = DbImplUtil.findElement(
            DbUtil.getDataSources(project).first(),
            columnFromOtherTable
        ) ?: return fail("Failed to resolve DB column from other table")

        val otherUsages = myFixture.findUsages(dbColumnFromOtherTable)

        UsefulTestCase.assertSize(0, otherUsages)
    }

    fun testResolvesTableAndColumnReference() {
        myFixture.configureByFile("inspection/knownTableColumn.php")

        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }
        val column = table.getDasChildren(ObjectKind.COLUMN).first { it.name == "id" }

        val dbTable = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), table)
            ?: return fail("Failed to resolve DB table")
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val tableUsages = myFixture.findUsages(dbTable)
        val columnUsages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(2, tableUsages) // from() + get()
        UsefulTestCase.assertSize(1, columnUsages)

        TestCase.assertEquals(TableOrViewPsiReference::class.java, tableUsages.last().referenceClass)
        TestCase.assertEquals(ColumnPsiReference::class.java, columnUsages.first().referenceClass)

        TestCase.assertTrue(tableUsages.last().element?.textMatches("'users.id'") ?: false)
        TestCase.assertTrue(columnUsages.first().element?.textMatches("'users.id'") ?: false)

        TestCase.assertEquals(78, tableUsages.last().navigationRange.startOffset)
        TestCase.assertEquals(78 + table.name.length + 1, columnUsages.first().navigationRange.startOffset)

        TestCase.assertEquals(78 + table.name.length, tableUsages.last().navigationRange.endOffset)
        TestCase.assertEquals(
            78 + table.name.length + 1 + column.name.length,
            columnUsages.first().navigationRange.endOffset
        )
    }

    @Suppress("ReturnCount")
    fun testResolvesSchemaAndTableAndColumnReferences() {
        myFixture.configureByFile("inspection/knownSchemaTableColumn.php")

        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }
        val schema = table.dasParent ?: return fail("Failed to load table schema")
        val column = table.getDasChildren(ObjectKind.COLUMN).first { it.name == "id" }

        val dbSchema = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), schema)
            ?: return fail("Failed to resolve DB schema")
        val dbTable = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), table)
            ?: return fail("Failed to resolve DB table")
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val schemaUsages = myFixture.findUsages(dbSchema)
        val tableUsages = myFixture.findUsages(dbTable)
        val columnUsages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(2, schemaUsages) // from() + get()
        UsefulTestCase.assertSize(2, tableUsages) // from() + get()
        UsefulTestCase.assertSize(1, columnUsages)

        TestCase.assertEquals(SchemaPsiReference::class.java, schemaUsages.first().referenceClass)
        TestCase.assertEquals(TableOrViewPsiReference::class.java, tableUsages.first().referenceClass)
        TestCase.assertEquals(ColumnPsiReference::class.java, columnUsages.first().referenceClass)

        TestCase.assertTrue(schemaUsages.last().element?.textMatches("'testProject1.users.id'") ?: false)
        TestCase.assertTrue(tableUsages.last().element?.textMatches("'testProject1.users.id'") ?: false)
        TestCase.assertTrue(columnUsages.first().element?.textMatches("'testProject1.users.id'") ?: false)

        TestCase.assertEquals(78, schemaUsages.last().navigationRange.startOffset)
        TestCase.assertEquals(78 + schema.name.length + 1, tableUsages.last().navigationRange.startOffset)
        TestCase.assertEquals(
            78 + schema.name.length + 1 + table.name.length + 1,
            columnUsages.first().navigationRange.startOffset
        )

        TestCase.assertEquals(78 + schema.name.length, schemaUsages.last().navigationRange.endOffset)
        TestCase.assertEquals(
            78 + schema.name.length + 1 + table.name.length,
            tableUsages.last().navigationRange.endOffset
        )
        TestCase.assertEquals(
            78 + schema.name.length + 1 + table.name.length + 1 + column.name.length,
            columnUsages.first().navigationRange.endOffset
        )
    }

    fun testResolvesJsonColumnReference() {
        myFixture.configureByFile("inspection/knownJsonColumn.php")

        val column = DasUtil.getTables(dataSource())
            .first { it.name == "users" }
            .getDasChildren(ObjectKind.COLUMN)
            .first { it.name == "id" }
        val dbColumn = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), column)
            ?: return fail("Failed to resolve DB column")

        val usages = myFixture.findUsages(dbColumn)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(ColumnPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'id->prop'") ?: false)
        TestCase.assertEquals(78, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(78 + column.name.length, usages.first().navigationRange.endOffset)
    }
}
