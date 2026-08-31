package dev.ekvedaras.hyperfquery.reference

import com.intellij.database.util.DasUtil
import com.intellij.database.util.DbImplUtil
import com.intellij.database.util.DbUtil
import com.intellij.psi.PsiElement
import com.intellij.testFramework.UsefulTestCase
import dev.ekvedaras.hyperfquery.BaseTestCase
import junit.framework.TestCase

internal class ModelTablePropertyReferenceTest : BaseTestCase() {
    fun testResolvesTableReferenceInTableProperty() {
        myFixture.configureByFile("model/modelTablePropertyReference.php")

        val usages = myFixture.findUsages(dbTable("users"))

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(TableOrViewPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'users'") ?: false)
    }

    fun testDoesNotResolveTableReferenceOutsideModel() {
        myFixture.configureByFile("model/notModelTablePropertyReference.php")

        UsefulTestCase.assertEmpty(myFixture.findUsages(dbTable("users")))
    }

    fun testResolvesTableReferenceOnModelConnection() {
        addDatabasesConfig()
        myFixture.configureByFile("model/modelTablePropertyOnConnectionReference.php")

        val usages = myFixture.findUsages(dbTable("jc_goods"))

        UsefulTestCase.assertSize(1, usages)
        TestCase.assertEquals(TableOrViewPsiReference::class.java, usages.first().referenceClass)
        TestCase.assertTrue(usages.first().element?.textMatches("'jc_goods'") ?: false)
    }

    fun testDoesNotResolveTableFromOtherConnectionSchema() {
        addDatabasesConfig()
        myFixture.configureByFile("model/modelTablePropertyOtherConnectionReference.php")

        UsefulTestCase.assertEmpty(myFixture.findUsages(dbTable("users")))
    }

    private fun dbTable(name: String): PsiElement {
        val table = DasUtil.getTables(dataSource()).first { it.name == name }
        return DbImplUtil.findElement(DbUtil.getDataSources(project).first(), table)
            ?: throw AssertionError("Failed to resolve DB table")
    }
}
