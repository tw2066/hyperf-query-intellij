package dev.ekvedaras.hyperfquery.schema

import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbImplUtil
import com.intellij.database.util.DbUtil
import com.intellij.testFramework.UsefulTestCase
import dev.ekvedaras.hyperfquery.BaseTestCase
import dev.ekvedaras.hyperfquery.inspection.UnknownTableOrViewInspection
import dev.ekvedaras.hyperfquery.reference.SchemaPsiReference
import dev.ekvedaras.hyperfquery.reference.TableOrViewPsiReference
import junit.framework.TestCase

internal class SchemaBuilderTest : BaseTestCase() {
    fun testCompletesTables() {
        myFixture.configureByFile("schema/table.php")
        myFixture.completeBasic()

        assertCompletion(*schemaTables.values.flatten().toTypedArray())
        assertNoCompletion(*schemas.toTypedArray())
    }

    fun testCompletesSchemas() {
        myFixture.configureByFile("schema/createDatabase.php")
        myFixture.completeBasic()

        assertCompletion(*schemas.toTypedArray())
        assertNoCompletion(*schemaTables.values.flatten().toTypedArray())
    }

    fun testDoesNotWarnAboutUnknownSchema() {
        assertInspection("schema/unknownSchema.php", UnknownTableOrViewInspection())
    }

    fun testDoesNotWarnAboutUnknownTable() {
        assertInspection("schema/unknownTable.php", UnknownTableOrViewInspection())
    }

    fun testResolvesSchemaReference() {
        myFixture.configureByFile("schema/knownSchema.php")

        val schema = DasUtil.getSchemas(db).first { it.name == "testProject1" }
        val dbSchema = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), schema)
            ?: return fail("Failed to resolve DB schema")

        val usages = myFixture.findUsages(dbSchema)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(SchemaPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'testProject1'") ?: false)
        TestCase.assertEquals(30, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(30 + schema.name.length, usages.first().navigationRange.endOffset)
    }

    fun testResolvesTableReference() {
        myFixture.configureByFile("schema/knownTable.php")

        val table = DasUtil.getTables(dataSource()).first { it.name == "users" }
        val dbTable = DbImplUtil.findElement(DbUtil.getDataSources(project).first(), table)
            ?: return fail("Failed to resolve DB table")

        val usages = myFixture.findUsages(dbTable)

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(TableOrViewPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'users'") ?: false)
        TestCase.assertEquals(21, usages.first().navigationRange.startOffset)
        TestCase.assertEquals(21 + table.name.length, usages.first().navigationRange.endOffset)
    }
}
